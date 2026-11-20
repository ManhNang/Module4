package com.pokemon.blindbox.dto.response;

import com.pokemon.blindbox.entity.BoosterBox;
import com.pokemon.blindbox.entity.enums.BoxStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record BoosterBoxSummaryResponseDTO(
        Long id,
        String boxCode,
        Long setId,
        String setCode,
        Integer totalPacks,
        Integer remainingPacks,
        Integer snapshotCardsPerPack,
        boolean canBuyWholeBox,
        BigDecimal snapshotBoxPrice,
        BoxStatus status,
        LocalDateTime createdAt
) {
    public static BoosterBoxSummaryResponseDTO fromEntity(BoosterBox box) {
        Integer snapshotCards = box.getSnapshotCardsPerPack() != null
                ? box.getSnapshotCardsPerPack()
                : box.getCardSet().getCardsPerPack();

        return BoosterBoxSummaryResponseDTO.builder()
                .id(box.getId())
                .boxCode(box.getBoxCode())
                .setId(box.getCardSet().getId())
                .setCode(box.getCardSet().getCode())
                .totalPacks(box.getTotalPacks())
                .remainingPacks(box.getRemainingPacks())
                .snapshotCardsPerPack(snapshotCards)
                .canBuyWholeBox(box.isCanBuyWholeBox())
                .snapshotBoxPrice(box.getSnapshotBoxPrice())
                .status(box.getStatus())
                .createdAt(box.getCreatedAt())
                .build();
    }
}
