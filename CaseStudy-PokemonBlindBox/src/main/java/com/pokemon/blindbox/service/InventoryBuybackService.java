package com.pokemon.blindbox.service;

import com.pokemon.blindbox.dto.request.BulkSellToShopRequest;
import com.pokemon.blindbox.entity.Rarity;
import com.pokemon.blindbox.entity.UserCard;
import com.pokemon.blindbox.entity.Wallet;
import com.pokemon.blindbox.entity.enums.ExpeditionStatus;
import com.pokemon.blindbox.entity.enums.TransactionType;
import com.pokemon.blindbox.exception.BusinessValidationException;
import com.pokemon.blindbox.exception.ResourceNotFoundException;
import com.pokemon.blindbox.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dịch vụ Thu mua Thẻ bài trực tiếp cho Cửa hàng (NPC Buyback).
 * Đảm bảo chống IDOR, kiểm tra khóa Chợ/Đấu giá/Thám hiểm và cộng tiền vào ví trong 1 @Transactional.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryBuybackService {

    private static final Long SYSTEM_SHOP_USER_ID = 0L;

    private final UserCardRepository userCardRepository;
    private final SanctuarySlotRepository sanctuarySlotRepository;
    private final ExpeditionCardRepository expeditionCardRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;

    @Transactional
    public Map<String, Object> sellCardToShop(Long userId, Long userCardId) {
        UserCard userCard = userCardRepository.findByIdForUpdate(userCardId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thẻ bài với id = " + userCardId));

        if (!userCard.getUserId().equals(userId)) {
            throw new BusinessValidationException("Bạn không có quyền bán thẻ bài này!");
        }
        if (Boolean.TRUE.equals(userCard.getIsLocked())) {
            throw new BusinessValidationException("Thẻ bài đang được rao bán trên Chợ, đang đấu giá hoặc đang đi thám hiểm!");
        }
        if (expeditionCardRepository.existsByUserCardIdAndExpeditionStatus(userCardId, ExpeditionStatus.IN_PROGRESS)) {
            throw new BusinessValidationException("Thẻ bài đang tham gia chuyến thám hiểm, không thể bán cho Cửa hàng!");
        }

        // Nếu thẻ đang đứng chơi trên Đảo thì tự động gỡ khỏi Đảo trước khi bán
        if (sanctuarySlotRepository.existsByUserCardId(userCardId)) {
            sanctuarySlotRepository.deleteByUserCardId(userCardId);
        }

        Rarity rarity = userCard.getCardTemplate().getRarity();
        BigDecimal buybackPrice = (rarity.getBuybackPrice() != null
                ? rarity.getBuybackPrice()
                : new BigDecimal("5.00")).setScale(2, RoundingMode.HALF_UP);

        String cardName = userCard.getCardTemplate().getName();
        String rarityName = rarity.getName();

        // Khóa ví PESSIMISTIC_WRITE & cộng tiền
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví của người dùng."));
        wallet.setBalance(wallet.getBalance().add(buybackPrice));
        Wallet savedWallet = walletRepository.save(wallet);

        // Chuyển quyền sở hữu thẻ về tài khoản Hệ thống (0L) & khóa cứng để bảo toàn toàn vẹn FK lịch sử Chợ/Đấu giá
        userCard.setUserId(SYSTEM_SHOP_USER_ID);
        userCard.setIsLocked(true);
        userCardRepository.save(userCard);

        walletService.recordTransaction(
                savedWallet,
                TransactionType.SHOP_BUYBACK,
                buybackPrice,
                String.format("Bán thẻ \"%s\" (%s) cho Cửa hàng", cardName, rarityName)
        );

        log.info("User {} sold UserCard {} ({}, {}) to NPC Shop for {} VND",
                userId, userCardId, cardName, rarityName, buybackPrice);

        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("userCardId", userCardId);
        extra.put("cardName", cardName);
        extra.put("buybackPrice", buybackPrice);
        extra.put("message", String.format("Đã bán thành công +%s Xu!", formatVnd(buybackPrice)));

        return buildBuybackResponse(
                userId,
                List.of(userCard.getCardTemplate().getId()),
                buybackPrice,
                savedWallet.getBalance(),
                extra
        );
    }

    /**
     * Bán nhanh hàng loạt (hoặc bán nhiều bản sao cùng loại) trong 1 Transaction duy nhất.
     */
    @Transactional
    public Map<String, Object> sellCardsBulkToShop(Long userId, BulkSellToShopRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new BusinessValidationException("Vui lòng chọn ít nhất 1 thẻ bài để bán nhanh!");
        }

        // Gộp các item trùng cardTemplateId
        Map<Long, Integer> requestedQtyByTemplate = new LinkedHashMap<>();
        for (BulkSellToShopRequest.Item item : request.items()) {
            if (item == null || item.cardTemplateId() == null || item.quantity() == null || item.quantity() <= 0) {
                throw new BusinessValidationException("Dữ liệu thẻ bài cần bán không hợp lệ!");
            }
            requestedQtyByTemplate.merge(item.cardTemplateId(), item.quantity(), Integer::sum);
        }

        List<UserCard> unlockedCards = userCardRepository.findUnlockedByUserIdAndTemplateIdsForUpdate(
                userId, requestedQtyByTemplate.keySet()
        );

        // Batch 1 query kiểm tra thẻ đang đi thám hiểm (loại bỏ hoàn toàn N+1 query)
        List<Long> unlockedCardIds = unlockedCards.stream().map(UserCard::getId).toList();
        Set<Long> activeExpeditionCardIds = unlockedCardIds.isEmpty()
                ? Collections.emptySet()
                : expeditionCardRepository.findActiveExpeditionCardIdsByUserCardIdIn(unlockedCardIds, ExpeditionStatus.IN_PROGRESS);

        Map<Long, List<UserCard>> availableByTemplate = unlockedCards.stream()
                .filter(uc -> !activeExpeditionCardIds.contains(uc.getId()))
                .collect(Collectors.groupingBy(uc -> uc.getCardTemplate().getId()));

        List<UserCard> cardsToSell = new ArrayList<>();
        BigDecimal totalBuyback = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (Map.Entry<Long, Integer> entry : requestedQtyByTemplate.entrySet()) {
            Long templateId = entry.getKey();
            int qty = entry.getValue();
            List<UserCard> available = availableByTemplate.getOrDefault(templateId, Collections.emptyList());
            if (available.size() < qty) {
                throw new BusinessValidationException(String.format(
                        "Không đủ bản sao khả dụng trong kho để bán (%d yêu cầu, chỉ còn %d thẻ chưa khóa).",
                        qty, available.size()
                ));
            }
            for (int i = 0; i < qty; i++) {
                UserCard uc = available.get(i);
                Rarity rarity = uc.getCardTemplate().getRarity();
                BigDecimal unitPrice = (rarity.getBuybackPrice() != null
                        ? rarity.getBuybackPrice()
                        : new BigDecimal("5.00")).setScale(2, RoundingMode.HALF_UP);
                totalBuyback = totalBuyback.add(unitPrice);
                cardsToSell.add(uc);
            }
        }

        // Batch 1 lệnh DELETE gỡ toàn bộ thẻ bán khỏi Đảo nếu có (loại bỏ 2N queries)
        List<Long> soldCardIds = cardsToSell.stream().map(UserCard::getId).toList();
        if (!soldCardIds.isEmpty()) {
            sanctuarySlotRepository.deleteByUserCardIdIn(soldCardIds);
        }
        for (UserCard uc : cardsToSell) {
            uc.setUserId(SYSTEM_SHOP_USER_ID);
            uc.setIsLocked(true);
        }

        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví của người dùng."));
        wallet.setBalance(wallet.getBalance().add(totalBuyback));
        Wallet savedWallet = walletRepository.save(wallet);

        userCardRepository.saveAll(cardsToSell);

        int soldCount = cardsToSell.size();
        String txDesc = soldCount == 1
                ? String.format("Bán thẻ \"%s\" (%s) cho Cửa hàng",
                cardsToSell.get(0).getCardTemplate().getName(),
                cardsToSell.get(0).getCardTemplate().getRarity().getName())
                : String.format("Bán nhanh hàng loạt %d thẻ bài cho Cửa hàng", soldCount);

        walletService.recordTransaction(savedWallet, TransactionType.SHOP_BUYBACK, totalBuyback, txDesc);

        log.info("User {} bulk-sold {} cards to NPC Shop for {} VND", userId, soldCount, totalBuyback);

        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("soldCount", soldCount);
        extra.put("totalBuyback", totalBuyback);
        extra.put("message", String.format("Đã bán thành công %d thẻ (+%s Xu)!", soldCount, formatVnd(totalBuyback)));

        return buildBuybackResponse(
                userId,
                requestedQtyByTemplate.keySet(),
                totalBuyback,
                savedWallet.getBalance(),
                extra
        );
    }

    private Map<String, Object> buildBuybackResponse(
            Long userId,
            Collection<Long> affectedTemplateIds,
            BigDecimal earnedAmount,
            BigDecimal newBalance,
            Map<String, Object> extraFields
    ) {
        long totalCards = userCardRepository.countByUserId(userId);
        List<UserCard> remainingCards = affectedTemplateIds.isEmpty()
                ? Collections.emptyList()
                : userCardRepository.findByUserIdAndCardTemplateIdInOrderByIdAsc(userId, affectedTemplateIds);

        Map<Long, List<UserCard>> remainingByTemplate = remainingCards.stream()
                .collect(Collectors.groupingBy(uc -> uc.getCardTemplate().getId()));

        List<Map<String, Object>> updatedItems = new ArrayList<>();
        for (Long templateId : affectedTemplateIds) {
            List<UserCard> list = remainingByTemplate.getOrDefault(templateId, Collections.emptyList());
            List<UserCard> unlocked = list.stream()
                    .filter(uc -> !Boolean.TRUE.equals(uc.getIsLocked()))
                    .toList();
            Map<String, Object> itemMap = new LinkedHashMap<>();
            itemMap.put("cardTemplateId", templateId);
            itemMap.put("remainingQuantity", list.size());
            itemMap.put("remainingSellableQuantity", unlocked.size());
            itemMap.put("nextUnlockedUserCardId", unlocked.isEmpty() ? null : unlocked.get(0).getId());
            updatedItems.add(itemMap);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sold", true);
        response.put("newBalance", newBalance);
        response.put("newWalletBalance", newBalance);
        response.put("totalCards", totalCards);
        response.put("earnedAmount", earnedAmount);
        response.put("updatedItems", updatedItems);
        if (extraFields != null) {
            response.putAll(extraFields);
        }
        return response;
    }

    private String formatVnd(BigDecimal amount) {
        NumberFormat nf = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
        nf.setMaximumFractionDigits(0);
        return nf.format(amount != null ? amount : BigDecimal.ZERO);
    }
}
