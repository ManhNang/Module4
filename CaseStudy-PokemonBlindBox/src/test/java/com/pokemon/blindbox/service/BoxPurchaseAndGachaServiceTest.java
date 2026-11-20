package com.pokemon.blindbox.service;

import com.pokemon.blindbox.dto.response.*;
import com.pokemon.blindbox.entity.*;
import com.pokemon.blindbox.entity.enums.BoxStatus;
import com.pokemon.blindbox.entity.enums.PackStatus;
import com.pokemon.blindbox.exception.BusinessValidationException;
import com.pokemon.blindbox.exception.GlobalExceptionHandler;
import com.pokemon.blindbox.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoxPurchaseAndGachaServiceTest {

    @Mock
    private BoosterBoxRepository boosterBoxRepository;

    @Mock
    private BoosterPackRepository boosterPackRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private CardTemplateRepository cardTemplateRepository;

    @Mock
    private UserCardRepository userCardRepository;

    @Mock
    private CatalogCacheService catalogCacheService;

    @InjectMocks
    private BoxPurchaseService boxPurchaseService;

    @InjectMocks
    private GachaEngineService gachaEngineService;

    @Test
    @DisplayName("1. Mua Pack lẻ: Trừ ví, chuyển Pack sang SOLD và lập tức khóa canBuyWholeBox = false")
    void shouldDeductWalletAndDisableCanBuyWholeBoxWhenBuyingSinglePack() {
        CardSet deltaReign = CardSet.builder()
                .id(1L)
                .name("Delta Reign")
                .code("DLR")
                .baseSetTotal(103)
                .cardsPerPack(5)
                .packsPerBox(30)
                .build();

        BoosterBox box = BoosterBox.builder()
                .id(10L)
                .boxCode("DLR-BOX-0001")
                .cardSet(deltaReign)
                .totalPacks(30)
                .remainingPacks(30)
                .canBuyWholeBox(true)
                .snapshotBoxPrice(new BigDecimal("1250000.00"))
                .status(BoxStatus.AVAILABLE)
                .version(0L)
                .build();

        BoosterPack packPos1 = BoosterPack.builder()
                .id(101L)
                .boosterBox(box)
                .positionInBox(1)
                .snapshotCardsPerPack(5)
                .snapshotPackPrice(new BigDecimal("45000.00"))
                .status(PackStatus.AVAILABLE)
                .version(0L)
                .build();

        Wallet wallet = Wallet.builder()
                .id(1L)
                .userId(99L)
                .balance(new BigDecimal("200000.00"))
                .lockedBalance(BigDecimal.ZERO)
                .version(0L)
                .build();

        when(boosterPackRepository.findByBoosterBoxIdAndPositionInBox(10L, 1)).thenReturn(Optional.of(packPos1));
        when(walletRepository.findByUserId(99L)).thenReturn(Optional.of(wallet));
        when(boosterBoxRepository.decrementRemainingPackAtomically(10L)).thenReturn(1);

        BoxPurchaseResponseDTO response = boxPurchaseService.buySinglePack(99L, 10L, 1);

        assertThat(response.canBuyWholeBox()).isFalse();
        assertThat(response.remainingPacks()).isEqualTo(29);
        assertThat(response.boxStatus()).isEqualTo(BoxStatus.PARTIALLY_SOLD);
        assertThat(response.remainingWalletBalance()).isEqualByComparingTo(new BigDecimal("155000.00"));
        assertThat(packPos1.getStatus()).isEqualTo(PackStatus.SOLD);
        assertThat(packPos1.getBuyerId()).isEqualTo(99L);

        verify(boosterPackRepository).saveAndFlush(packPos1);
        verify(boosterBoxRepository).decrementRemainingPackAtomically(10L);
        verify(walletService).recordTransaction(eq(wallet), any(), eq(new BigDecimal("45000.00")), anyString());
    }

    @Test
    @DisplayName("2. Mua nguyên Box: Từ chối ngay lập tức nếu canBuyWholeBox == false hoặc đã bán 1 Pack lẻ")
    void shouldRejectBuyWholeBoxWhenSinglePackAlreadySold() {
        BoosterBox partiallySoldBox = BoosterBox.builder()
                .id(10L)
                .boxCode("DLR-BOX-0001")
                .totalPacks(30)
                .remainingPacks(29)
                .canBuyWholeBox(false)
                .status(BoxStatus.PARTIALLY_SOLD)
                .build();

        when(boosterBoxRepository.findById(10L)).thenReturn(Optional.of(partiallySoldBox));

        assertThatThrownBy(() -> boxPurchaseService.buyWholeBox(99L, 10L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Không thể mua nguyên hộp");

        verifyNoInteractions(walletRepository);
    }

    @Test
    @DisplayName("3. GlobalExceptionHandler bắt ObjectOptimisticLockingFailureException trả về HTTP 409 CONFLICT")
    void shouldReturn409ConflictWhenTwoPlayersBuySamePackSimultaneously() {
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getRequestURI()).thenReturn("/api/v1/boxes/10/packs/buy");

        ObjectOptimisticLockingFailureException lockEx =
                new ObjectOptimisticLockingFailureException(BoosterPack.class, 101L);

        ResponseEntity<ApiErrorResponse> response =
                exceptionHandler.handleOptimisticLockingFailure(lockEx, mockRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .isEqualTo("Vật phẩm hoặc mức giá vừa được người chơi khác cập nhật. Vui lòng thao tác lại!");
    }

    @Test
    @DisplayName("4. Thuật toán Gacha openPack: Đọc số lượng thẻ từ snapshotCardsPerPack (không đọc CardSet.cardsPerPack)")
    void shouldDrawCardsUsingSnapshotCardsPerPackAndRedisCachedWeights() {
        // Giả sử CardSet đã bị Admin cập nhật cardsPerPack lên 10, nhưng Pack cũ có snapshotCardsPerPack = 5
        CardSet deltaReign = CardSet.builder()
                .id(1L)
                .name("Delta Reign")
                .code("DLR")
                .baseSetTotal(103)
                .cardsPerPack(10)
                .build();

        BoosterBox box = BoosterBox.builder()
                .id(10L)
                .cardSet(deltaReign)
                .build();

        BoosterPack soldPack = BoosterPack.builder()
                .id(101L)
                .boosterBox(box)
                .positionInBox(2)
                .snapshotCardsPerPack(5) // Đóng băng 5 thẻ/pack
                .status(PackStatus.SOLD)
                .buyerId(99L)
                .version(1L)
                .build();

        RarityResponseDTO uncommonRarity = RarityResponseDTO.builder()
                .id(2L)
                .name("Không phổ biến")
                .dropWeight(280)
                .colorHex("#10B981")
                .build();

        CardTemplateResponseDTO card98 = CardTemplateResponseDTO.builder()
                .id(980L)
                .name("Hầm hào huyền thoại")
                .cardNumber(98)
                .baseSetTotal(103)
                .formattedCardNumber("098 / 103")
                .rarityId(2L)
                .rarityName("Không phổ biến")
                .speciesId(1L)
                .speciesName("Hầm hào huyền thoại")
                .build();

        when(boosterPackRepository.findWithBoxAndSetById(101L)).thenReturn(Optional.of(soldPack));
        when(catalogCacheService.getAllRaritiesWithWeights()).thenReturn(List.of(uncommonRarity));
        when(catalogCacheService.getCardTemplatesBySetAndRarity(1L, 2L)).thenReturn(List.of(card98));
        when(cardTemplateRepository.getReferenceById(980L)).thenReturn(CardTemplate.builder().id(980L).build());
        when(userCardRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        OpenPackResultResponseDTO result = gachaEngineService.openPack(99L, 101L);

        // Phải rút đúng 5 thẻ từ snapshotCardsPerPack chứ không phải 10 thẻ từ CardSet
        assertThat(result.snapshotCardsPerPack()).isEqualTo(5);
        assertThat(result.drawnCards()).hasSize(5);
        assertThat(result.drawnCards().getFirst().card().formattedCardNumber()).isEqualTo("098 / 103");
        assertThat(soldPack.getStatus()).isEqualTo(PackStatus.OPENED);

        verify(boosterPackRepository).saveAndFlush(soldPack);
        verify(userCardRepository).saveAll(anyList());
    }
}
