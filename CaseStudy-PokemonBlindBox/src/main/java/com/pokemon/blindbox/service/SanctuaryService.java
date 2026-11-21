package com.pokemon.blindbox.service;

import com.pokemon.blindbox.dto.request.StartExpeditionRequest;
import com.pokemon.blindbox.dto.response.CardTemplateResponseDTO;
import com.pokemon.blindbox.dto.response.UserCardResponseDTO;
import com.pokemon.blindbox.entity.*;
import com.pokemon.blindbox.entity.enums.BoxStatus;
import com.pokemon.blindbox.entity.enums.ExpeditionStatus;
import com.pokemon.blindbox.entity.enums.PackStatus;
import com.pokemon.blindbox.entity.enums.TransactionType;
import com.pokemon.blindbox.exception.BusinessValidationException;
import com.pokemon.blindbox.exception.ResourceNotFoundException;
import com.pokemon.blindbox.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Poke-Sanctuary Idle 2.5D Service:
 * - Quản lý tối đa 5 thẻ trên Đảo (SanctuarySlot) & thu hoạch Xu theo thời gian Server (Instant.now()).
 * - Quản lý 3 vùng đất Thám Hiểm (Expedition) với thời gian & phần thưởng hoàn toàn tính phía Server.
 * - Quản lý Nhiệm Vụ Ngày (DailyQuest) & Daily Cap (150.000 ₫/ngày) chống lạm phát.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SanctuaryService {

    public static final int MAX_ISLAND_SLOTS = 5;
    public static final long HARVEST_TICK_SECONDS = 60L;
    public static final long MAX_ACCUMULATED_TICKS = 12L;
    public static final BigDecimal DAILY_GAME_COIN_CAP = new BigDecimal("250.00");

    public static final String QUEST_HARVEST = "HARVEST_ONCE";
    public static final String QUEST_EXPEDITION = "EXPEDITION_ONCE";
    public static final String QUEST_OPEN_PACK = "OPEN_PACK_ONCE";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final Map<String, ExpeditionZoneConfig> ZONES = Map.of(
            "GREEN_FOREST", new ExpeditionZoneConfig("GREEN_FOREST", "Rừng Xanh", 15, new BigDecimal("20.00"), 5),
            "CRYSTAL_CAVE", new ExpeditionZoneConfig("CRYSTAL_CAVE", "Hang Tinh Thể", 60, new BigDecimal("65.00"), 12),
            "LEGEND_PEAK", new ExpeditionZoneConfig("LEGEND_PEAK", "Đỉnh Huyền Thoại", 180, new BigDecimal("160.00"), 25)
    );

    private final SanctuarySlotRepository sanctuarySlotRepository;
    private final ExpeditionRepository expeditionRepository;
    private final ExpeditionCardRepository expeditionCardRepository;
    private final DailyQuestRepository dailyQuestRepository;
    private final SanctuaryDailyEarningRepository dailyEarningRepository;
    private final UserCardRepository userCardRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final BoosterBoxRepository boosterBoxRepository;
    private final BoosterPackRepository boosterPackRepository;

    public record ExpeditionZoneConfig(
            String code,
            String name,
            int durationMinutes,
            BigDecimal baseCoins,
            int bonusPackChance
    ) {}

    /**
     * Trả về tập userCardId đang bận đi thám hiểm (IN_PROGRESS) để Kho đồ hiển thị đúng lý do khóa.
     */
    @Transactional(readOnly = true)
    public Set<Long> getActiveExpeditionCardIds(Long userId) {
        List<Expedition> active = expeditionRepository
                .findByUserIdAndStatusOrderByStartedAtDesc(userId, ExpeditionStatus.IN_PROGRESS);
        Set<Long> ids = new HashSet<>();
        for (Expedition exp : active) {
            if (exp.getCards() != null) {
                for (ExpeditionCard ec : exp.getCards()) {
                    if (ec.getUserCard() != null) {
                        ids.add(ec.getUserCard().getId());
                    }
                }
            }
        }
        return ids;
    }

    /**
     * Trả về toàn bộ trạng thái Đảo Pokémon cho người dùng hiện tại (Server-authoritative Instant.now()).
     */
    @Transactional
    public Map<String, Object> getSanctuaryState(Long userId) {
        Instant now = Instant.now();
        LocalDate today = LocalDate.now();
        List<DailyQuest> quests = ensureDailyQuests(userId, today);

        List<SanctuarySlot> slots = sanctuarySlotRepository.findByUserIdOrderByCreatedAtAsc(userId);
        Set<Long> islandCardIds = new HashSet<>();
        List<Map<String, Object>> slotDtos = new ArrayList<>();
        for (SanctuarySlot slot : slots) {
            islandCardIds.add(slot.getUserCard().getId());
            slotDtos.add(toSlotMap(slot, now));
        }

        List<Expedition> activeExpeditions = expeditionRepository
                .findByUserIdAndStatusOrderByStartedAtDesc(userId, ExpeditionStatus.IN_PROGRESS);
        List<Map<String, Object>> expeditionDtos = activeExpeditions.stream()
                .map(exp -> toExpeditionMap(exp, now))
                .toList();

        List<Map<String, Object>> questDtos = quests.stream()
                .map(this::toQuestMap)
                .toList();

        BigDecimal earnedToday = dailyEarningRepository.findByUserIdAndEarningDate(userId, today)
                .map(SanctuaryDailyEarning::getCoinsEarned)
                .orElse(BigDecimal.ZERO);

        List<UserCardResponseDTO> availableCards = userCardRepository
                .findByUserIdOrderByAcquiredAtDesc(userId, PageRequest.of(0, 150))
                .getContent()
                .stream()
                .filter(uc -> !Boolean.TRUE.equals(uc.getIsLocked()))
                .filter(uc -> !islandCardIds.contains(uc.getId()))
                .map(uc -> UserCardResponseDTO.fromEntity(uc, CardTemplateResponseDTO.fromEntity(uc.getCardTemplate())))
                .toList();

        BigDecimal walletBalance = walletRepository.findByUserId(userId)
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("serverTime", now.toString());
        result.put("maxSlots", MAX_ISLAND_SLOTS);
        result.put("harvestTickSeconds", HARVEST_TICK_SECONDS);
        result.put("dailyCap", DAILY_GAME_COIN_CAP);
        result.put("dailyEarned", earnedToday);
        result.put("dailyRemaining", DAILY_GAME_COIN_CAP.subtract(earnedToday).max(BigDecimal.ZERO));
        result.put("walletBalance", walletBalance);
        result.put("slots", slotDtos);
        result.put("expeditions", expeditionDtos);
        result.put("quests", questDtos);
        result.put("availableCards", availableCards);
        return result;
    }

    /**
     * Thả 1 thẻ bài từ Kho đồ lên Đảo (tối đa 5 thẻ).
     */
    @Transactional
    public Map<String, Object> placeCardOnIsland(Long userId, Long userCardId) {
        if (sanctuarySlotRepository.countByUserId(userId) >= MAX_ISLAND_SLOTS) {
            throw new BusinessValidationException("Đảo đã đạt giới hạn tối đa " + MAX_ISLAND_SLOTS + " Pokémon! Hãy gỡ bớt trước khi thêm mới.");
        }
        UserCard card = userCardRepository.findByIdForUpdate(userCardId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thẻ bài với id = " + userCardId));

        if (!card.getUserId().equals(userId)) {
            throw new BusinessValidationException("Bạn không sở hữu thẻ bài này!");
        }
        if (Boolean.TRUE.equals(card.getIsLocked())) {
            throw new BusinessValidationException("Thẻ bài đang khóa trên Chợ/Đấu giá hoặc đang đi thám hiểm!");
        }
        if (sanctuarySlotRepository.existsByUserCardId(userCardId)) {
            throw new BusinessValidationException("Thẻ bài này đã ở trên Đảo rồi!");
        }

        SanctuarySlot saved = sanctuarySlotRepository.save(SanctuarySlot.builder()
                .userId(userId)
                .userCard(card)
                .lastHarvestAt(Instant.now().minusSeconds(HARVEST_TICK_SECONDS)) // Cho sẵn 1 bong bóng thu hoạch đầu tiên để trải nghiệm ngay!
                .build());

        log.info("User {} placed card {} on Sanctuary island slot {}", userId, userCardId, saved.getId());
        return Map.of(
                "slot", toSlotMap(saved, Instant.now()),
                "message", "Đã thả " + card.getCardTemplate().getName() + " lên Đảo Pokémon!"
        );
    }

    /**
     * Gỡ thẻ bài khỏi Đảo về lại Kho đồ.
     */
    @Transactional
    public Map<String, Object> removeCardFromIsland(Long userId, Long slotId) {
        SanctuarySlot slot = sanctuarySlotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vị trí trên Đảo."));
        if (!slot.getUserId().equals(userId)) {
            throw new BusinessValidationException("Bạn không có quyền gỡ Pokémon này!");
        }
        String name = slot.getUserCard().getCardTemplate().getName();
        sanctuarySlotRepository.delete(slot);
        return Map.of("removed", true, "slotId", slotId, "message", "Đã đưa " + name + " trở về Kho đồ.");
    }

    /**
     * Thu hoạch Xu từ 1 Pokémon trên Đảo (tính theo Instant.now() trên Server + kiểm tra Daily Cap).
     */
    @Transactional
    public Map<String, Object> harvestSlot(Long userId, Long slotId) {
        SanctuarySlot slot = sanctuarySlotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vị trí trên Đảo."));
        if (!slot.getUserId().equals(userId)) {
            throw new BusinessValidationException("Bạn không có quyền thu hoạch ô này!");
        }

        Instant now = Instant.now();
        long elapsedSeconds = Duration.between(slot.getLastHarvestAt(), now).getSeconds();
        long ticks = Math.min(MAX_ACCUMULATED_TICKS, elapsedSeconds / HARVEST_TICK_SECONDS);
        if (ticks <= 0) {
            long waitSec = HARVEST_TICK_SECONDS - Math.max(0, elapsedSeconds);
            throw new BusinessValidationException("Pokémon đang tích tụ năng lượng, vui lòng đợi thêm " + waitSec + " giây!");
        }

        Rarity rarity = slot.getUserCard().getCardTemplate().getRarity();
        BigDecimal coinPerTick = resolveHarvestCoinsPerTick(rarity);
        BigDecimal rawReward = coinPerTick.multiply(BigDecimal.valueOf(ticks)).setScale(2, RoundingMode.HALF_UP);

        BigDecimal actualCredited = creditWithDailyCap(
                userId,
                rawReward,
                TransactionType.SANCTUARY_HARVEST,
                String.format("Thu hoạch Đảo Pokémon từ %s (%d chu kỳ)", slot.getUserCard().getCardTemplate().getName(), ticks)
        );

        slot.setLastHarvestAt(now);
        sanctuarySlotRepository.save(slot);

        incrementDailyQuestProgress(userId, QUEST_HARVEST);

        Wallet wallet = walletRepository.findByUserId(userId).orElse(null);
        return Map.of(
                "slotId", slotId,
                "coinsEarned", actualCredited,
                "capped", actualCredited.compareTo(rawReward) < 0,
                "newWalletBalance", wallet != null ? wallet.getBalance() : BigDecimal.ZERO,
                "message", actualCredited.compareTo(BigDecimal.ZERO) > 0
                        ? String.format("Thu hoạch thành công +%s Xu!", actualCredited.stripTrailingZeros().toPlainString())
                        : "Đã đạt giới hạn thu nhập Đảo hôm nay (250 Xu/ngày)!"
        );
    }

    /**
     * Bắt đầu 1 chuyến Thám Hiểm (khóa thẻ tham gia, tính thời gian kết thúc hoàn toàn trên Server).
     */
    @Transactional
    public Map<String, Object> startExpedition(Long userId, StartExpeditionRequest request) {
        ExpeditionZoneConfig zone = ZONES.get(request.zoneCode().trim().toUpperCase());
        if (zone == null) {
            throw new BusinessValidationException("Vùng đất thám hiểm không hợp lệ!");
        }

        List<Long> uniqueCardIds = request.userCardIds().stream().distinct().toList();
        if (uniqueCardIds.isEmpty() || uniqueCardIds.size() > 3) {
            throw new BusinessValidationException("Mỗi chuyến thám hiểm cần từ 1 đến 3 thẻ bài!");
        }

        List<UserCard> lockedCards = new ArrayList<>();
        BigDecimal rarityBonus = BigDecimal.ZERO;

        for (Long cardId : uniqueCardIds) {
            UserCard card = userCardRepository.findByIdForUpdate(cardId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thẻ bài id = " + cardId));
            if (!card.getUserId().equals(userId)) {
                throw new BusinessValidationException("Bạn không sở hữu thẻ bài id = " + cardId);
            }
            if (Boolean.TRUE.equals(card.getIsLocked())) {
                throw new BusinessValidationException("Thẻ \"" + card.getCardTemplate().getName() + "\" đang được sử dụng hoặc rao bán!");
            }
            if (sanctuarySlotRepository.existsByUserCardId(cardId)) {
                sanctuarySlotRepository.deleteByUserCardId(cardId);
            }
            card.setIsLocked(true);
            lockedCards.add(card);
            rarityBonus = rarityBonus.add(resolveHarvestCoinsPerTick(card.getCardTemplate().getRarity()).multiply(BigDecimal.valueOf(2)));
        }
        userCardRepository.saveAll(lockedCards);

        Instant now = Instant.now();
        Instant endsAt = now.plus(Duration.ofMinutes(zone.durationMinutes()));
        BigDecimal totalRewardCoins = zone.baseCoins().setScale(2, RoundingMode.HALF_UP);

        Expedition expedition = Expedition.builder()
                .userId(userId)
                .zoneCode(zone.code())
                .zoneName(zone.name())
                .durationMinutes(zone.durationMinutes())
                .startedAt(now)
                .endsAt(endsAt)
                .rewardCoins(totalRewardCoins)
                .bonusPackChance(zone.bonusPackChance())
                .status(ExpeditionStatus.IN_PROGRESS)
                .build();

        for (UserCard c : lockedCards) {
            expedition.getCards().add(ExpeditionCard.builder()
                    .expedition(expedition)
                    .userCard(c)
                    .build());
        }

        Expedition saved = expeditionRepository.save(expedition);
        log.info("User {} started expedition {} to {} ({} min, {} cards)",
                userId, saved.getId(), zone.code(), zone.durationMinutes(), lockedCards.size());

        return Map.of(
                "expedition", toExpeditionMap(saved, now),
                "message", String.format("Đội hình %d Pokémon đã xuất phát tới %s!", lockedCards.size(), zone.name())
        );
    }

    /**
     * Nhận thưởng chuyến Thám Hiểm sau khi hoàn tất (xác thực Instant.now() >= endsAt trên Server).
     */
    @Transactional
    public Map<String, Object> claimExpedition(Long userId, Long expeditionId) {
        Expedition expedition = expeditionRepository.findByIdForUpdate(expeditionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyến thám hiểm."));
        if (!expedition.getUserId().equals(userId)) {
            throw new BusinessValidationException("Bạn không có quyền nhận thưởng chuyến thám hiểm này!");
        }
        if (expedition.getStatus() == ExpeditionStatus.CLAIMED) {
            throw new BusinessValidationException("Chuyến thám hiểm này đã được nhận thưởng trước đó!");
        }

        Instant now = Instant.now();
        if (now.isBefore(expedition.getEndsAt())) {
            long remSec = Duration.between(now, expedition.getEndsAt()).getSeconds();
            throw new BusinessValidationException("Chuyến thám hiểm chưa hoàn tất! Còn " + remSec + " giây.");
        }

        // Mở khóa lại toàn bộ thẻ bài đã tham gia
        for (ExpeditionCard ec : expedition.getCards()) {
            UserCard uc = ec.getUserCard();
            if (uc != null && uc.getUserId().equals(userId)) {
                uc.setIsLocked(false);
                userCardRepository.save(uc);
            }
        }

        BigDecimal actualCoins = creditWithDailyCap(
                userId,
                expedition.getRewardCoins(),
                TransactionType.EXPEDITION_REWARD,
                String.format("Thưởng thám hiểm %s (%d phút)", expedition.getZoneName(), expedition.getDurationMinutes())
        );

        // Quay cơ hội rơi Pack ngẫu nhiên
        Long rewardPackId = null;
        boolean bonusPackWon = SECURE_RANDOM.nextInt(100) < expedition.getBonusPackChance();
        if (bonusPackWon) {
            rewardPackId = tryGrantBonusPackToUser(userId);
        }

        expedition.setStatus(ExpeditionStatus.CLAIMED);
        expedition.setRewardPackId(rewardPackId);
        expeditionRepository.save(expedition);

        incrementDailyQuestProgress(userId, QUEST_EXPEDITION);

        Wallet wallet = walletRepository.findByUserId(userId).orElse(null);
        String msg = rewardPackId != null
                ? String.format("Nhận +%s Xu và TRÚNG THƯỞNG 1 Pack Miễn Phí (#%d)!", actualCoins.stripTrailingZeros().toPlainString(), rewardPackId)
                : String.format("Hoàn thành thám hiểm %s, nhận +%s Xu!", expedition.getZoneName(), actualCoins.stripTrailingZeros().toPlainString());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("expeditionId", expeditionId);
        response.put("coinsEarned", actualCoins);
        response.put("bonusPackWon", rewardPackId != null);
        response.put("rewardPackId", rewardPackId);
        response.put("newWalletBalance", wallet != null ? wallet.getBalance() : BigDecimal.ZERO);
        response.put("message", msg);
        return response;
    }

    /**
     * Nhận thưởng Nhiệm Vụ Ngày.
     */
    @Transactional
    public Map<String, Object> claimDailyQuest(Long userId, Long questId) {
        DailyQuest quest = dailyQuestRepository.findByIdForUpdate(questId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhiệm vụ ngày."));
        if (!quest.getUserId().equals(userId)) {
            throw new BusinessValidationException("Bạn không có quyền nhận nhiệm vụ này!");
        }
        if (Boolean.TRUE.equals(quest.getIsClaimed())) {
            throw new BusinessValidationException("Nhiệm vụ này đã được nhận thưởng!");
        }
        if (quest.getCurrentCount() < quest.getTargetCount()) {
            throw new BusinessValidationException("Nhiệm vụ chưa hoàn thành đủ tiến độ!");
        }

        BigDecimal credited = creditWithDailyCap(
                userId,
                quest.getRewardCoins(),
                TransactionType.DAILY_QUEST_REWARD,
                "Thưởng nhiệm vụ ngày: " + quest.getTitle()
        );

        quest.setIsClaimed(true);
        dailyQuestRepository.save(quest);

        Wallet wallet = walletRepository.findByUserId(userId).orElse(null);
        return Map.of(
                "questId", questId,
                "coinsEarned", credited,
                "newWalletBalance", wallet != null ? wallet.getBalance() : BigDecimal.ZERO,
                "message", String.format("Đã nhận thưởng nhiệm vụ \"%s\": +%s Xu!", quest.getTitle(), credited.stripTrailingZeros().toPlainString())
        );
    }

    /**
     * Hook tăng tiến độ nhiệm vụ ngày (được gọi từ GachaEngineService khi mở gói, hoặc nội bộ khi thu hoạch/thám hiểm).
     */
    @Transactional
    public void incrementDailyQuestProgress(Long userId, String questCode) {
        LocalDate today = LocalDate.now();
        ensureDailyQuests(userId, today);
        dailyQuestRepository.findByUserIdAndQuestDateAndQuestCode(userId, today, questCode)
                .ifPresent(q -> {
                    if (q.getCurrentCount() < q.getTargetCount()) {
                        q.setCurrentCount(q.getCurrentCount() + 1);
                        dailyQuestRepository.save(q);
                    }
                });
    }

    private List<DailyQuest> ensureDailyQuests(Long userId, LocalDate today) {
        List<DailyQuest> existing = dailyQuestRepository.findByUserIdAndQuestDateOrderByIdAsc(userId, today);
        if (!existing.isEmpty()) {
            boolean dirty = false;
            for (DailyQuest q : existing) {
                if (QUEST_OPEN_PACK.equals(q.getQuestCode()) && !"Mở 1 Pack bất kỳ".equals(q.getTitle())) {
                    q.setTitle("Mở 1 Pack bất kỳ");
                    dirty = true;
                }
                if (q.getRewardCoins() != null && q.getRewardCoins().compareTo(new BigDecimal("100.00")) > 0) {
                    q.setRewardCoins(switch (q.getQuestCode()) {
                        case QUEST_HARVEST -> new BigDecimal("25.00");
                        case QUEST_EXPEDITION -> new BigDecimal("40.00");
                        default -> new BigDecimal("35.00");
                    });
                    dirty = true;
                }
            }
            return dirty ? dailyQuestRepository.saveAll(existing) : existing;
        }
        List<DailyQuest> created = List.of(
                DailyQuest.builder()
                        .userId(userId).questDate(today).questCode(QUEST_HARVEST)
                        .title("Thu hoạch Xu trên đảo 1 lần")
                        .targetCount(1).currentCount(0).rewardCoins(new BigDecimal("25.00")).isClaimed(false).build(),
                DailyQuest.builder()
                        .userId(userId).questDate(today).questCode(QUEST_EXPEDITION)
                        .title("Hoàn thành 1 chuyến thám hiểm")
                        .targetCount(1).currentCount(0).rewardCoins(new BigDecimal("40.00")).isClaimed(false).build(),
                DailyQuest.builder()
                        .userId(userId).questDate(today).questCode(QUEST_OPEN_PACK)
                        .title("Mở 1 Pack bất kỳ")
                        .targetCount(1).currentCount(0).rewardCoins(new BigDecimal("35.00")).isClaimed(false).build()
        );
        return dailyQuestRepository.saveAll(created);
    }

    private BigDecimal creditWithDailyCap(Long userId, BigDecimal requestedAmount, TransactionType type, String description) {
        LocalDate today = LocalDate.now();
        SanctuaryDailyEarning daily = dailyEarningRepository.findByUserIdAndEarningDateForUpdate(userId, today)
                .orElseGet(() -> dailyEarningRepository.save(SanctuaryDailyEarning.builder()
                        .userId(userId)
                        .earningDate(today)
                        .coinsEarned(BigDecimal.ZERO)
                        .build()));

        BigDecimal remainingCap = DAILY_GAME_COIN_CAP.subtract(daily.getCoinsEarned()).max(BigDecimal.ZERO);
        BigDecimal allowed = requestedAmount.min(remainingCap).setScale(2, RoundingMode.HALF_UP);
        if (allowed.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví của người dùng."));
        wallet.setBalance(wallet.getBalance().add(allowed));
        Wallet savedWallet = walletRepository.save(wallet);

        daily.setCoinsEarned(daily.getCoinsEarned().add(allowed));
        dailyEarningRepository.save(daily);

        walletService.recordTransaction(savedWallet, type, allowed, description);
        return allowed;
    }

    private Long tryGrantBonusPackToUser(Long userId) {
        List<BoosterBox> boxes = boosterBoxRepository.findAllByOrderByIdAsc();
        for (BoosterBox box : boxes) {
            if (box.getStatus() != BoxStatus.AVAILABLE || box.getRemainingPacks() <= 0) continue;
            List<BoosterPack> packs = boosterPackRepository.findByBoosterBoxIdOrderByPositionInBoxAsc(box.getId());
            for (BoosterPack pack : packs) {
                if (pack.getStatus() == PackStatus.AVAILABLE) {
                    pack.setStatus(PackStatus.SOLD);
                    pack.setBuyerId(userId);
                    boosterPackRepository.save(pack);
                    boosterBoxRepository.decrementRemainingPackAtomically(box.getId());
                    log.info("Granted bonus Expedition Pack {} (box {}) to user {}", pack.getId(), box.getBoxCode(), userId);
                    return pack.getId();
                }
            }
        }
        return null;
    }

    private BigDecimal resolveHarvestCoinsPerTick(Rarity rarity) {
        String name = rarity != null && rarity.getName() != null ? rarity.getName().toLowerCase() : "";
        if (name.matches(".*(hyper|secret|gold|special illustration).*")) return new BigDecimal("25.00");
        if (name.matches(".*(\\bex\\b|double rare|ultra rare|full art|illustration rare).*")) return new BigDecimal("12.00");
        if (name.matches(".*(uncommon|không phổ biến).*")) return new BigDecimal("4.00");
        if (name.matches(".*(\\brare\\b|hiếm).*") || (rarity != null && Boolean.TRUE.equals(rarity.getIsHolographic()))) {
            return new BigDecimal("8.00");
        }
        return new BigDecimal("2.00");
    }

    private Map<String, Object> toSlotMap(SanctuarySlot slot, Instant now) {
        UserCard uc = slot.getUserCard();
        CardTemplateResponseDTO cardDto = CardTemplateResponseDTO.fromEntity(uc.getCardTemplate());
        long elapsedSeconds = Math.max(0, Duration.between(slot.getLastHarvestAt(), now).getSeconds());
        long ticks = Math.min(MAX_ACCUMULATED_TICKS, elapsedSeconds / HARVEST_TICK_SECONDS);
        BigDecimal coinPerTick = resolveHarvestCoinsPerTick(uc.getCardTemplate().getRarity());
        BigDecimal pendingCoins = coinPerTick.multiply(BigDecimal.valueOf(ticks));
        long secondsToNextTick = ticks >= MAX_ACCUMULATED_TICKS ? 0 : (HARVEST_TICK_SECONDS - (elapsedSeconds % HARVEST_TICK_SECONDS));

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("slotId", slot.getId());
        map.put("userCardId", uc.getId());
        map.put("card", cardDto);
        map.put("lastHarvestAt", slot.getLastHarvestAt().toString());
        map.put("coinPerTick", coinPerTick);
        map.put("readyTicks", ticks);
        map.put("pendingCoins", pendingCoins);
        map.put("readyToHarvest", ticks > 0);
        map.put("secondsToNextTick", secondsToNextTick);
        return map;
    }

    private Map<String, Object> toExpeditionMap(Expedition exp, Instant now) {
        long remainingSeconds = Math.max(0, Duration.between(now, exp.getEndsAt()).getSeconds());
        List<Map<String, Object>> cardSummaries = exp.getCards().stream()
                .map(ec -> {
                    CardTemplate ct = ec.getUserCard().getCardTemplate();
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("userCardId", ec.getUserCard().getId());
                    m.put("name", ct.getName());
                    m.put("imageUrl", ct.getImageUrl());
                    m.put("rarityColorHex", ct.getRarity().getColorHex());
                    return m;
                }).toList();

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", exp.getId());
        map.put("zoneCode", exp.getZoneCode());
        map.put("zoneName", exp.getZoneName());
        map.put("durationMinutes", exp.getDurationMinutes());
        map.put("startedAt", exp.getStartedAt().toString());
        map.put("endsAt", exp.getEndsAt().toString());
        map.put("remainingSeconds", remainingSeconds);
        map.put("readyToClaim", remainingSeconds == 0);
        map.put("rewardCoins", exp.getRewardCoins());
        map.put("bonusPackChance", exp.getBonusPackChance());
        map.put("cards", cardSummaries);
        return map;
    }

    private Map<String, Object> toQuestMap(DailyQuest q) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", q.getId());
        map.put("questCode", q.getQuestCode());
        map.put("title", q.getTitle());
        map.put("targetCount", q.getTargetCount());
        map.put("currentCount", q.getCurrentCount());
        map.put("rewardCoins", q.getRewardCoins());
        map.put("completed", q.getCurrentCount() >= q.getTargetCount());
        map.put("claimed", Boolean.TRUE.equals(q.getIsClaimed()));
        return map;
    }
}
