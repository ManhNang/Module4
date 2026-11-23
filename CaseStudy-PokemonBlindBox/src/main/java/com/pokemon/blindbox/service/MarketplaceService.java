package com.pokemon.blindbox.service;

import com.pokemon.blindbox.dto.request.BuyMarketListingRequest;
import com.pokemon.blindbox.dto.request.CreateMarketListingRequest;
import com.pokemon.blindbox.dto.response.AuctionResponseDTO;
import com.pokemon.blindbox.dto.response.MarketListingResponseDTO;
import com.pokemon.blindbox.dto.response.MarketplaceCrossLookupResponseDTO;
import com.pokemon.blindbox.entity.MarketListing;
import com.pokemon.blindbox.entity.UserCard;
import com.pokemon.blindbox.entity.UserSealedItem;
import com.pokemon.blindbox.entity.Wallet;
import com.pokemon.blindbox.entity.enums.AuctionStatus;
import com.pokemon.blindbox.entity.enums.ExpeditionStatus;
import com.pokemon.blindbox.entity.enums.ListingStatus;
import com.pokemon.blindbox.entity.enums.MarketItemType;
import com.pokemon.blindbox.entity.enums.SealedItemType;
import com.pokemon.blindbox.entity.enums.TransactionType;
import com.pokemon.blindbox.exception.BusinessValidationException;
import com.pokemon.blindbox.exception.ResourceNotFoundException;
import com.pokemon.blindbox.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    private static final Pageable DEFAULT_MARKET_PAGE = PageRequest.of(0, 60);

    private final MarketListingRepository marketListingRepository;
    private final AuctionRepository auctionRepository;
    private final UserCardRepository userCardRepository;
    private final UserSealedItemRepository userSealedItemRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final SanctuarySlotRepository sanctuarySlotRepository;
    private final ExpeditionCardRepository expeditionCardRepository;

    /**
     * 1. Treo Thẻ bài / Pack lẻ / Box nguyên seal lên Chợ bán giá cố định (MarketListing).
     * Dùng Pessimistic Write Lock + @Version để chống bán đúp (P0-02).
     */
    @Transactional
    public MarketListingResponseDTO createFixedPriceListing(CreateMarketListingRequest request) {
        MarketItemType itemType = request.itemType() != null ? request.itemType() : MarketItemType.CARD;

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

            MarketListing listing = MarketListing.builder()
                    .sellerId(request.sellerId())
                    .itemType(itemType)
                    .sealedItem(sealed)
                    .price(request.price())
                    .status(ListingStatus.ACTIVE)
                    .build();

            MarketListing saved = marketListingRepository.saveAndFlush(listing);
            log.info("Created P2P MarketListing id={} for {} sealedItemId={}, sellerId={}, price={}",
                    saved.getId(), itemType, sealed.getId(), request.sellerId(), request.price());
            return MarketListingResponseDTO.fromEntity(saved);
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
            throw new BusinessValidationException("Thẻ bài này đang được treo trên Sàn hoặc Phiên Đấu Giá khác!");
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

        MarketListing listing = MarketListing.builder()
                .sellerId(request.sellerId())
                .itemType(MarketItemType.CARD)
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
     * 2. Mua đứt Thẻ bài / Pack / Box đang rao bán giá cố định trên Chợ.
     */
    @Transactional
    public MarketListingResponseDTO buyFixedPriceListing(Long listingId, BuyMarketListingRequest request) {
        MarketListing listing = marketListingRepository.findWithDetailsById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin rao bán với listingId = " + listingId));

        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new BusinessValidationException("Vật phẩm này đã được người chơi khác mua hoặc đã gỡ khỏi Chợ!");
        }
        if (listing.getSellerId().equals(request.buyerId())) {
            throw new BusinessValidationException("Người bán không thể tự mua lại vật phẩm của chính mình!");
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

        MarketItemType itemType = listing.getItemType() != null ? listing.getItemType() : MarketItemType.CARD;
        String itemLabel = itemType == MarketItemType.BOX ? "Box nguyên seal" : (itemType == MarketItemType.PACK ? "Pack lẻ" : "thẻ bài");

        walletService.recordTransaction(buyerWallet, TransactionType.MARKET_PURCHASE, price, "Mua " + itemLabel + " trên Chợ #" + listingId);
        walletService.recordTransaction(sellerWallet, TransactionType.MARKET_SALE, price, "Bán " + itemLabel + " trên Chợ #" + listingId);

        if (itemType == MarketItemType.PACK || itemType == MarketItemType.BOX) {
            UserSealedItem sealed = listing.getSealedItem();
            sealed.setUserId(request.buyerId());
            sealed.setIsLocked(false);
            userSealedItemRepository.saveAndFlush(sealed);
        } else {
            UserCard userCard = listing.getUserCard();
            userCard.setUserId(request.buyerId());
            userCard.setIsLocked(false);
            userCardRepository.saveAndFlush(userCard);
        }

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
