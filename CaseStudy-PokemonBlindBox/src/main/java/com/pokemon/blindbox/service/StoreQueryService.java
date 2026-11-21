package com.pokemon.blindbox.service;

import com.pokemon.blindbox.dto.response.*;
import com.pokemon.blindbox.entity.*;
import com.pokemon.blindbox.entity.enums.AuctionStatus;
import com.pokemon.blindbox.entity.enums.ListingStatus;
import com.pokemon.blindbox.exception.ResourceNotFoundException;
import com.pokemon.blindbox.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * StoreQueryService chuyên trách truy vấn dữ liệu đọc (@Transactional(readOnly = true))
 * cho tầng Web MVC, loại bỏ hoàn toàn việc WebViewController gọi trực tiếp Repository,
 * diệt 2N+1 Query (P1-01), giới hạn phân trang (P1-05) và tận dụng Redis Cache (P1-06).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreQueryService {

    private static final int MAX_USER_CARDS_IN_VIEW = 200;

    private final CatalogCacheService catalogCacheService;
    private final BoosterBoxRepository boosterBoxRepository;
    private final BoosterPackRepository boosterPackRepository;
    private final CardTemplateRepository cardTemplateRepository;
    private final UserCardRepository userCardRepository;
    private final MarketListingRepository marketListingRepository;
    private final AuctionRepository auctionRepository;
    private final AuctionBidRepository auctionBidRepository;
    private final UserRepository userRepository;

    public StorePageData loadStorePage(Long boxId) {
        List<BoosterBox> allBoxes = boosterBoxRepository.findAllByOrderByIdAsc();
        BoosterBox selectedBox = null;
        if (!allBoxes.isEmpty()) {
            selectedBox = (boxId != null)
                    ? allBoxes.stream().filter(b -> b.getId().equals(boxId)).findFirst().orElse(allBoxes.getFirst())
                    : allBoxes.getFirst();
        }

        List<BoosterPackResponseDTO> packs = List.of();
        BoosterBoxSummaryResponseDTO selectedBoxDto = null;
        if (selectedBox != null) {
            packs = boosterPackRepository.findByBoosterBoxIdOrderByPositionInBoxAsc(selectedBox.getId())
                    .stream()
                    .map(BoosterPackResponseDTO::fromEntity)
                    .toList();
            selectedBoxDto = BoosterBoxSummaryResponseDTO.fromEntity(selectedBox);
        }

        List<BoosterBoxSummaryResponseDTO> boxList = allBoxes.stream()
                .map(BoosterBoxSummaryResponseDTO::fromEntity)
                .toList();

        List<RarityResponseDTO> rarities = catalogCacheService.getAllRaritiesWithWeights();
        int totalDropWeight = rarities.stream()
                .mapToInt(r -> r.dropWeight() != null ? r.dropWeight() : 0)
                .sum();

        return new StorePageData(boxList, selectedBoxDto, packs, rarities, totalDropWeight);
    }

    public InventoryPageData loadInventoryPage(Long userId) {
        List<CardSetResponseDTO> cardSets = catalogCacheService.getAllCardSets();
        List<RarityResponseDTO> rarities = catalogCacheService.getAllRaritiesWithWeights();
        List<CardSpeciesResponseDTO> speciesList = catalogCacheService.getAllCardSpecies();

        List<UserCardResponseDTO> myCards = userCardRepository
                .findByUserIdOrderByAcquiredAtDesc(userId, PageRequest.of(0, MAX_USER_CARDS_IN_VIEW))
                .getContent()
                .stream()
                .map(uc -> UserCardResponseDTO.fromEntity(uc, CardTemplateResponseDTO.fromEntity(uc.getCardTemplate())))
                .toList();

        Map<Long, Long> templateCopyCounts = myCards.stream()
                .collect(Collectors.groupingBy(uc -> uc.card().id(), Collectors.counting()));

        long unlockedCardCount = myCards.stream().filter(uc -> !Boolean.TRUE.equals(uc.isLocked())).count();
        long listedCardCount = myCards.size() - unlockedCardCount;

        return new InventoryPageData(cardSets, rarities, speciesList, myCards, templateCopyCounts, unlockedCardCount, listedCardCount);
    }

    public CardDetailPageData loadCardDetailById(Long userId, Long cardId) {
        CardTemplate template = cardTemplateRepository.findWithDetailsById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thẻ bài với id = " + cardId));
        return buildCardDetailData(userId, template);
    }

    public CardDetailPageData loadCardDetailByNumber(Long userId, Long setId, Integer cardNumber) {
        CardTemplate template = cardTemplateRepository.findByCardSetIdAndCardNumber(setId, cardNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Không tìm thấy thẻ bài số #%d trong bộ setId = %d", cardNumber, setId)
                ));
        return buildCardDetailData(userId, template);
    }

    private CardDetailPageData buildCardDetailData(Long userId, CardTemplate template) {
        CardTemplateResponseDTO cardDto = CardTemplateResponseDTO.fromEntity(template);
        boolean isHoloRarity = Boolean.TRUE.equals(cardDto.rarityIsHolographic());

        // P1-05: Chỉ truy vấn các bản sao của đúng thẻ này thay vì load toàn bộ kho thẻ
        List<UserCard> matchingCopies = userCardRepository.findByUserIdAndCardTemplateIdOrderByIdAsc(userId, cardDto.id());
        long ownedCopiesCount = matchingCopies.size();
        Long firstUnlockedUserCardId = matchingCopies.stream()
                .filter(uc -> !Boolean.TRUE.equals(uc.getIsLocked()))
                .map(UserCard::getId)
                .findFirst()
                .orElse(null);

        List<CardTemplateResponseDTO> setCards = catalogCacheService.getCardTemplatesBySetId(cardDto.setId());

        CardTemplateResponseDTO prevCard = null;
        CardTemplateResponseDTO nextCard = null;
        for (int i = 0; i < setCards.size(); i++) {
            if (setCards.get(i).id().equals(cardDto.id())) {
                if (i > 0) prevCard = setCards.get(i - 1);
                if (i < setCards.size() - 1) nextCard = setCards.get(i + 1);
                break;
            }
        }

        List<RarityResponseDTO> rarities = catalogCacheService.getAllRaritiesWithWeights();
        int totalDropWeight = rarities.stream()
                .mapToInt(r -> r.dropWeight() != null ? r.dropWeight() : 0)
                .sum();
        int cardDropWeight = rarities.stream()
                .filter(r -> r.id() != null && r.id().equals(cardDto.rarityId()))
                .mapToInt(r -> r.dropWeight() != null ? r.dropWeight() : 0)
                .findFirst()
                .orElse(0);
        double dropRatePercent = (totalDropWeight > 0 && cardDropWeight > 0)
                ? (cardDropWeight * 100.0) / totalDropWeight
                : 0.0;

        return new CardDetailPageData(cardDto, isHoloRarity, ownedCopiesCount, firstUnlockedUserCardId, setCards, prevCard, nextCard, dropRatePercent);
    }

    public MarketPageData loadMarketPage(Long userId, String setName, String speciesName) {
        String cleanSetName = (setName != null && !setName.isBlank()) ? setName.trim() : null;
        String cleanSpeciesName = (speciesName != null && !speciesName.isBlank()) ? speciesName.trim() : null;

        List<MarketListingResponseDTO> listings;
        List<AuctionResponseDTO> auctions;
        String activeCrossFilter = null;

        if (cleanSpeciesName != null) {
            listings = marketListingRepository.findActiveBySpecies(ListingStatus.ACTIVE, null, cleanSpeciesName)
                    .stream().map(MarketListingResponseDTO::fromEntity).toList();
            auctions = auctionRepository.findActiveBySpecies(AuctionStatus.ACTIVE, null, cleanSpeciesName)
                    .stream().map(AuctionResponseDTO::fromEntity).toList();
            activeCrossFilter = "Loài: " + cleanSpeciesName;
        } else if (cleanSetName != null) {
            listings = marketListingRepository.findActiveByCardSet(ListingStatus.ACTIVE, null, null, cleanSetName)
                    .stream().map(MarketListingResponseDTO::fromEntity).toList();
            auctions = auctionRepository.findActiveByCardSet(AuctionStatus.ACTIVE, null, null, cleanSetName)
                    .stream().map(AuctionResponseDTO::fromEntity).toList();
            activeCrossFilter = "Bộ: " + cleanSetName;
        } else {
            listings = marketListingRepository.findActiveByCardSet(ListingStatus.ACTIVE, null, null, null)
                    .stream().map(MarketListingResponseDTO::fromEntity).toList();
            List<AuctionResponseDTO> activeList = auctionRepository.findActiveByCardSet(AuctionStatus.ACTIVE, null, null, null)
                    .stream().map(AuctionResponseDTO::fromEntity).toList();
            List<AuctionResponseDTO> endedList = auctionRepository
                    .findTop30ByStatusInOrderByEndTimeDesc(List.of(AuctionStatus.ENDED, AuctionStatus.SETTLED))
                    .stream().map(AuctionResponseDTO::fromEntity).toList();
            java.util.ArrayList<AuctionResponseDTO> merged = new java.util.ArrayList<>(activeList);
            merged.addAll(endedList);
            auctions = merged;
        }

        // Batch-load tên người bán & số lượt trả giá cho tất cả phiên đấu giá (tránh N+1)
        java.util.Set<Long> sellerIds = auctions.stream()
                .map(AuctionResponseDTO::sellerId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> sellerNameMap = sellerIds.isEmpty()
                ? Map.of()
                : userRepository.findAllById(sellerIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));

        List<Long> auctionIds = auctions.stream().map(AuctionResponseDTO::id).toList();
        Map<Long, Long> bidCountMap = new java.util.HashMap<>();
        if (!auctionIds.isEmpty()) {
            for (Object[] row : auctionBidRepository.countBidsByAuctionIds(auctionIds)) {
                bidCountMap.put((Long) row[0], (Long) row[1]);
            }
        }

        List<UserCardResponseDTO> myCards = userCardRepository
                .findByUserIdOrderByAcquiredAtDesc(userId, PageRequest.of(0, MAX_USER_CARDS_IN_VIEW))
                .getContent()
                .stream()
                .map(uc -> UserCardResponseDTO.fromEntity(uc, CardTemplateResponseDTO.fromEntity(uc.getCardTemplate())))
                .toList();

        List<CardSetResponseDTO> cardSets = catalogCacheService.getAllCardSets();
        List<RarityResponseDTO> rarities = catalogCacheService.getAllRaritiesWithWeights();
        List<CardSpeciesResponseDTO> speciesList = catalogCacheService.getAllCardSpecies();

        return new MarketPageData(listings, auctions, sellerNameMap, bidCountMap, myCards, cardSets, rarities, speciesList, activeCrossFilter, cleanSetName, cleanSpeciesName);
    }

    public AuctionDetailPageData loadAuctionDetailPage(Long auctionId) {
        Auction auction = auctionRepository.findWithDetailsById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên đấu giá với id = " + auctionId));

        AuctionResponseDTO auctionDto = AuctionResponseDTO.fromEntity(auction);
        List<AuctionBidSummaryDTO> bidHistory = auctionBidRepository
                .findTop50ByAuctionIdOrderByBidTimestampDesc(auctionId)
                .stream()
                .map(AuctionBidSummaryDTO::fromEntity)
                .toList();

        String currentLeaderMasked = bidHistory.isEmpty() ? "Chưa có lượt đặt giá" : bidHistory.getFirst().maskedBidderName();
        BigDecimal nextMinBid = auction.getHighestBidderId() == null
                ? auction.getStartingPrice()
                : auction.getCurrentHighestBid().add(auction.getMinBidIncrement());

        String sellerDisplayName = auction.getSellerId() != null
                ? userRepository.findById(auction.getSellerId())
                        .map(User::getUsername)
                        .orElse("Trainer #" + auction.getSellerId())
                : "Trainer";

        return new AuctionDetailPageData(auctionDto, bidHistory, currentLeaderMasked, nextMinBid, sellerDisplayName);
    }

    public AdminDashboardPageData loadAdminDashboardPage(Long setId) {
        List<CardSetResponseDTO> cardSets = catalogCacheService.getAllCardSets();
        List<RarityResponseDTO> rarities = catalogCacheService.getAllRaritiesWithWeights();
        List<CardSpeciesResponseDTO> speciesList = catalogCacheService.getAllCardSpecies();

        CardSetResponseDTO activeSet = null;
        if (!cardSets.isEmpty()) {
            activeSet = (setId != null)
                    ? cardSets.stream().filter(s -> s.id().equals(setId)).findFirst().orElse(cardSets.getFirst())
                    : cardSets.getFirst();
        }

        List<CardTemplateResponseDTO> cardTemplates = activeSet != null
                ? catalogCacheService.getCardTemplatesBySetId(activeSet.id())
                : List.of();

        List<BoosterBoxSummaryResponseDTO> boosterBoxes = activeSet != null
                ? boosterBoxRepository.findByCardSetIdOrderByIdDesc(activeSet.id())
                        .stream()
                        .map(BoosterBoxSummaryResponseDTO::fromEntity)
                        .toList()
                : List.of();

        long sealedAvailableBoxCount = boosterBoxes.stream()
                .filter(b -> b.status() == com.pokemon.blindbox.entity.enums.BoxStatus.AVAILABLE
                        && b.remainingPacks() != null
                        && b.remainingPacks().equals(b.totalPacks()))
                .count();

        int totalDropWeight = rarities.stream()
                .mapToInt(r -> r.dropWeight() != null ? r.dropWeight() : 0)
                .sum();

        return new AdminDashboardPageData(
                cardSets, activeSet, rarities, totalDropWeight, speciesList, cardTemplates, boosterBoxes, sealedAvailableBoxCount
        );
    }

    public record StorePageData(
            List<BoosterBoxSummaryResponseDTO> boxes,
            BoosterBoxSummaryResponseDTO selectedBox,
            List<BoosterPackResponseDTO> packs,
            List<RarityResponseDTO> rarities,
            int totalDropWeight
    ) {}

    public record InventoryPageData(
            List<CardSetResponseDTO> cardSets,
            List<RarityResponseDTO> rarities,
            List<CardSpeciesResponseDTO> speciesList,
            List<UserCardResponseDTO> myCards,
            Map<Long, Long> templateCopyCounts,
            long unlockedCardCount,
            long listedCardCount
    ) {}

    public record CardDetailPageData(
            CardTemplateResponseDTO card,
            boolean isHoloRarity,
            long ownedCopiesCount,
            Long firstUnlockedUserCardId,
            List<CardTemplateResponseDTO> setCards,
            CardTemplateResponseDTO prevCard,
            CardTemplateResponseDTO nextCard,
            double dropRatePercent
    ) {}

    public record MarketPageData(
            List<MarketListingResponseDTO> fixedPriceListings,
            List<AuctionResponseDTO> activeAuctions,
            Map<Long, String> sellerNameMap,
            Map<Long, Long> bidCountMap,
            List<UserCardResponseDTO> myCards,
            List<CardSetResponseDTO> cardSets,
            List<RarityResponseDTO> rarities,
            List<CardSpeciesResponseDTO> speciesList,
            String activeCrossFilter,
            String selectedSetName,
            String selectedSpeciesName
    ) {}

    public record AuctionDetailPageData(
            AuctionResponseDTO auction,
            List<AuctionBidSummaryDTO> bidHistory,
            String currentLeaderMasked,
            BigDecimal nextMinBid,
            String sellerDisplayName
    ) {}

    public record AdminDashboardPageData(
            List<CardSetResponseDTO> cardSets,
            CardSetResponseDTO activeSet,
            List<RarityResponseDTO> rarities,
            int totalDropWeight,
            List<CardSpeciesResponseDTO> speciesList,
            List<CardTemplateResponseDTO> cardTemplates,
            List<BoosterBoxSummaryResponseDTO> boosterBoxes,
            long sealedAvailableBoxCount
    ) {}
}
