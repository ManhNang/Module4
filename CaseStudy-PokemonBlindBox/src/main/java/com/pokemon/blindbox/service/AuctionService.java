package com.pokemon.blindbox.service;

import com.pokemon.blindbox.dto.event.AuctionBidUpdateEventDTO;
import com.pokemon.blindbox.dto.request.CreateAuctionRequest;
import com.pokemon.blindbox.dto.request.PlaceAuctionBidRequest;
import com.pokemon.blindbox.dto.response.AuctionBidResponseDTO;
import com.pokemon.blindbox.dto.response.AuctionResponseDTO;
import com.pokemon.blindbox.entity.*;
import com.pokemon.blindbox.entity.enums.AuctionStatus;
import com.pokemon.blindbox.entity.enums.ExpeditionStatus;
import com.pokemon.blindbox.entity.enums.MarketItemType;
import com.pokemon.blindbox.entity.enums.SealedItemType;
import com.pokemon.blindbox.entity.enums.TransactionType;
import com.pokemon.blindbox.exception.BusinessValidationException;
import com.pokemon.blindbox.exception.ResourceNotFoundException;
import com.pokemon.blindbox.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * AuctionService xử lý nghiệp vụ Đấu Giá Thời Gian Thực (Auction & AuctionBid):
 * 1. Treo thẻ từ UserCard lên Phiên Đấu Giá (Pessimistic Write Lock + @Version chống bán đúp, P0-02).
 * 2. Đặt bước giá mới (placeBid):
 *    - Khóa 2 ví theo thứ tự userId tăng dần chống InnoDB Deadlock (P1-03).
 *    - Dùng Optimistic Locking (@Version) trên Auction chống đè giá đồng thời.
 *    - Hoàn trả tiền tạm giữ (lockedBalance -> balance) cho người dẫn đầu trước đó.
 *    - Tạm giữ tiền (balance -> lockedBalance) trong Wallet của người đặt giá mới.
 *    - Tự động gia hạn chống bắn tỉa (Anti-sniping: cộng thêm 60 giây nếu đặt giá trong 30 giây cuối).
 *    - Phát sự kiện AuctionBidUpdateEventDTO để WebSocketBroadcastListener bắn tới
 *      /topic/auctions/{auctionId} sau khi Transaction COMMIT thành công.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionService {

    private static final long ANTI_SNIPE_THRESHOLD_SECONDS = 30L;
    private static final long ANTI_SNIPE_EXTENSION_SECONDS = 60L;

    private final AuctionRepository auctionRepository;
    private final AuctionBidRepository auctionBidRepository;
    private final UserCardRepository userCardRepository;
    private final UserSealedItemRepository userSealedItemRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final ApplicationEventPublisher eventPublisher;
    private final SanctuarySlotRepository sanctuarySlotRepository;
    private final ExpeditionCardRepository expeditionCardRepository;

    /**
     * 1. Tạo phiên đấu giá mới từ Thẻ bài / Pack lẻ / Box nguyên seal của người chơi.
     */
    @Transactional
    public AuctionResponseDTO createAuction(CreateAuctionRequest request) {
        MarketItemType itemType = request.itemType() != null ? request.itemType() : MarketItemType.CARD;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = now.plusMinutes(request.durationMinutes());

        if (itemType == MarketItemType.PACK || itemType == MarketItemType.BOX) {
            if (request.sealedItemId() == null) {
                throw new BusinessValidationException("Vui lòng chọn Pack hoặc Box hợp lệ từ Kho đồ!");
            }
            UserSealedItem sealed = userSealedItemRepository.findByIdForUpdate(request.sealedItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Pack/Box với id = " + request.sealedItemId()));

            if (!sealed.getUserId().equals(request.sellerId())) {
                throw new BusinessValidationException("Bạn không sở hữu Pack/Box này!");
            }
            if (Boolean.TRUE.equals(sealed.getIsConsumed()) || Boolean.TRUE.equals(sealed.getIsLocked())) {
                throw new BusinessValidationException("Pack/Box này đã được mở hoặc đang treo trên Chợ!");
            }
            SealedItemType expected = itemType == MarketItemType.BOX ? SealedItemType.BOX : SealedItemType.PACK;
            if (sealed.getItemType() != expected) {
                throw new BusinessValidationException("Loại vật phẩm không khớp với Pack/Box đã chọn!");
            }

            sealed.setIsLocked(true);
            userSealedItemRepository.saveAndFlush(sealed);

            Auction auction = Auction.builder()
                    .sellerId(request.sellerId())
                    .itemType(itemType)
                    .sealedItem(sealed)
                    .startingPrice(request.startingPrice())
                    .minBidIncrement(request.minBidIncrement())
                    .currentHighestBid(request.startingPrice())
                    .highestBidderId(null)
                    .startTime(now)
                    .endTime(endTime)
                    .status(AuctionStatus.ACTIVE)
                    .build();

            Auction saved = auctionRepository.saveAndFlush(auction);
            log.info("Created Auction id={} for {} sealedItemId={}, startingPrice={}",
                    saved.getId(), itemType, sealed.getId(), request.startingPrice());
            return AuctionResponseDTO.fromEntity(saved);
        }

        if (request.userCardId() == null) {
            throw new BusinessValidationException("userCardId không được để trống!");
        }
        UserCard userCard = userCardRepository.findByIdForUpdate(request.userCardId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thẻ bài sở hữu với userCardId = " + request.userCardId()));

        if (!userCard.getUserId().equals(request.sellerId())) {
            throw new BusinessValidationException("Bạn không phải là chủ sở hữu của thẻ bài này!");
        }
        if (Boolean.TRUE.equals(userCard.getIsLocked())) {
            throw new BusinessValidationException("Thẻ bài này đang bị khóa trên Sàn hoặc Phiên Đấu Giá khác!");
        }
        if (expeditionCardRepository != null
                && expeditionCardRepository.existsByUserCardIdAndExpeditionStatus(userCard.getId(), ExpeditionStatus.IN_PROGRESS)) {
            throw new BusinessValidationException("Thẻ bài này đang tham gia chuyến thám hiểm trên Đảo Pokémon!");
        }
        if (sanctuarySlotRepository != null) {
            sanctuarySlotRepository.deleteByUserCardId(userCard.getId());
        }

        userCard.setIsLocked(true);
        userCardRepository.saveAndFlush(userCard);

        Auction auction = Auction.builder()
                .sellerId(request.sellerId())
                .itemType(MarketItemType.CARD)
                .userCard(userCard)
                .startingPrice(request.startingPrice())
                .minBidIncrement(request.minBidIncrement())
                .currentHighestBid(request.startingPrice())
                .highestBidderId(null)
                .startTime(now)
                .endTime(endTime)
                .status(AuctionStatus.ACTIVE)
                .build();

        Auction saved = auctionRepository.saveAndFlush(auction);
        log.info("Created Auction id={} for userCardId={}, startingPrice={}, endTime={}",
                saved.getId(), userCard.getId(), request.startingPrice(), endTime);

        return AuctionResponseDTO.fromEntity(saved);
    }

    /**
     * 2. Đặt bước giá mới trên phiên đấu giá (có @Version chống đè giá, tạm giữ tiền trong Wallet
     * và bắn sự kiện WebSocket tới /topic/auctions/{auctionId} sau khi DB commit).
     */
    @Transactional
    public AuctionBidResponseDTO placeBid(Long auctionId, PlaceAuctionBidRequest request) {
        // GAP-02: Khóa Auction (PESSIMISTIC_WRITE) TRƯỚC khi khóa Wallet để đồng nhất thứ tự khóa với AuctionSettlementService
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên đấu giá với auctionId = " + auctionId));

        LocalDateTime now = LocalDateTime.now();
        if (auction.getStatus() != AuctionStatus.ACTIVE || !now.isBefore(auction.getEndTime())) {
            throw new BusinessValidationException("Phiên đấu giá đã kết thúc hoặc không còn hoạt động!");
        }
        if (auction.getSellerId().equals(request.bidderId())) {
            throw new BusinessValidationException("Người bán không được phép tự đặt giá trên phiên đấu giá của mình!");
        }
        if (request.bidderId().equals(auction.getHighestBidderId())) {
            throw new BusinessValidationException("Bạn đang là người dẫn đầu phiên đấu giá này!");
        }

        BigDecimal minimumAllowedBid = auction.getHighestBidderId() == null
                ? auction.getStartingPrice()
                : auction.getCurrentHighestBid().add(auction.getMinBidIncrement());

        if (request.bidAmount().compareTo(minimumAllowedBid) < 0) {
            throw new BusinessValidationException(String.format(
                    "Mức giá đặt chưa đủ! Mức giá tối thiểu cần đặt là %s Xu.",
                    minimumAllowedBid.stripTrailingZeros().toPlainString()
            ));
        }

        // 1 & 2. Khóa ví theo thứ tự userId tăng dần (P1-03 chống Deadlock), hoàn cọc người cũ & tạm giữ người mới
        Long previousLeaderId = auction.getHighestBidderId();
        BigDecimal previousHighestBid = auction.getCurrentHighestBid();
        Wallet newBidderWallet;

        if (previousLeaderId != null) {
            Map<Long, Wallet> lockedWallets = walletService.lockTwoWalletsInDeterministicOrder(
                    previousLeaderId, request.bidderId()
            );
            Wallet previousLeaderWallet = lockedWallets.get(previousLeaderId);
            newBidderWallet = lockedWallets.get(request.bidderId());

            previousLeaderWallet.refundHeldBid(previousHighestBid);
            walletRepository.saveAndFlush(previousLeaderWallet);

            walletService.recordTransaction(
                    previousLeaderWallet,
                    TransactionType.AUCTION_BID_REFUND,
                    previousHighestBid,
                    "Hoàn trả tiền tạm giữ do có người trả giá cao hơn tại phiên đấu giá #" + auctionId
            );
        } else {
            newBidderWallet = walletRepository.findByUserIdForUpdate(request.bidderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví của người đặt giá với userId = " + request.bidderId()));
        }

        newBidderWallet.holdForBid(request.bidAmount());
        walletRepository.saveAndFlush(newBidderWallet);

        walletService.recordTransaction(
                newBidderWallet,
                TransactionType.AUCTION_BID_HOLD,
                request.bidAmount(),
                "Tạm giữ tiền đặt giá phiên đấu giá #" + auctionId
        );

        // 3. Anti-sniping: Nếu đặt giá trong 30 giây cuối, tự động cộng thêm 60 giây vào endTime
        long secondsRemaining = Duration.between(now, auction.getEndTime()).getSeconds();
        if (secondsRemaining <= ANTI_SNIPE_THRESHOLD_SECONDS) {
            auction.setEndTime(auction.getEndTime().plusSeconds(ANTI_SNIPE_EXTENSION_SECONDS));
        }

        // 4. Cập nhật giá cao nhất trên Auction (saveAndFlush để kiểm tra @Version ngay lập tức)
        auction.setCurrentHighestBid(request.bidAmount());
        auction.setHighestBidderId(request.bidderId());
        auctionRepository.saveAndFlush(auction);

        String maskedBidderName = maskBidderName(request.bidderDisplayName(), request.bidderId());
        AuctionBid savedBid = auctionBidRepository.save(AuctionBid.builder()
                .auction(auction)
                .bidderId(request.bidderId())
                .maskedBidderName(maskedBidderName)
                .bidAmount(request.bidAmount())
                .bidTimestamp(now)
                .build());

        // 5. Phát sự kiện để WebSocketBroadcastListener bắn tới /topic/auctions/{auctionId} sau khi DB COMMIT
        eventPublisher.publishEvent(AuctionBidUpdateEventDTO.builder()
                .auctionId(auction.getId())
                .newHighestBid(auction.getCurrentHighestBid())
                .maskedBidderName(maskedBidderName)
                .bidTimestamp(now)
                .endTime(auction.getEndTime())
                .build());

        log.info("New highest bid placed on auctionId={}: amount={}, bidder={}, endTime={}",
                auctionId, request.bidAmount(), maskedBidderName, auction.getEndTime());

        return AuctionBidResponseDTO.builder()
                .bidId(savedBid.getId())
                .auctionId(auction.getId())
                .bidderId(request.bidderId())
                .maskedBidderName(maskedBidderName)
                .bidAmount(request.bidAmount())
                .bidTimestamp(now)
                .auctionEndTime(auction.getEndTime())
                .build();
    }

    public static String maskBidderName(String displayName, Long bidderId) {
        if (displayName != null && displayName.trim().length() >= 3) {
            String trimmed = displayName.trim();
            return trimmed.substring(0, 3) + "***" + trimmed.charAt(trimmed.length() - 1);
        }
        return "Trainer***" + (bidderId != null ? bidderId % 100 : 0);
    }
}
