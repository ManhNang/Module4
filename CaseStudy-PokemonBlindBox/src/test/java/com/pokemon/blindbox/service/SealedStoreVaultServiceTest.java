package com.pokemon.blindbox.service;

import com.pokemon.blindbox.dto.response.CardTemplateResponseDTO;
import com.pokemon.blindbox.dto.response.RarityResponseDTO;
import com.pokemon.blindbox.dto.response.UserCardResponseDTO;
import com.pokemon.blindbox.entity.CardSet;
import com.pokemon.blindbox.entity.CardTemplate;
import com.pokemon.blindbox.entity.UserCard;
import com.pokemon.blindbox.entity.UserSealedItem;
import com.pokemon.blindbox.entity.enums.SealedItemType;
import com.pokemon.blindbox.exception.BusinessValidationException;
import com.pokemon.blindbox.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SealedStoreVaultServiceTest {

    @Mock private CardSetRepository cardSetRepository;
    @Mock private SetDailySaleRepository setDailySaleRepository;
    @Mock private UserSealedItemRepository userSealedItemRepository;
    @Mock private CardTemplateRepository cardTemplateRepository;
    @Mock private UserCardRepository userCardRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private WalletService walletService;
    @Mock private CatalogCacheService catalogCacheService;
    @Mock private ObjectProvider<SanctuaryService> sanctuaryServiceProvider;

    @InjectMocks
    private SealedStoreVaultService sealedStoreVaultService;

    private CardSet sampleSet() {
        return CardSet.builder()
                .id(1L).name("Delta Reign").code("DLR")
                .baseSetTotal(103).cardsPerPack(6).packsPerBox(20)
                .packPrice(new BigDecimal("45000.00"))
                .boxPrice(new BigDecimal("810000.00"))
                .dailyPackLimit(200).dailyBoxLimit(15)
                .isActive(true)
                .build();
    }

    private List<UserSealedItem> buildPacks(CardSet set, int count) {
        List<UserSealedItem> packs = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            packs.add(UserSealedItem.builder()
                    .id((long) i).userId(10L).cardSet(set)
                    .itemType(SealedItemType.PACK)
                    .cardsPerPack(6).packsPerBox(20)
                    .isLocked(false).isConsumed(false)
                    .build());
        }
        return packs;
    }

    private void stubPools() {
        RarityResponseDTO common = RarityResponseDTO.builder()
                .id(1L).name("Common").dropWeight(100).colorHex("#64748B").isHolographic(false)
                .buybackPrice(new BigDecimal("3000.00")).build();
        CardTemplateResponseDTO tpl = CardTemplateResponseDTO.builder()
                .id(50L).name("Sprigatito").cardNumber(12).baseSetTotal(103)
                .formattedCardNumber("012 / 103").imageUrl("https://example.com/12.webp")
                .setId(1L).setName("Delta Reign").setCode("DLR")
                .rarityId(1L).rarityName("Common").rarityColorHex("#64748B").rarityIsHolographic(false)
                .rarityBuybackPrice(new BigDecimal("3000.00"))
                .speciesId(1L).speciesName("Sprigatito").build();

        when(catalogCacheService.getAllRaritiesWithWeights()).thenReturn(List.of(common));
        when(catalogCacheService.getCardTemplatesBySetAndRarity(1L, 1L)).thenReturn(List.of(tpl));
        when(cardTemplateRepository.getReferenceById(50L)).thenReturn(CardTemplate.builder().id(50L).build());
        when(userCardRepository.saveAllAndFlush(anyList())).thenAnswer(inv -> {
            List<UserCard> list = inv.getArgument(0);
            long idSeq = 1000L;
            for (UserCard uc : list) {
                uc.setId(idSeq++);
            }
            return list;
        });
    }

    @Test
    @DisplayName("Mở 1 Pack từ Kho đồ thành công, trừ 1 Pack và tạo 6 thẻ (boosterPack=null không bị NPE)")
    void openSinglePackFromVault_success() {
        CardSet set = sampleSet();
        List<UserSealedItem> packs = buildPacks(set, 1);
        when(userSealedItemRepository.findAvailableForUpdate(10L, 1L, SealedItemType.PACK, PageRequest.of(0, 1)))
                .thenReturn(packs);
        stubPools();

        Map<String, Object> result = sealedStoreVaultService.openSealedPacks(10L, 1L, 1);

        assertThat(packs.getFirst().getIsConsumed()).isTrue();
        assertThat(result.get("packsOpened")).isEqualTo(1);
        @SuppressWarnings("unchecked")
        List<UserCardResponseDTO> drawn = (List<UserCardResponseDTO>) result.get("drawnCards");
        assertThat(drawn).hasSize(6);
        assertThat(drawn.getFirst().boosterPackId()).isNull();
    }

    @Test
    @DisplayName("Mở nhanh 5 Pack từ Kho đồ thành công, trừ 5 Pack và tạo đủ 30 thẻ")
    void openQuickFivePacksFromVault_success() {
        CardSet set = sampleSet();
        List<UserSealedItem> packs = buildPacks(set, 5);
        when(userSealedItemRepository.findAvailableForUpdate(10L, 1L, SealedItemType.PACK, PageRequest.of(0, 5)))
                .thenReturn(packs);
        stubPools();

        Map<String, Object> result = sealedStoreVaultService.openSealedPacks(10L, 1L, 5);

        assertThat(packs).allMatch(p -> Boolean.TRUE.equals(p.getIsConsumed()));
        assertThat(result.get("packsOpened")).isEqualTo(5);
        @SuppressWarnings("unchecked")
        List<UserCardResponseDTO> drawn = (List<UserCardResponseDTO>) result.get("drawnCards");
        assertThat(drawn).hasSize(30);
    }

    @Test
    @DisplayName("Mở nhanh 5 Pack khi kho chỉ còn 3 Pack phải báo lỗi nghiệp vụ rõ ràng (4xx)")
    void openQuickFivePacks_insufficientPacks_throwsBusinessValidation() {
        CardSet set = sampleSet();
        when(userSealedItemRepository.findAvailableForUpdate(10L, 1L, SealedItemType.PACK, PageRequest.of(0, 5)))
                .thenReturn(buildPacks(set, 3));

        assertThatThrownBy(() -> sealedStoreVaultService.openSealedPacks(10L, 1L, 5))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("5 Pack");
    }
}
