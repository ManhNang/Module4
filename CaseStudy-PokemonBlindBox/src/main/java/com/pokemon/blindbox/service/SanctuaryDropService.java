package com.pokemon.blindbox.service;

import com.pokemon.blindbox.entity.*;
import com.pokemon.blindbox.entity.enums.SanctuaryDropType;
import com.pokemon.blindbox.entity.enums.SealedItemType;
import com.pokemon.blindbox.entity.enums.TransactionType;
import com.pokemon.blindbox.exception.BusinessValidationException;
import com.pokemon.blindbox.exception.ResourceNotFoundException;
import com.pokemon.blindbox.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service quản lý Sự kiện ngẫu nhiên trên Đảo Pokémon (Random Drops) & Mảnh Pack:
 * - Chống gian lận 100% phía Server (PESSIMISTIC_WRITE lock, kiểm tra TTL 5 phút, giới hạn 8 lượt/ngày, cooldown).
 * - Xu thưởng từ Sự kiện cộng thẳng vào Ví (KHÔNG tính vào hạn mức 1.000 Xu/ngày của Đảo).
 * - Tích lũy 5 Mảnh Pack -> Đổi ngay 1 Pack ngẫu nhiên từ các Bộ thẻ đang mở vào Kho Pack & Box.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SanctuaryDropService {

    public static final int DAILY_DROP_LIMIT = 8;
    public static final int SHARDS_PER_PACK = 5;
    public static final long DROP_TTL_SECONDS = 300L; // 5 phút

    // Tọa độ trung tâm hồ nước bên phải đảo (khớp 100% với isInsidePond trong sanctuary.js)
    private static final double POND_CENTER_U = 0.46;
    private static final double POND_CENTER_V = -0.36;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${app.sanctuary.drop-cooldown-seconds:600}")
    private long dropCooldownSeconds = 600L;

    private final SanctuaryUserProfileRepository profileRepository;
    private final SanctuaryRandomDropRepository dropRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final CardSetRepository cardSetRepository;
    private final UserSealedItemRepository userSealedItemRepository;

    /**
     * Kiểm tra vật phẩm đang xuất hiện trên đảo; nếu đã qua cooldown & chưa đạt 8/8 lượt ngày thì spawn mới.
     */
    @Transactional
    public Map<String, Object> checkOrSpawnDrop(Long userId) {
        Instant now = Instant.now();
        LocalDate today = LocalDate.now();
        SanctuaryUserProfile profile = getOrCreateProfileForUpdate(userId, today);

        Optional<SanctuaryRandomDrop> existingOpt = dropRepository
                .findFirstByUserIdAndClaimedFalseAndExpiresAtAfterOrderBySpawnedAtDesc(userId, now);

        SanctuaryRandomDrop activeDrop = existingOpt.orElse(null);

        if (activeDrop == null && profile.getDailyDropsClaimed() < DAILY_DROP_LIMIT) {
            boolean cooldownReady = profile.getLastDropAt() == null
                    || Duration.between(profile.getLastDropAt(), now).getSeconds() >= dropCooldownSeconds;
            if (cooldownReady) {
                activeDrop = spawnNewDrop(userId, now);
                log.info("Spawned SanctuaryRandomDrop id={} type={} for userId={}",
                        activeDrop.getId(), activeDrop.getDropType(), userId);
            }
        }

        long nextSpawnInSeconds = computeNextSpawnSeconds(profile, activeDrop, now);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("activeDrop", activeDrop != null ? toDropDto(activeDrop, now) : null);
        resp.put("dailyClaimed", profile.getDailyDropsClaimed());
        resp.put("dailyLimit", DAILY_DROP_LIMIT);
        resp.put("packShards", profile.getPackShards());
        resp.put("shardsPerPack", SHARDS_PER_PACK);
        resp.put("nextSpawnInSeconds", nextSpawnInSeconds);
        return resp;
    }

    /**
     * Nhặt vật phẩm sự kiện (xác thực chặt id, quyền sở hữu, chưa hết hạn, chưa nhặt).
     */
    @Transactional
    public Map<String, Object> claimDrop(Long userId, Long dropId) {
        Instant now = Instant.now();
        LocalDate today = LocalDate.now();

        SanctuaryUserProfile profile = getOrCreateProfileForUpdate(userId, today);
        SanctuaryRandomDrop drop = dropRepository.findByIdForUpdate(dropId)
                .orElseThrow(() -> new ResourceNotFoundException("Vật phẩm sự kiện không tồn tại hoặc đã biến mất!"));

        if (!drop.getUserId().equals(userId)) {
            throw new BusinessValidationException("Bạn không có quyền nhặt vật phẩm này!");
        }
        if (Boolean.TRUE.equals(drop.getClaimed())) {
            throw new BusinessValidationException("Vật phẩm này đã được nhặt trước đó!");
        }
        if (now.isAfter(drop.getExpiresAt())) {
            throw new BusinessValidationException("Vật phẩm đã hết hạn và biến mất khỏi đảo!");
        }
        if (profile.getDailyDropsClaimed() >= DAILY_DROP_LIMIT) {
            throw new BusinessValidationException("Bạn đã đạt giới hạn " + DAILY_DROP_LIMIT + "/" + DAILY_DROP_LIMIT + " lượt nhặt sự kiện hôm nay!");
        }

        // Quay phần thưởng hoàn toàn phía Server theo từng loại sự kiện
        String rewardType;
        int rewardAmount;
        String rewardLabel;
        String txDesc;

        switch (drop.getDropType()) {
            case POKEBALL -> {
                rewardType = "COINS";
                rewardAmount = 40 + SECURE_RANDOM.nextInt(41); // 40..80 Xu
                rewardLabel = "+" + rewardAmount + " Xu";
                txDesc = "Sự kiện Đảo Pokémon: Nhặt Pokéball rơi";
            }
            case POND_SPLASH -> {
                if (SECURE_RANDOM.nextBoolean()) {
                    rewardType = "SHARD";
                    rewardAmount = 1;
                    rewardLabel = "+1 Mảnh Pack";
                    txDesc = "Sự kiện Đảo Pokémon: Cá nhảy hồ nước";
                } else {
                    rewardType = "COINS";
                    rewardAmount = 100 + SECURE_RANDOM.nextInt(51); // 100..150 Xu
                    rewardLabel = "+" + rewardAmount + " Xu";
                    txDesc = "Sự kiện Đảo Pokémon: Cá nhảy hồ nước";
                }
            }
            case TREASURE_CHEST -> {
                // Ưu tiên Mảnh Pack (70% +1 Mảnh Pack, 30% +200 Xu)
                if (SECURE_RANDOM.nextInt(100) < 70) {
                    rewardType = "SHARD";
                    rewardAmount = 1;
                    rewardLabel = "+1 Mảnh Pack";
                    txDesc = "Sự kiện Đảo Pokémon: Rương kho báu";
                } else {
                    rewardType = "COINS";
                    rewardAmount = 200;
                    rewardLabel = "+200 Xu";
                    txDesc = "Sự kiện Đảo Pokémon: Rương kho báu";
                }
            }
            default -> {
                rewardType = "COINS";
                rewardAmount = 50;
                rewardLabel = "+50 Xu";
                txDesc = "Sự kiện Đảo Pokémon";
            }
        }

        BigDecimal newWalletBalance = null;
        if ("COINS".equals(rewardType)) {
            // Cộng thẳng vào Ví, KHÔNG tính vào hạn mức 1.000 Xu/ngày của Đảo
            Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví của người dùng."));
            BigDecimal credit = BigDecimal.valueOf(rewardAmount).setScale(2, RoundingMode.HALF_UP);
            wallet.setBalance(wallet.getBalance().add(credit));
            Wallet savedWallet = walletRepository.save(wallet);
            walletService.recordTransaction(savedWallet, TransactionType.SANCTUARY_RANDOM_DROP, credit, txDesc);
            newWalletBalance = savedWallet.getBalance();
        } else {
            profile.setPackShards(profile.getPackShards() + rewardAmount);
            newWalletBalance = walletRepository.findByUserId(userId).map(Wallet::getBalance).orElse(null);
        }

        drop.setClaimed(true);
        drop.setClaimedAt(now);
        drop.setRewardType(rewardType);
        drop.setRewardAmount(rewardAmount);
        dropRepository.save(drop);

        profile.setDailyDropsClaimed(profile.getDailyDropsClaimed() + 1);
        profile.setLastDropAt(now);
        profileRepository.save(profile);

        log.info("User {} claimed dropId={} type={} -> {} ({})",
                userId, dropId, drop.getDropType(), rewardType, rewardAmount);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("dropId", dropId);
        resp.put("dropType", drop.getDropType().name());
        resp.put("rewardType", rewardType);
        resp.put("rewardAmount", rewardAmount);
        resp.put("rewardLabel", rewardLabel);
        resp.put("packShards", profile.getPackShards());
        resp.put("shardsPerPack", SHARDS_PER_PACK);
        resp.put("dailyClaimed", profile.getDailyDropsClaimed());
        resp.put("dailyLimit", DAILY_DROP_LIMIT);
        resp.put("nextSpawnInSeconds", computeNextSpawnSeconds(profile, null, now));
        resp.put("newWalletBalance", newWalletBalance);
        resp.put("message", "Nhặt thành công: " + rewardLabel + "!");
        return resp;
    }

    /**
     * Đổi 5 Mảnh Pack lấy 1 Pack ngẫu nhiên từ các Bộ thẻ đang mở vào Kho Pack & Box.
     */
    @Transactional
    public Map<String, Object> exchangeShardsForPack(Long userId) {
        LocalDate today = LocalDate.now();
        SanctuaryUserProfile profile = getOrCreateProfileForUpdate(userId, today);

        if (profile.getPackShards() < SHARDS_PER_PACK) {
            throw new BusinessValidationException(
                    "Cần đủ " + SHARDS_PER_PACK + " Mảnh Pack để đổi (hiện có " + profile.getPackShards() + "/" + SHARDS_PER_PACK + ")."
            );
        }

        List<CardSet> activeSets = cardSetRepository.findAll().stream()
                .filter(cs -> cs.getIsActive() == null || cs.getIsActive())
                .sorted(Comparator.comparing(CardSet::getId))
                .toList();
        if (activeSets.isEmpty()) {
            throw new BusinessValidationException("Hiện chưa có Bộ thẻ nào đang mở để quy đổi Pack!");
        }

        CardSet chosenSet = activeSets.get(SECURE_RANDOM.nextInt(activeSets.size()));
        int cardsPerPack = chosenSet.getCardsPerPack() != null && chosenSet.getCardsPerPack() > 0 ? chosenSet.getCardsPerPack() : 6;
        int packsPerBox = chosenSet.getPacksPerBox() != null && chosenSet.getPacksPerBox() > 0 ? chosenSet.getPacksPerBox() : 20;

        profile.setPackShards(profile.getPackShards() - SHARDS_PER_PACK);
        profileRepository.save(profile);

        UserSealedItem pack = userSealedItemRepository.save(UserSealedItem.builder()
                .userId(userId)
                .cardSet(chosenSet)
                .itemType(SealedItemType.PACK)
                .cardsPerPack(cardsPerPack)
                .packsPerBox(packsPerBox)
                .isLocked(false)
                .isConsumed(false)
                .acquiredAt(LocalDateTime.now())
                .build());

        log.info("User {} exchanged {} pack shards for 1 sealed PACK id={} (set={})",
                userId, SHARDS_PER_PACK, pack.getId(), chosenSet.getCode());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("sealedItemId", pack.getId());
        resp.put("setId", chosenSet.getId());
        resp.put("setName", chosenSet.getName());
        resp.put("setCode", chosenSet.getCode());
        resp.put("packShards", profile.getPackShards());
        resp.put("shardsPerPack", SHARDS_PER_PACK);
        resp.put("message", "Đổi thành công 1 Pack \"" + chosenSet.getName() + "\" vào Kho Pack & Box!");
        return resp;
    }

    private SanctuaryUserProfile getOrCreateProfileForUpdate(Long userId, LocalDate today) {
        SanctuaryUserProfile profile = profileRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> profileRepository.saveAndFlush(SanctuaryUserProfile.builder()
                        .userId(userId)
                        .packShards(0)
                        .dailyDropDate(today)
                        .dailyDropsClaimed(0)
                        .build()));

        if (profile.getDailyDropDate() == null || !profile.getDailyDropDate().equals(today)) {
            profile.setDailyDropDate(today);
            profile.setDailyDropsClaimed(0);
            profileRepository.save(profile);
        }
        return profile;
    }

    private SanctuaryRandomDrop spawnNewDrop(Long userId, Instant now) {
        int roll = SECURE_RANDOM.nextInt(100);
        SanctuaryDropType type;
        if (roll < 60) {
            type = SanctuaryDropType.POKEBALL;
        } else if (roll < 85) {
            type = SanctuaryDropType.POND_SPLASH;
        } else {
            type = SanctuaryDropType.TREASURE_CHEST;
        }

        double posX;
        double posY;
        if (type == SanctuaryDropType.POND_SPLASH) {
            posX = POND_CENTER_U;
            posY = POND_CENTER_V;
        } else {
            double[] uv = pickRandomMeadowUv();
            posX = uv[0];
            posY = uv[1];
        }

        return dropRepository.save(SanctuaryRandomDrop.builder()
                .userId(userId)
                .dropType(type)
                .posX(posX)
                .posY(posY)
                .spawnedAt(now)
                .expiresAt(now.plusSeconds(DROP_TTL_SECONDS))
                .claimed(false)
                .build());
    }

    private double[] pickRandomMeadowUv() {
        for (int tries = 0; tries < 20; tries++) {
            double u = -0.60 + SECURE_RANDOM.nextDouble() * 1.20;
            double v = -0.50 + SECURE_RANDOM.nextDouble() * 1.06;
            double pondDu = (u - POND_CENTER_U) / 0.40;
            double pondDv = (v - POND_CENTER_V) / 0.34;
            boolean inPond = (pondDu * pondDu + pondDv * pondDv) < 1.0;
            boolean nearPortal = Math.hypot(u + 0.70, v + 0.70) < 0.36;
            if (!inPond && !nearPortal) {
                return new double[]{Math.round(u * 1000.0) / 1000.0, Math.round(v * 1000.0) / 1000.0};
            }
        }
        return new double[]{-0.15, 0.22};
    }

    private long computeNextSpawnSeconds(SanctuaryUserProfile profile, SanctuaryRandomDrop activeDrop, Instant now) {
        if (activeDrop != null) return 0L;
        if (profile.getDailyDropsClaimed() >= DAILY_DROP_LIMIT) return -1L;
        if (profile.getLastDropAt() == null) return 0L;
        long elapsed = Duration.between(profile.getLastDropAt(), now).getSeconds();
        return Math.max(0L, dropCooldownSeconds - elapsed);
    }

    private Map<String, Object> toDropDto(SanctuaryRandomDrop d, Instant now) {
        long remainingSec = Math.max(0L, Duration.between(now, d.getExpiresAt()).getSeconds());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("type", d.getDropType().name());
        m.put("posX", d.getPosX());
        m.put("posY", d.getPosY());
        m.put("expiresAt", d.getExpiresAt().toString());
        m.put("remainingSeconds", remainingSec);
        return m;
    }
}
