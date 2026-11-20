package com.pokemon.blindbox.service;

import com.pokemon.blindbox.dto.request.BuyMarketListingRequest;
import com.pokemon.blindbox.dto.request.CreateMarketListingRequest;
import com.pokemon.blindbox.dto.response.AuctionResponseDTO;
import com.pokemon.blindbox.dto.response.MarketListingResponseDTO;
import com.pokemon.blindbox.dto.response.MarketplaceCrossLookupResponseDTO;
import com.pokemon.blindbox.entity.MarketListing;
import com.pokemon.blindbox.entity.UserCard;
import com.pokemon.blindbox.entity.Wallet;
import com.pokemon.blindbox.entity.enums.AuctionStatus;
import com.pokemon.blindbox.entity.enums.ListingStatus;
import com.pokemon.blindbox.entity.enums.TransactionType;
import com.pokemon.blindbox.exception.BusinessValidationException;
import com.pokemon.blindbox.exception.ResourceNotFoundException;
import com.pokemon.blindbox.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * MarketplaceService xử lý nghiệp vụ Sàn Giao Dịch P2P Giá Cố Định (MarketListing)
 * và API Tra cứu chéo theo Bộ sưu tập (Đặt: "Delta Reign") hoặc Loài ("Hầm hào huyền thoại").
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketplaceService {

    private final MarketListingRepository marketListingRepository;
    private final AuctionRepository auctionRepository;
    private final UserCardRepository userCardRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;

    /**
     * 1. Treo thẻ bài từ UserCard lên Sàn bán giá cố định (MarketListing).
     * Dùng Pessimistic Write Lock + @Version trên UserCard để chống bán đúp thẻ (P0-02).
     */
    @Transactional
    public MarketListingResponseDTO createFixedPriceListing(CreateMarketListingRequest request) {
        UserCard userCard = userCardRepository.findByIdForUpdate(request.userCardId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thẻ bài sở hữu với userCardId = " + request.userCardId()));

        if (!userCard.getUserId().equals(request.sellerId())) {
            throw new BusinessValidationException("Bạn không phải là chủ sở hữu của thẻ bài này!");
        }
        if (Boolean.TRUE.equals(userCard.getIsLocked())) {
            throw new BusinessValidationException("Thẻ bài này đang được treo trên Sàn hoặc Phiên Đấu Giá khác!");
        }

        userCard.setIsLocked(true);
        userCardRepository.saveAndFlush(userCard);

        MarketListing listing = MarketListing.builder()
                .sellerId(request.sellerId())
                .userCard(userCard)
                .price(request.price())
                .status(ListingStatus.ACTIVE)
                .build();

        MarketListing saved = marketListingRepository.saveAndFlush(listing);
        log.info("Created P2P MarketListing id={} for userCardId={}, sellerId={}, price={}",
                saved.getId(), userCard.getId(), request.sellerId(), request.price());

        return MarketListingResponseDTO.fromEntity(saved);
    }

    /**
     * 2. Mua đứt thẻ bài đang rao bán giá cố định trên Sàn P2P:
     * - Khóa 2 ví theo thứ tự userId tăng dần chống InnoDB Deadlock (P1-03).
     * - Ghi đúng TransactionType.MARKET_PURCHASE (người mua) và MARKET_SALE (người bán) (P1-09).
     */
    @Transactional
    public MarketListingResponseDTO buyFixedPriceListing(Long listingId, BuyMarketListingRequest request) {
        MarketListing listing = marketListingRepository.findWithDetailsById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin rao bán với listingId = " + listingId));

        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new BusinessValidationException("Thẻ bài này đã được người chơi khác mua hoặc đã gỡ khỏi sàn!");
        }
        if (listing.getSellerId().equals(request.buyerId())) {
            throw new BusinessValidationException("Người bán không thể tự mua lại thẻ bài của chính mình!");
        }

        Map<Long, Wallet> lockedWallets = walletService.lockTwoWalletsInDeterministicOrder(
                request.buyerId(), listing.getSellerId()
        );
        Wallet buyerWallet = lockedWallets.get(request.buyerId());
        Wallet sellerWallet = lockedWallets.get(listing.getSellerId());

        BigDecimal price = listing.getPrice();
        buyerWallet.debit(price);
        sellerWallet.credit(price);
        walletRepository.saveAndFlush(buyerWallet);
        walletRepository.saveAndFlush(sellerWallet);

        walletService.recordTransaction(buyerWallet, TransactionType.MARKET_PURCHASE, price, "Mua thẻ P2P #" + listingId);
        walletService.recordTransaction(sellerWallet, TransactionType.MARKET_SALE, price, "Bán thẻ P2P #" + listingId);

        UserCard userCard = listing.getUserCard();
        userCard.setUserId(request.buyerId());
        userCard.setIsLocked(false);
        userCardRepository.saveAndFlush(userCard);

        listing.setStatus(ListingStatus.SOLD);
        listing.setBuyerId(request.buyerId());
        listing.setSoldAt(LocalDateTime.now());
        MarketListing saved = marketListingRepository.saveAndFlush(listing);

        return MarketListingResponseDTO.fromEntity(saved);
    }

    /**
     * 3. API Tra cứu chéo khi người dùng bấm vào liên kết Đặt ("Delta Reign" / setId / setCode):
     * Trả về toàn bộ các thẻ cùng Bộ đang được rao bán giá cố định hoặc đang Đấu Giá trên Sàn.
     */
    @Transactional(readOnly = true)
    public MarketplaceCrossLookupResponseDTO lookupMarketByCardSet(
            Long setId,
            String setCode,
            String setName
    ) {
        String normalizedCode = normalizeNullable(setCode);
        String normalizedName = normalizeNullable(setName);

        List<MarketListingResponseDTO> listings = marketListingRepository
                .findActiveByCardSet(ListingStatus.ACTIVE, setId, normalizedCode, normalizedName)
                .stream()
                .map(MarketListingResponseDTO::fromEntity)
                .toList();

        List<AuctionResponseDTO> auctions = auctionRepository
                .findActiveByCardSet(AuctionStatus.ACTIVE, setId, normalizedCode, normalizedName)
                .stream()
                .map(AuctionResponseDTO::fromEntity)
                .toList();

        String filterLabel = normalizedName != null ? normalizedName
                : (normalizedCode != null ? normalizedCode : String.valueOf(setId));

        return MarketplaceCrossLookupResponseDTO.builder()
                .filterType("CARD_SET")
                .filterValue(filterLabel)
                .totalActiveItems(listings.size() + auctions.size())
                .fixedPriceListings(listings)
                .activeAuctions(auctions)
                .build();
    }

    /**
     * 4. API Tra cứu chéo khi người dùng bấm vào liên kết Loài ("Hầm hào huyền thoại" / speciesId):
     * Trả về toàn bộ các thẻ cùng Loài đang được rao bán giá cố định hoặc đang Đấu Giá trên Sàn.
     */
    @Transactional(readOnly = true)
    public MarketplaceCrossLookupResponseDTO lookupMarketBySpecies(
            Long speciesId,
            String speciesName
    ) {
        String normalizedSpeciesName = normalizeNullable(speciesName);

        List<MarketListingResponseDTO> listings = marketListingRepository
                .findActiveBySpecies(ListingStatus.ACTIVE, speciesId, normalizedSpeciesName)
                .stream()
                .map(MarketListingResponseDTO::fromEntity)
                .toList();

        List<AuctionResponseDTO> auctions = auctionRepository
                .findActiveBySpecies(AuctionStatus.ACTIVE, speciesId, normalizedSpeciesName)
                .stream()
                .map(AuctionResponseDTO::fromEntity)
                .toList();

        String filterLabel = normalizedSpeciesName != null ? normalizedSpeciesName : String.valueOf(speciesId);

        return MarketplaceCrossLookupResponseDTO.builder()
                .filterType("CARD_SPECIES")
                .filterValue(filterLabel)
                .totalActiveItems(listings.size() + auctions.size())
                .fixedPriceListings(listings)
                .activeAuctions(auctions)
                .build();
    }

    private String normalizeNullable(String input) {
        return (input != null && !input.trim().isEmpty()) ? input.trim() : null;
    }
}
