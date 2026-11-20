package com.pokemon.blindbox.service;

import com.pokemon.blindbox.dto.request.CardTemplateFilterRequest;
import com.pokemon.blindbox.dto.response.BinderSlotDTO;
import com.pokemon.blindbox.dto.response.CardTemplateResponseDTO;
import com.pokemon.blindbox.dto.response.MasterSetProgressDTO;
import com.pokemon.blindbox.entity.CardSet;
import com.pokemon.blindbox.entity.CardSpecies;
import com.pokemon.blindbox.entity.CardTemplate;
import com.pokemon.blindbox.entity.Rarity;
import com.pokemon.blindbox.repository.CardSetRepository;
import com.pokemon.blindbox.repository.CardTemplateRepository;
import com.pokemon.blindbox.repository.UserCardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DigitalBinderServiceTest {

    @Mock
    private CardSetRepository cardSetRepository;

    @Mock
    private CardTemplateRepository cardTemplateRepository;

    @Mock
    private UserCardRepository userCardRepository;

    @InjectMocks
    private DigitalBinderService digitalBinderService;

    private CardSet deltaReign;
    private CardTemplate card52Steelix;
    private CardTemplate card98LegendaryTrench;
    private CardTemplate card141MegaRayquaza;

    @BeforeEach
    void setUp() {
        deltaReign = CardSet.builder()
                .id(1L)
                .name("Delta Reign")
                .code("DLR")
                .baseSetTotal(103)
                .cardsPerPack(5)
                .packsPerBox(30)
                .build();

        Rarity rare = Rarity.builder().id(3L).name("Hiếm").dropWeight(90).colorHex("#3B82F6").build();
        Rarity uncommon = Rarity.builder().id(2L).name("Không phổ biến").dropWeight(280).colorHex("#10B981").build();
        Rarity hyperRare = Rarity.builder().id(5L).name("Hyper Rare").dropWeight(5).colorHex("#EC4899").build();

        CardSpecies steelixSpecies = CardSpecies.builder().id(2L).name("Steelix").isBeta(false).build();
        CardSpecies trenchSpecies = CardSpecies.builder().id(1L).name("Hầm hào huyền thoại").isBeta(true).build();
        CardSpecies rayquazaSpecies = CardSpecies.builder().id(3L).name("Rayquaza").isBeta(true).build();

        card52Steelix = CardTemplate.builder()
                .id(52L)
                .name("Steelix")
                .cardNumber(52)
                .imageUrl("https://assets.pokemon-blindbox.vn/cards/dlr/052_steelix.webp")
                .cardSet(deltaReign)
                .rarity(rare)
                .cardSpecies(steelixSpecies)
                .build();

        card98LegendaryTrench = CardTemplate.builder()
                .id(98L)
                .name("Hầm hào huyền thoại")
                .cardNumber(98)
                .imageUrl("https://assets.pokemon-blindbox.vn/cards/dlr/098_ham_hao_huyen_thoai.webp")
                .cardSet(deltaReign)
                .rarity(uncommon)
                .cardSpecies(trenchSpecies)
                .build();

        card141MegaRayquaza = CardTemplate.builder()
                .id(141L)
                .name("Mega Rayquaza ex")
                .cardNumber(141)
                .imageUrl("https://assets.pokemon-blindbox.vn/cards/dlr/141_mega_rayquaza_ex.webp")
                .cardSet(deltaReign)
                .rarity(hyperRare)
                .cardSpecies(rayquazaSpecies)
                .build();
    }

    @Test
    @DisplayName("1. Lọc thẻ bài với CardTemplateSpecification theo Tên, Số thứ tự 98, Độ hiếm 'Không phổ biến', Đặt 'Delta Reign', Loài 'Hầm hào huyền thoại'")
    @SuppressWarnings("unchecked")
    void shouldFilterCardTemplatesUsingSpecificationAndFormatCardNumber() {
        CardTemplateFilterRequest filter = CardTemplateFilterRequest.builder()
                .name("Hầm hào")
                .cardNumber(98)
                .rarityName("Không phổ biến")
                .setName("Delta Reign")
                .speciesName("Hầm hào huyền thoại")
                .build();

        Page<CardTemplate> mockPage = new PageImpl<>(List.of(card98LegendaryTrench));
        when(cardTemplateRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(mockPage);

        Page<CardTemplateResponseDTO> result = digitalBinderService.searchCardTemplates(filter, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        CardTemplateResponseDTO dto = result.getContent().getFirst();
        assertThat(dto.name()).isEqualTo("Hầm hào huyền thoại");
        assertThat(dto.cardNumber()).isEqualTo(98);
        assertThat(dto.formattedCardNumber()).isEqualTo("098 / 103");
        assertThat(dto.rarityName()).isEqualTo("Không phổ biến");
        assertThat(dto.setName()).isEqualTo("Delta Reign");
        assertThat(dto.speciesName()).isEqualTo("Hầm hào huyền thoại");
        assertThat(dto.isBetaSpecies()).isTrue();

        verify(cardTemplateRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("2. Thống kê MasterSetProgressDTO: Tách thẻ chuẩn (<= 103) và thẻ Secret (> 103), trả về đủ 103 ô sắp xếp 001..103 kèm cờ isOwned")
    void shouldCalculateMasterSetProgressAndGenerate103OrderedSlots() {
        when(cardSetRepository.findById(1L)).thenReturn(Optional.of(deltaReign));
        when(cardTemplateRepository.findByCardSetIdOrderByCardNumberAsc(1L))
                .thenReturn(List.of(card52Steelix, card98LegendaryTrench, card141MegaRayquaza));

        // Giả sử người chơi sở hữu: 3 thẻ #98 (trùng lặp) và 1 thẻ Secret #141, nhưng CHƯA sở hữu thẻ #52
        UserCardRepository.OwnedCardCountProjection owned98 = new UserCardRepository.OwnedCardCountProjection() {
            @Override
            public Long getCardTemplateId() {
                return 98L;
            }

            @Override
            public Long getOwnedQuantity() {
                return 3L;
            }
        };
        UserCardRepository.OwnedCardCountProjection owned141 = new UserCardRepository.OwnedCardCountProjection() {
            @Override
            public Long getCardTemplateId() {
                return 141L;
            }

            @Override
            public Long getOwnedQuantity() {
                return 1L;
            }
        };

        when(userCardRepository.countOwnedCardsByUserAndSet(10L, 1L))
                .thenReturn(List.of(owned98, owned141));

        MasterSetProgressDTO progress = digitalBinderService.getMasterSetProgress(10L, 1L);

        // Kiểm tra thống kê tổng quan:
        assertThat(progress.baseSetTotal()).isEqualTo(103);
        assertThat(progress.ownedBaseCardCount()).isEqualTo(1); // Chỉ tính 1 thẻ chuẩn duy nhất (#98), không cộng dồn thẻ trùng hay thẻ Secret #141
        assertThat(progress.formattedBaseProgress()).isEqualTo("1 / 103");
        assertThat(progress.totalSecretRareInSet()).isEqualTo(1);
        assertThat(progress.ownedSecretRareCount()).isEqualTo(1);

        // Kiểm tra danh sách 103 ô chuẩn (001 / 103 -> 103 / 103) sắp xếp ORDER BY cardNumber ASC:
        assertThat(progress.baseSetSlots()).hasSize(103);
        assertThat(progress.baseSetSlots().getFirst().formattedCardNumber()).isEqualTo("001 / 103");
        assertThat(progress.baseSetSlots().getLast().formattedCardNumber()).isEqualTo("103 / 103");

        // Ô #52 (Steelix): Chưa sở hữu -> isOwned = false (hiển thị bóng mờ)
        BinderSlotDTO slot52 = progress.baseSetSlots().get(51);
        assertThat(slot52.cardNumber()).isEqualTo(52);
        assertThat(slot52.formattedCardNumber()).isEqualTo("052 / 103");
        assertThat(slot52.isOwned()).isFalse();
        assertThat(slot52.ownedQuantity()).isZero();
        assertThat(slot52.cardTemplate()).isNotNull();
        assertThat(slot52.cardTemplate().name()).isEqualTo("Steelix");

        // Ô #98 (Hầm hào huyền thoại): Đã sở hữu 3 bản -> isOwned = true (hiển thị sáng)
        BinderSlotDTO slot98 = progress.baseSetSlots().get(97);
        assertThat(slot98.cardNumber()).isEqualTo(98);
        assertThat(slot98.formattedCardNumber()).isEqualTo("098 / 103");
        assertThat(slot98.isOwned()).isTrue();
        assertThat(slot98.ownedQuantity()).isEqualTo(3L);

        // Kiểm tra danh sách thẻ Secret vượt khung (141 / 103):
        assertThat(progress.secretRareSlots()).hasSize(1);
        BinderSlotDTO slot141 = progress.secretRareSlots().getFirst();
        assertThat(slot141.cardNumber()).isEqualTo(141);
        assertThat(slot141.formattedCardNumber()).isEqualTo("141 / 103");
        assertThat(slot141.isOwned()).isTrue();
        assertThat(slot141.secretRare()).isTrue();
    }
}
