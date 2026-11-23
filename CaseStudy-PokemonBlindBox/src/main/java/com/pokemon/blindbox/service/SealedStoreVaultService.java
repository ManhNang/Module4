package com.pokemon.blindbox.service;

import com.pokemon.blindbox.config.RedisCacheConfig;
import com.pokemon.blindbox.dto.response.CardSetResponseDTO;
import com.pokemon.blindbox.dto.response.CardTemplateResponseDTO;
import com.pokemon.blindbox.dto.response.RarityResponseDTO;
import com.pokemon.blindbox.dto.response.UserCardResponseDTO;
import com.pokemon.blindbox.entity.*;
import com.pokemon.blindbox.entity.enums.SealedItemType;
import com.pokemon.blindbox.entity.enums.TransactionType;
import com.pokemon.blindbox.exception.BusinessValidationException;
import com.pokemon.blindbox.exception.ResourceNotFoundException;
import com.pokemon.blindbox.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SealedStoreVaultService quản lý:
 * 1. Cửa hàng đa Bộ sưu tập với Hạn mức bán toàn cầu theo ngày (Daily Global Stock, reset 00:00).
 *    - 1 Pack (mặc định 6 thẻ): Giá niêm yết của bộ thẻ.
 *    - 1 Box nguyên seal (20 Pack): Tự động giảm 10% = 20 * giá Pack * 0.9.
 * 2. Kho chứa Pack & Box của người chơi (UserSealedItem):
 *    - Mua từ Cửa hàng -> lưu vào Kho (không ép mở ngay).
 *    - Bóc Seal 1 Box -> tách thành 20 Pack lẻ trong Kho.
 *    - Mở 1 Pack hoặc Mở nhanh 5 Pack -> quay Gacha CSPRNG lưu vào UserCard.
 * 3. Admin quản trị Hạn mức Pack/Box bán mỗi ngày và Reset nhanh.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SealedStoreVaultService {

    private static final BigDecimal BOX_DISCOUNT_MULTIPLIER = new BigDecimal("0.9");

    private final CardSetRepository cardSetRepository;
    private final SetDailySaleRepository setDailySaleRepository;
    private final UserSealedItemRepository userSealedItemRepository;
    private final CardTemplateRepository cardTemplateRepository;
    private final UserCardRepository userCardRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final CatalogCacheService catalogCacheService;
    private final ObjectProvider<SanctuaryService> sanctuaryServiceProvider;

    public record StoreSetCatalogItem(
            Long setId,
            String setName,
            String setCode,
            String packImageUrl,
            String boxImageUrl,
            Integer baseSetTotal,
            Integer cardsPerPack,
            Integer packsPerBox,
            BigDecimal packPrice,
            BigDecimal boxOriginalPrice,
            BigDecimal boxDiscountedPrice,
            Integer dailyPackLimit,
            Integer packsSoldToday,
            Integer packsRemainingToday,
            Integer dailyBoxLimit,
            Integer boxesSoldToday,
            Integer boxesRemainingToday
    ) {}

    public record SealedSetVaultGroup(
            Long setId,
            String setName,
            String setCode,
            String packImageUrl,
            String boxImageUrl,
            Integer cardsPerPack,
            Integer packsPerBox,
            BigDecimal packPrice,
            BigDecimal boxPrice,
            long availablePacks,
            long lockedPacks,
            long availableBoxes,
            long lockedBoxes,
            Long firstAvailablePackId,
            Long firstAvailableBoxId
    ) {}

    public record SealedMarketOption(
            Long sealedItemId,
            Long setId,
            String setName,
            String setCode,
            String packImageUrl,
            String boxImageUrl,
            SealedItemType itemType,
            Integer cardsPerPack,
            Integer packsPerBox,
            BigDecimal referencePrice,
            long availableQuantity
    ) {}

    public static BigDecimal computeDiscountedBoxPrice(BigDecimal packPrice, int packsPerBox) {
        if (packPrice == null) return BigDecimal.ZERO;
        return packPrice
                .multiply(BigDecimal.valueOf(packsPerBox))
                .multiply(BOX_DISCOUNT_MULTIPLIER)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public List<StoreSetCatalogItem> getStoreCatalogWithDailyStock() {
        LocalDate today = LocalDate.now();
        Map<Long, SetDailySale> todaySales = setDailySaleRepository.findBySaleDate(today).stream()
                .collect(Collectors.toMap(s -> s.getCardSet().getId(), s -> s));

        List<CardSetResponseDTO> cachedSets = catalogCacheService.getAllCardSets();
        return cachedSets.stream()
                .filter(cs -> cs.isActive() == null || cs.isActive())
                .sorted(Comparator.comparing(CardSetResponseDTO::id))
                .map(cs -> {
                    int cardsPerPack = cs.cardsPerPack() != null && cs.cardsPerPack() > 0 ? cs.cardsPerPack() : 6;
                    int packsPerBox = cs.packsPerBox() != null && cs.packsPerBox() > 0 ? cs.packsPerBox() : 20;
                    BigDecimal packPrice = cs.packPrice() != null ? cs.packPrice() : new BigDecimal("450.00");
                    BigDecimal boxOriginal = packPrice.multiply(BigDecimal.valueOf(packsPerBox)).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal boxDiscounted = computeDiscountedBoxPrice(packPrice, packsPerBox);

                    int packLimit = cs.dailyPackLimit() != null ? cs.dailyPackLimit() : 200;
                    int boxLimit = cs.dailyBoxLimit() != null ? cs.dailyBoxLimit() : 15;

                    SetDailySale sale = todaySales.get(cs.id());
                    int packsSold = sale != null ? sale.getPacksSold() : 0;
                    int boxesSold = sale != null ? sale.getBoxesSold() : 0;

                    return new StoreSetCatalogItem(
                            cs.id(),
                            cs.name(),
                            cs.code(),
                            cs.packImageUrl(),
                            cs.boxImageUrl(),
                            cs.baseSetTotal(),
                            cardsPerPack,
                            packsPerBox,
                            packPrice,
                            boxOriginal,
                            boxDiscounted,
                            packLimit,
                            packsSold,
                            Math.max(0, packLimit - packsSold),
                            boxLimit,
                            boxesSold,
                            Math.max(0, boxLimit - boxesSold)
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, SetDailySale> getTodaySalesMap() {
        return setDailySaleRepository.findBySaleDate(LocalDate.now()).stream()
                .collect(Collectors.toMap(s -> s.getCardSet().getId(), s -> s));
    }

    /**
     * Mua Pack lẻ tại Cửa hàng -> cộng trực tiếp vào Kho đồ (UserSealedItem).
     * P1-6: Dùng INSERT IGNORE + Atomic Conditional UPDATE để tránh giữ khóa PESSIMISTIC_WRITE
     * trên dòng hot-row set_daily_sales suốt thời gian trừ tiền ví & ghi lịch sử.
     */
    @Transactional
    public Map<String, Object> buyPacksFromStore(Long userId, Long setId, int quantity) {
        if (quantity < 1 || quantity > 20) {
            throw new BusinessValidationException("Số lượng mua mỗi lần từ 1 đến 20 Pack!");
        }
        CardSet cardSet = cardSetRepository.findById(setId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Bộ sưu tập với id = " + setId));
        if (Boolean.FALSE.equals(cardSet.getIsActive())) {
            throw new BusinessValidationException("Bộ sưu tập này hiện đã ngừng mở bán!");
        }

        LocalDate today = LocalDate.now();
        int limit = cardSet.getDailyPackLimit() != null ? cardSet.getDailyPackLimit() : 200;
        setDailySaleRepository.initDailySaleIfAbsent(setId, today);
        int updated = setDailySaleRepository.incrementPacksSoldAtomically(setId, today, quantity, limit);
        if (updated == 0) {
            int currentSold = setDailySaleRepository.findByCardSetIdAndSaleDate(setId, today)
                    .map(SetDailySale::getPacksSold)
                    .orElse(limit);
            int remaining = Math.max(0, limit - currentSold);
            throw new BusinessValidationException(remaining <= 0
                    ? "Đã hết suất Pack hôm nay cho bộ " + cardSet.getName() + "! Hãy tìm mua trên Chợ."
                    : "Chỉ còn " + remaining + " Pack hôm nay cho bộ " + cardSet.getName() + "!");
        }

        int cardsPerPack = cardSet.getCardsPerPack() != null && cardSet.getCardsPerPack() > 0 ? cardSet.getCardsPerPack() : 6;
        int packsPerBox = cardSet.getPacksPerBox() != null && cardSet.getPacksPerBox() > 0 ? cardSet.getPacksPerBox() : 20;
        BigDecimal totalCost = cardSet.getPackPrice().multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);

        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví của người chơi!"));
        wallet.debit(totalCost);
        Wallet savedWallet = walletRepository.saveAndFlush(wallet);

        walletService.recordTransaction(
                savedWallet,
                TransactionType.PURCHASE_PACK,
                totalCost,
                String.format("Mua %d Pack bộ %s (%s) vào Kho đồ", quantity, cardSet.getName(), cardSet.getCode())
        );

        List<UserSealedItem> items = new ArrayList<>(quantity);
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < quantity; i++) {
            items.add(UserSealedItem.builder()
                    .userId(userId)
                    .cardSet(cardSet)
                    .itemType(SealedItemType.PACK)
                    .cardsPerPack(cardsPerPack)
                    .packsPerBox(packsPerBox)
                    .isLocked(false)
                    .isConsumed(false)
                    .acquiredAt(now)
                    .build());
        }
        userSealedItemRepository.saveAll(items);

        int packsSoldToday = setDailySaleRepository.findByCardSetIdAndSaleDate(setId, today)
                .map(SetDailySale::getPacksSold)
                .orElse(quantity);
        int newRemaining = Math.max(0, limit - packsSoldToday);
        log.info("User {} bought {} PACK(s) of set {} (remaining today: {})", userId, quantity, cardSet.getCode(), newRemaining);

        return Map.of(
                "itemType", "PACK",
                "setId", setId,
                "setName", cardSet.getName(),
                "quantity", quantity,
                "totalCost", totalCost,
                "newWalletBalance", savedWallet.getBalance(),
                "packsRemainingToday", newRemaining,
                "packsSoldToday", packsSoldToday,
                "message", String.format("Đã mua %d Pack \"%s\" và lưu vào Kho đồ!", quantity, cardSet.getName())
        );
    }

    /**
     * Mua Box nguyên seal (20 Pack, giảm 10%) tại Cửa hàng -> cộng trực tiếp vào Kho đồ.
     */
    @Transactional
    public Map<String, Object> buyBoxesFromStore(Long userId, Long setId, int quantity) {
        if (quantity < 1 || quantity > 5) {
            throw new BusinessValidationException("Số lượng mua mỗi lần từ 1 đến 5 Box nguyên seal!");
        }
        CardSet cardSet = cardSetRepository.findById(setId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Bộ sưu tập với id = " + setId));
        if (Boolean.FALSE.equals(cardSet.getIsActive())) {
            throw new BusinessValidationException("Bộ sưu tập này hiện đã ngừng mở bán!");
        }

        LocalDate today = LocalDate.now();
        int limit = cardSet.getDailyBoxLimit() != null ? cardSet.getDailyBoxLimit() : 15;
        setDailySaleRepository.initDailySaleIfAbsent(setId, today);
        int updated = setDailySaleRepository.incrementBoxesSoldAtomically(setId, today, quantity, limit);
        if (updated == 0) {
            int currentSold = setDailySaleRepository.findByCardSetIdAndSaleDate(setId, today)
                    .map(SetDailySale::getBoxesSold)
                    .orElse(limit);
            int remaining = Math.max(0, limit - currentSold);
            throw new BusinessValidationException(remaining <= 0
                    ? "Đã hết suất Box hôm nay cho bộ " + cardSet.getName() + "! Hãy tìm mua trên Chợ."
                    : "Chỉ còn " + remaining + " Box hôm nay cho bộ " + cardSet.getName() + "!");
        }

        int cardsPerPack = cardSet.getCardsPerPack() != null && cardSet.getCardsPerPack() > 0 ? cardSet.getCardsPerPack() : 6;
        int packsPerBox = cardSet.getPacksPerBox() != null && cardSet.getPacksPerBox() > 0 ? cardSet.getPacksPerBox() : 20;
        BigDecimal unitBoxPrice = computeDiscountedBoxPrice(cardSet.getPackPrice(), packsPerBox);
        BigDecimal totalCost = unitBoxPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);

        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví của người chơi!"));
        wallet.debit(totalCost);
        Wallet savedWallet = walletRepository.saveAndFlush(wallet);

        walletService.recordTransaction(
                savedWallet,
                TransactionType.PURCHASE_BOX,
                totalCost,
                String.format("Mua %d Box nguyên seal (%d Pack/Box, -10%%) bộ %s vào Kho đồ", quantity, packsPerBox, cardSet.getName())
        );

        List<UserSealedItem> items = new ArrayList<>(quantity);
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < quantity; i++) {
            items.add(UserSealedItem.builder()
                    .userId(userId)
                    .cardSet(cardSet)
                    .itemType(SealedItemType.BOX)
                    .cardsPerPack(cardsPerPack)
                    .packsPerBox(packsPerBox)
                    .isLocked(false)
                    .isConsumed(false)
                    .acquiredAt(now)
                    .build());
        }
        userSealedItemRepository.saveAll(items);

        int boxesSoldToday = setDailySaleRepository.findByCardSetIdAndSaleDate(setId, today)
                .map(SetDailySale::getBoxesSold)
                .orElse(quantity);
        int newRemaining = Math.max(0, limit - boxesSoldToday);
        log.info("User {} bought {} BOX(es) of set {} (remaining today: {})", userId, quantity, cardSet.getCode(), newRemaining);

        return Map.of(
                "itemType", "BOX",
                "setId", setId,
                "setName", cardSet.getName(),
                "quantity", quantity,
                "totalCost", totalCost,
                "newWalletBalance", savedWallet.getBalance(),
                "boxesRemainingToday", newRemaining,
                "boxesSoldToday", boxesSoldToday,
                "message", String.format("Đã mua %d Box nguyên seal \"%s\" và lưu vào Kho đồ!", quantity, cardSet.getName())
        );
    }

    /**
     * Lấy danh sách Pack & Box trong Kho đồ của người chơi gom theo từng Bộ sưu tập.
     * P1-6: Gom nhóm trực tiếp bằng câu lệnh JPQL GROUP BY (COUNT + MIN(id)) thay vì kéo hàng nghìn dòng vào RAM.
     */
    @Transactional(readOnly = true)
    public List<SealedSetVaultGroup> getUserSealedVaultGroups(Long userId) {
        List<UserSealedItemRepository.SealedVaultAggProjection> rows =
                userSealedItemRepository.aggregateVaultByUserId(userId);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }

        Map<Long, List<UserSealedItemRepository.SealedVaultAggProjection>> bySet = rows.stream()
                .collect(Collectors.groupingBy(
                        UserSealedItemRepository.SealedVaultAggProjection::getSetId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<SealedSetVaultGroup> groups = new ArrayList<>();
        for (List<UserSealedItemRepository.SealedVaultAggProjection> list : bySet.values()) {
            UserSealedItemRepository.SealedVaultAggProjection head = list.getFirst();
            int cardsPerPack = head.getSetCardsPerPack() != null ? head.getSetCardsPerPack() : 6;
            int packsPerBox = head.getSetPacksPerBox() != null ? head.getSetPacksPerBox() : 20;
            BigDecimal packPrice = head.getPackPrice() != null ? head.getPackPrice() : new BigDecimal("450.00");
            BigDecimal boxPrice = computeDiscountedBoxPrice(packPrice, packsPerBox);

            long availPacks = 0, lockedPacks = 0, availBoxes = 0, lockedBoxes = 0;
            Long firstAvailPackId = null, firstAvailBoxId = null;

            for (UserSealedItemRepository.SealedVaultAggProjection r : list) {
                boolean locked = Boolean.TRUE.equals(r.getIsLocked());
                long count = r.getTotalCount() != null ? r.getTotalCount() : 0L;
                if (r.getItemType() == SealedItemType.PACK) {
                    if (locked) {
                        lockedPacks += count;
                    } else {
                        availPacks += count;
                        if (firstAvailPackId == null) firstAvailPackId = r.getFirstItemId();
                    }
                } else {
                    if (locked) {
                        lockedBoxes += count;
                    } else {
                        availBoxes += count;
                        if (firstAvailBoxId == null) firstAvailBoxId = r.getFirstItemId();
                    }
                }
            }

            groups.add(new SealedSetVaultGroup(
                    head.getSetId(), head.getSetName(), head.getSetCode(),
                    head.getPackImageUrl(), head.getBoxImageUrl(),
                    cardsPerPack, packsPerBox, packPrice, boxPrice,
                    availPacks, lockedPacks, availBoxes, lockedBoxes,
                    firstAvailPackId, firstAvailBoxId
            ));
        }
        return groups;
    }

    /**
     * Lấy danh sách Pack & Box sẵn sàng đưa lên Chợ (gom theo Bộ + Loại) cho Popup Đăng bán.
     * P1-6: Tái sử dụng kết quả GROUP BY từ DB thay vì load toàn bộ entity UserSealedItem.
     */
    @Transactional(readOnly = true)
    public List<SealedMarketOption> getSellableSealedOptions(Long userId) {
        List<UserSealedItemRepository.SealedVaultAggProjection> rows =
                userSealedItemRepository.aggregateVaultByUserId(userId);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }

        List<SealedMarketOption> result = new ArrayList<>();
        for (UserSealedItemRepository.SealedVaultAggProjection r : rows) {
            if (Boolean.TRUE.equals(r.getIsLocked())) {
                continue;
            }
            long qty = r.getTotalCount() != null ? r.getTotalCount() : 0L;
            if (qty <= 0 || r.getFirstItemId() == null) {
                continue;
            }
            int cardsPerPack = r.getSetCardsPerPack() != null ? r.getSetCardsPerPack() : 6;
            int packsPerBox = r.getSetPacksPerBox() != null ? r.getSetPacksPerBox() : 20;
            BigDecimal packPrice = r.getPackPrice() != null ? r.getPackPrice() : new BigDecimal("450.00");
            BigDecimal refPrice = r.getItemType() == SealedItemType.BOX
                    ? computeDiscountedBoxPrice(packPrice, packsPerBox)
                    : packPrice;

            result.add(new SealedMarketOption(
                    r.getFirstItemId(),
                    r.getSetId(),
                    r.getSetName(),
                    r.getSetCode(),
                    r.getPackImageUrl(),
                    r.getBoxImageUrl(),
                    r.getItemType(),
                    cardsPerPack,
                    packsPerBox,
                    refPrice,
                    qty
            ));
        }
        return result;
    }

    /**
     * Bóc Seal 1 Box nguyên seal trong Kho -> tách thành 20 Pack lẻ.
     */
    @Transactional
    public Map<String, Object> unsealBox(Long userId, Long setId) {
        List<UserSealedItem> boxes = userSealedItemRepository.findAvailableForUpdate(
                userId, setId, SealedItemType.BOX, PageRequest.of(0, 1)
        );
        if (boxes.isEmpty()) {
            throw new BusinessValidationException("Bạn không có Box nguyên seal nào sẵn sàng để bóc trong bộ này!");
        }
        UserSealedItem box = boxes.getFirst();
        box.setIsConsumed(true);
        userSealedItemRepository.saveAndFlush(box);

        int packsToCreate = box.getPacksPerBox() != null && box.getPacksPerBox() > 0 ? box.getPacksPerBox() : 20;
        int cardsPerPack = box.getCardsPerPack() != null && box.getCardsPerPack() > 0 ? box.getCardsPerPack() : 6;
        LocalDateTime now = LocalDateTime.now();

        List<UserSealedItem> spawnedPacks = new ArrayList<>(packsToCreate);
        for (int i = 0; i < packsToCreate; i++) {
            spawnedPacks.add(UserSealedItem.builder()
                    .userId(userId)
                    .cardSet(box.getCardSet())
                    .itemType(SealedItemType.PACK)
                    .cardsPerPack(cardsPerPack)
                    .packsPerBox(packsToCreate)
                    .isLocked(false)
                    .isConsumed(false)
                    .acquiredAt(now)
                    .build());
        }
        userSealedItemRepository.saveAll(spawnedPacks);

        log.info("User {} unsealed 1 BOX of set {} into {} PACKs", userId, box.getCardSet().getCode(), packsToCreate);
        return Map.of(
                "unsealed", true,
                "setId", setId,
                "packsAdded", packsToCreate,
                "message", String.format("Đã bóc seal 1 Box \"%s\" thành +%d Pack lẻ!", box.getCardSet().getName(), packsToCreate)
        );
    }

    /**
     * Mở 1 Pack hoặc Mở nhanh 5 Pack từ Kho đồ -> quay CSPRNG và thêm vào UserCard.
     */
    @Transactional
    public Map<String, Object> openSealedPacks(Long userId, Long setId, int count) {
        if (count != 1 && count != 5) {
            throw new BusinessValidationException("Chỉ hỗ trợ mở 1 Pack hoặc mở nhanh 5 Pack mỗi lượt!");
        }

        List<UserSealedItem> packs = userSealedItemRepository.findAvailableForUpdate(
                userId, setId, SealedItemType.PACK, PageRequest.of(0, count)
        );
        if (packs.size() < count) {
            throw new BusinessValidationException(String.format("Bạn cần ít nhất %d Pack sẵn sàng (hiện có %d)!", count, packs.size()));
        }

        List<RarityResponseDTO> cachedRarities = catalogCacheService.getAllRaritiesWithWeights();
        List<GachaEngineService.WeightedRarityPool> activePools =
                GachaEngineService.buildActiveRarityPools(catalogCacheService, setId, cachedRarities);
        if (activePools.isEmpty()) {
            throw new BusinessValidationException("Bộ sưu tập chưa có khuôn mẫu thẻ bài hợp lệ để mở Pack!");
        }
        int totalWeight = activePools.stream().mapToInt(GachaEngineService.WeightedRarityPool::dropWeight).sum();

        LocalDateTime now = LocalDateTime.now();
        List<UserCard> cardsToSave = new ArrayList<>();
        List<CardTemplateResponseDTO> chosenDtos = new ArrayList<>();

        for (UserSealedItem pack : packs) {
            pack.setIsConsumed(true);
            int cardsPerPack = pack.getCardsPerPack() != null && pack.getCardsPerPack() > 0 ? pack.getCardsPerPack() : 6;
            for (int i = 0; i < cardsPerPack; i++) {
                GachaEngineService.WeightedRarityPool pool = GachaEngineService.rollRarityPool(activePools, totalWeight);
                CardTemplateResponseDTO dto = GachaEngineService.drawRandomTemplateFromPool(pool);
                CardTemplate ref = cardTemplateRepository.getReferenceById(dto.id());
                cardsToSave.add(UserCard.builder()
                        .userId(userId)
                        .cardTemplate(ref)
                        .boosterPack(null)
                        .isLocked(false)
                        .acquiredAt(now)
                        .build());
                chosenDtos.add(dto);
            }
        }

        userSealedItemRepository.saveAllAndFlush(packs);
        List<UserCard> savedCards = userCardRepository.saveAllAndFlush(cardsToSave);

        if (sanctuaryServiceProvider != null) {
            sanctuaryServiceProvider.ifAvailable(s -> s.incrementDailyQuestProgress(userId, SanctuaryService.QUEST_OPEN_PACK));
        }

        List<UserCardResponseDTO> drawnCards = new ArrayList<>(savedCards.size());
        for (int i = 0; i < savedCards.size(); i++) {
            drawnCards.add(UserCardResponseDTO.fromEntity(savedCards.get(i), chosenDtos.get(i)));
        }

        UserSealedItem firstPack = packs.getFirst();
        String setName = firstPack.getCardSet() != null ? firstPack.getCardSet().getName() : "Pokémon";
        int resolvedCardsPerPack = firstPack.getCardsPerPack() != null && firstPack.getCardsPerPack() > 0
                ? firstPack.getCardsPerPack() : 6;
        log.info("User {} opened {} sealed PACK(s) of set {}, drew {} cards", userId, count, setName, drawnCards.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("setId", setId);
        result.put("setName", setName);
        result.put("packsOpened", count);
        result.put("cardsPerPack", resolvedCardsPerPack);
        result.put("drawnCards", drawnCards);
        result.put("message", String.format("Đã mở %d Pack \"%s\" và nhận %d thẻ bài mới!", count, setName, drawnCards.size()));
        return result;
    }

    /**
     * Admin: Cập nhật Hạn mức Pack/Box bán mỗi ngày cho Bộ sưu tập.
     */
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(
            value = {RedisCacheConfig.CACHE_CARD_SETS, RedisCacheConfig.CACHE_CARD_TEMPLATES},
            allEntries = true
    )
    public Map<String, Object> updateDailyLimits(Long setId, Integer dailyPackLimit, Integer dailyBoxLimit) {
        CardSet cardSet = cardSetRepository.findById(setId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Bộ sưu tập với id = " + setId));
        if (dailyPackLimit != null && dailyPackLimit >= 0) {
            cardSet.setDailyPackLimit(dailyPackLimit);
        }
        if (dailyBoxLimit != null && dailyBoxLimit >= 0) {
            cardSet.setDailyBoxLimit(dailyBoxLimit);
        }
        cardSetRepository.saveAndFlush(cardSet);

        return Map.of(
                "setId", setId,
                "dailyPackLimit", cardSet.getDailyPackLimit(),
                "dailyBoxLimit", cardSet.getDailyBoxLimit(),
                "message", "Đã cập nhật hạn mức bán trong ngày cho bộ " + cardSet.getName()
        );
    }

    /**
     * Admin: Reset số lượng Pack & Box đã bán hôm nay về 0.
     */
    @Transactional
    public Map<String, Object> resetDailySales(Long setId) {
        CardSet cardSet = cardSetRepository.findById(setId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Bộ sưu tập với id = " + setId));
        LocalDate today = LocalDate.now();
        SetDailySale sale = lockOrCreateDailySale(cardSet, today);
        sale.setPacksSold(0);
        sale.setBoxesSold(0);
        setDailySaleRepository.saveAndFlush(sale);

        return Map.of(
                "setId", setId,
                "packsSoldToday", 0,
                "boxesSoldToday", 0,
                "message", "Đã reset số lượng bán hôm nay của bộ \"" + cardSet.getName() + "\" về 0!"
        );
    }

    private SetDailySale lockOrCreateDailySale(CardSet cardSet, LocalDate today) {
        setDailySaleRepository.initDailySaleIfAbsent(cardSet.getId(), today);
        return setDailySaleRepository.findByCardSetIdAndSaleDateForUpdate(cardSet.getId(), today)
                .orElseGet(() -> setDailySaleRepository.saveAndFlush(SetDailySale.builder()
                        .cardSet(cardSet)
                        .saleDate(today)
                        .packsSold(0)
                        .boxesSold(0)
                        .build()));
    }
}
