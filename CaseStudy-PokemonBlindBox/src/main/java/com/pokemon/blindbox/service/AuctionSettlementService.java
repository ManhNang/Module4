package com.pokemon.blindbox.service;

import com.pokemon.blindbox.entity.Auction;
import com.pokemon.blindbox.entity.UserCard;
import com.pokemon.blindbox.entity.UserSealedItem;
import com.pokemon.blindbox.entity.Wallet;
import com.pokemon.blindbox.entity.enums.AuctionStatus;
import com.pokemon.blindbox.entity.enums.MarketItemType;
import com.pokemon.blindbox.entity.enums.TransactionType;
import com.pokemon.blindbox.repository.AuctionRepository;
import com.pokemon.blindbox.repository.UserCardRepository;
import com.pokemon.blindbox.repository.UserSealedItemRepository;
import com.pokemon.blindbox.repository.WalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AuctionSettlementService (P0-03, GAP-01, GAP-02):
 * - Cô lập mỗi phiên đấu giá trong một transaction độc lập (PROPAGATION_REQUIRES_NEW)
 *   để 1 phiên lỗi không làm rollback 19 phiên còn lại (GAP-01).
 * - Khóa theo thứ tự chuẩn: Auction (SKIP LOCKED) -> UserCard -> Wallet(minId) -> Wallet(maxId) (GAP-02).
 */
@Service
@Slf4j
public class AuctionSettlementService {

    private static final int BATCH_SIZE = 20;

    private final AuctionRepository auctionRepository;
    private final UserCardRepository userCardRepository;
    private final UserSealedItemRepository userSealedItemRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final TransactionTemplate requiresNewTxTemplate;

    public AuctionSettlementService(
            AuctionRepository auctionRepository,
            UserCardRepository userCardRepository,
            UserSealedItemRepository userSealedItemRepository,
            WalletRepository walletRepository,
            WalletService walletService,
            PlatformTransactionManager transactionManager
    ) {
        this.auctionRepository = auctionRepository;
        this.userCardRepository = userCardRepository;
        this.userSealedItemRepository = userSealedItemRepository;
        this.walletRepository = walletRepository;
        this.walletService = walletService;
        this.requiresNewTxTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTxTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Scheduled(fixedDelayString = "${app.auction.settlement-delay-ms:10000}")
    public void settleExpiredAuctions() {
        List<Long> expiredIds = auctionRepository.findExpiredActiveAuctionIds(
                AuctionStatus.ACTIVE,
                LocalDateTime.now(),
                PageRequest.of(0, BATCH_SIZE)
        );
        if (expiredIds.isEmpty()) {
            return;
        }

        for (Long auctionId : expiredIds) {
            try {
                requiresNewTxTemplate.executeWithoutResult(status -> settleSingleAuction(auctionId));
            } catch (Exception ex) {
                log.error("[AUCTION-SETTLEMENT] Lỗi khi tất toán phiên đấu giá #{}: {}", auctionId, ex.getMessage(), ex);
            }
        }
    }

    private void settleSingleAuction(Long auctionId) {
        // 1. Khóa Auction trước bằng FOR UPDATE SKIP LOCKED (multi-instance safe & đúng thứ tự khóa GAP-02)
        Auction auction = auctionRepository.findActiveByIdForUpdateSkipLocked(auctionId).orElse(null);
        if (auction == null || auction.getStatus() != AuctionStatus.ACTIVE || LocalDateTime.now().isBefore(auction.getEndTime())) {
            return;
        }

        boolean isSealed = auction.getItemType() == MarketItemType.PACK || auction.getItemType() == MarketItemType.BOX;
        UserCard card = (!isSealed && auction.getUserCard() != null)
                ? userCardRepository.findByIdForUpdate(auction.getUserCard().getId()).orElse(auction.getUserCard())
                : null;
        UserSealedItem sealed = (isSealed && auction.getSealedItem() != null)
                ? userSealedItemRepository.findByIdForUpdate(auction.getSealedItem().getId()).orElse(auction.getSealedItem())
                : null;

        Long winnerId = auction.getHighestBidderId();
        if (winnerId == null) {
            if (isSealed && sealed != null) {
                sealed.setIsLocked(false);
                userSealedItemRepository.saveAndFlush(sealed);
            } else if (card != null) {
                card.setIsLocked(false);
                userCardRepository.saveAndFlush(card);
            }

            auction.setStatus(AuctionStatus.CANCELLED);
            auctionRepository.saveAndFlush(auction);

            log.info("[AUCTION-SETTLEMENT] Cancelled auction #{} (no bids), unlocked asset", auctionId);
            return;
        }

        // Có người thắng -> Khóa 2 ví theo thứ tự userId tăng dần (chống Deadlock)
        Long sellerId = auction.getSellerId();
        BigDecimal winningAmount = auction.getCurrentHighestBid();

        Map<Long, Wallet> lockedWallets = walletService.lockTwoWalletsInDeterministicOrder(winnerId, sellerId);
        Wallet winnerWallet = lockedWallets.get(winnerId);
        Wallet sellerWallet = lockedWallets.get(sellerId);

        winnerWallet.settleHeldBid(winningAmount);
        sellerWallet.credit(winningAmount);
        walletRepository.saveAndFlush(winnerWallet);
        walletRepository.saveAndFlush(sellerWallet);

        walletService.recordTransaction(
                winnerWallet,
                TransactionType.AUCTION_SETTLEMENT,
                winningAmount,
                "Tất toán thắng phiên đấu giá #" + auctionId
        );
        walletService.recordTransaction(
                sellerWallet,
                TransactionType.MARKET_SALE,
                winningAmount,
                "Nhận tiền bán vật phẩm từ phiên đấu giá #" + auctionId
        );

        // Chuyển quyền sở hữu sang người thắng & mở khóa
        if (isSealed && sealed != null) {
            sealed.setUserId(winnerId);
            sealed.setIsLocked(false);
            userSealedItemRepository.saveAndFlush(sealed);
        } else if (card != null) {
            card.setUserId(winnerId);
            card.setIsLocked(false);
            userCardRepository.saveAndFlush(card);
        }

        auction.setStatus(AuctionStatus.SETTLED);
        auctionRepository.saveAndFlush(auction);

        log.info("[AUCTION-SETTLEMENT] Completed auction #{}: winner={}, seller={}, amount={}, itemType={}",
                auctionId, winnerId, sellerId, winningAmount, auction.getItemType());
    }
}
