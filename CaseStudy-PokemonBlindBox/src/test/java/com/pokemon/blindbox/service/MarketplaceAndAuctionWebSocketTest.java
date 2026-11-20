package com.pokemon.blindbox.service;

import com.pokemon.blindbox.dto.event.AuctionBidUpdateEventDTO;
import com.pokemon.blindbox.dto.event.BoxStateUpdateEventDTO;
import com.pokemon.blindbox.dto.request.CreateMarketListingRequest;
import com.pokemon.blindbox.dto.request.PlaceAuctionBidRequest;
import com.pokemon.blindbox.dto.response.AuctionBidResponseDTO;
import com.pokemon.blindbox.dto.response.MarketListingResponseDTO;
import com.pokemon.blindbox.dto.response.MarketplaceCrossLookupResponseDTO;
import com.pokemon.blindbox.entity.*;
import com.pokemon.blindbox.entity.enums.AuctionStatus;
import com.pokemon.blindbox.entity.enums.ListingStatus;
import com.pokemon.blindbox.event.WebSocketBroadcastListener;
import com.pokemon.blindbox.exception.BusinessValidationException;
import com.pokemon.blindbox.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketplaceAndAuctionWebSocketTest {

    @Mock
    private MarketListingRepository marketListingRepository;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private AuctionBidRepository auctionBidRepository;

    @Mock
    private UserCardRepository userCardRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @InjectMocks
    private MarketplaceService marketplaceService;

    @InjectMocks
    private AuctionService auctionService;

    private UserCard userCard98;

    @BeforeEach
    void setUp() {
        CardSet deltaReign = CardSet.builder()
                .id(1L)
                .name("Delta Reign")
                .code("DLR")
                .baseSetTotal(103)
                .cardsPerPack(5)
                .packsPerBox(30)
                .build();

        Rarity uncommon = Rarity.builder()
                .id(2L)
                .name("Không phổ biến")
                .dropWeight(280)
                .colorHex("#10B981")
                .build();

        CardSpecies trenchSpecies = CardSpecies.builder()
                .id(1L)
                .name("Hầm hào huyền thoại")
                .isBeta(true)
                .build();

        CardTemplate template98 = CardTemplate.builder()
                .id(98L)
                .name("Hầm hào huyền thoại")
                .cardNumber(98)
                .imageUrl("https://assets.pokemon-blindbox.vn/cards/dlr/098_ham_hao_huyen_thoai.webp")
                .cardSet(deltaReign)
                .rarity(uncommon)
                .cardSpecies(trenchSpecies)
                .build();

        userCard98 = UserCard.builder()
                .id(500L)
                .userId(10L)
                .cardTemplate(template98)
                .isLocked(false)
                .build();
    }

    @Test
    @DisplayName("1. Treo thẻ UserCard lên Sàn P2P giá cố định: Khóa isLocked = true và chặn treo trùng")
    void shouldLockUserCardWhenCreatingListingAndPreventDuplicateListing() {
        when(userCardRepository.findByIdForUpdate(500L)).thenReturn(Optional.of(userCard98));
        when(marketListingRepository.saveAndFlush(any(MarketListing.class)))
                .thenAnswer(inv -> {
                    MarketListing ml = inv.getArgument(0);
                    ml.setId(77L);
                    return ml;
                });

        CreateMarketListingRequest request = CreateMarketListingRequest.builder()
                .sellerId(10L)
                .userCardId(500L)
                .price(new BigDecimal("150000.00"))
                .build();

        MarketListingResponseDTO response = marketplaceService.createFixedPriceListing(request);

        assertThat(response.id()).isEqualTo(77L);
        assertThat(response.card().formattedCardNumber()).isEqualTo("098 / 103");
        assertThat(userCard98.getIsLocked()).isTrue();

        // Thử treo lại chính thẻ đã khóa -> ném lỗi BusinessValidationException
        assertThatThrownBy(() -> marketplaceService.createFixedPriceListing(request))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("đang được treo");
    }

    @Test
    @DisplayName("2. API Tra cứu chéo theo Đặt ('Delta Reign') và Loài ('Hầm hào huyền thoại') trên Sàn")
    void shouldCrossLookupActiveListingsAndAuctionsBySetAndSpecies() {
        MarketListing activeListing = MarketListing.builder()
                .id(1L)
                .sellerId(10L)
                .userCard(userCard98)
                .price(new BigDecimal("120000.00"))
                .status(ListingStatus.ACTIVE)
                .build();

        Auction activeAuction = Auction.builder()
                .id(2L)
                .sellerId(10L)
                .userCard(userCard98)
                .startingPrice(new BigDecimal("80000.00"))
                .minBidIncrement(new BigDecimal("10000.00"))
                .currentHighestBid(new BigDecimal("90000.00"))
                .status(AuctionStatus.ACTIVE)
                .startTime(LocalDateTime.now().minusMinutes(10))
                .endTime(LocalDateTime.now().plusMinutes(20))
                .build();

        when(marketListingRepository.findActiveBySpecies(ListingStatus.ACTIVE, null, "Hầm hào huyền thoại"))
                .thenReturn(List.of(activeListing));
        when(auctionRepository.findActiveBySpecies(AuctionStatus.ACTIVE, null, "Hầm hào huyền thoại"))
                .thenReturn(List.of(activeAuction));

        MarketplaceCrossLookupResponseDTO speciesLookup =
                marketplaceService.lookupMarketBySpecies(null, "Hầm hào huyền thoại");

        assertThat(speciesLookup.filterType()).isEqualTo("CARD_SPECIES");
        assertThat(speciesLookup.filterValue()).isEqualTo("Hầm hào huyền thoại");
        assertThat(speciesLookup.totalActiveItems()).isEqualTo(2);
        assertThat(speciesLookup.fixedPriceListings()).hasSize(1);
        assertThat(speciesLookup.activeAuctions()).hasSize(1);
    }

    @Test
    @DisplayName("3. Đặt giá Đấu Giá (placeBid): Hoàn cọc người cũ, tạm giữ tiền người mới, cập nhật @Version và bắn WebSocket /topic/auctions/{auctionId}")
    void shouldRefundPreviousBidderAndHoldNewBidderFundsAndBroadcastWebSocket() {
        Auction auction = Auction.builder()
                .id(88L)
                .sellerId(10L)
                .userCard(userCard98)
                .startingPrice(new BigDecimal("100000.00"))
                .minBidIncrement(new BigDecimal("10000.00"))
                .currentHighestBid(new BigDecimal("120000.00"))
                .highestBidderId(20L) // Người dẫn đầu cũ
                .startTime(LocalDateTime.now().minusMinutes(5))
                .endTime(LocalDateTime.now().plusMinutes(10))
                .status(AuctionStatus.ACTIVE)
                .version(1L)
                .build();

        Wallet previousLeaderWallet = Wallet.builder()
                .id(2L)
                .userId(20L)
                .balance(new BigDecimal("30000.00"))
                .lockedBalance(new BigDecimal("120000.00")) // Đang bị giữ 120k
                .version(1L)
                .build();

        Wallet newBidderWallet = Wallet.builder()
                .id(3L)
                .userId(30L)
                .balance(new BigDecimal("200000.00"))
                .lockedBalance(BigDecimal.ZERO)
                .version(1L)
                .build();

        when(auctionRepository.findByIdForUpdate(88L)).thenReturn(Optional.of(auction));
        when(walletService.lockTwoWalletsInDeterministicOrder(20L, 30L))
                .thenReturn(Map.of(20L, previousLeaderWallet, 30L, newBidderWallet));
        when(auctionBidRepository.save(any(AuctionBid.class))).thenAnswer(inv -> {
            AuctionBid bid = inv.getArgument(0);
            bid.setId(999L);
            return bid;
        });

        PlaceAuctionBidRequest bidRequest = PlaceAuctionBidRequest.builder()
                .bidderId(30L)
                .bidderDisplayName("Satoshi99")
                .bidAmount(new BigDecimal("150000.00"))
                .build();

        AuctionBidResponseDTO bidResponse = auctionService.placeBid(88L, bidRequest);

        // 1. Ví người dẫn đầu cũ được hoàn lại 120,000 vào balance, lockedBalance về 0
        assertThat(previousLeaderWallet.getBalance()).isEqualByComparingTo(new BigDecimal("150000.00"));
        assertThat(previousLeaderWallet.getLockedBalance()).isEqualByComparingTo(BigDecimal.ZERO);

        // 2. Ví người đặt giá mới bị trừ 150,000 từ balance sang lockedBalance
        assertThat(newBidderWallet.getBalance()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(newBidderWallet.getLockedBalance()).isEqualByComparingTo(new BigDecimal("150000.00"));

        // 3. Auction cập nhật currentHighestBid = 150,000 và highestBidderId = 30
        assertThat(auction.getCurrentHighestBid()).isEqualByComparingTo(new BigDecimal("150000.00"));
        assertThat(auction.getHighestBidderId()).isEqualTo(30L);
        assertThat(bidResponse.maskedBidderName()).isEqualTo("Sat***9");

        // 4. Kiểm chứng sự kiện WebSocket gửi tới /topic/auctions/88 và /topic/boxes/10
        ArgumentCaptor<AuctionBidUpdateEventDTO> eventCaptor = ArgumentCaptor.forClass(AuctionBidUpdateEventDTO.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        WebSocketBroadcastListener wsListener = new WebSocketBroadcastListener(simpMessagingTemplate);
        wsListener.onAuctionBidUpdated(eventCaptor.getValue());
        verify(simpMessagingTemplate).convertAndSend(eq("/topic/auctions/88"), eq(eventCaptor.getValue()));

        BoxStateUpdateEventDTO boxEvent = BoxStateUpdateEventDTO.builder()
                .boxId(10L)
                .soldPackPosition(1)
                .remainingPacks(29)
                .canBuyWholeBox(false)
                .build();
        wsListener.onBoxStateUpdated(boxEvent);
        verify(simpMessagingTemplate).convertAndSend(eq("/topic/boxes/10"), eq(boxEvent));
    }
}
