package com.pokemon.blindbox.dto.response;

import com.pokemon.blindbox.entity.BoosterPack;
import com.pokemon.blindbox.entity.enums.PackStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record BoosterPackResponseDTO(
        Long id,
        Long boxId,
        Integer positionInBox,
        Integer snapshotCardsPerPack,
        BigDecimal snapshotPackPrice,
        PackStatus status,
        Long buyerId,
        LocalDateTime purchasedAt,
        LocalDateTime openedAt
) {
    public static BoosterPackResponseDTO fromEntity(BoosterPack pack) {
        return BoosterPackResponseDTO.builder()
                .id(pack.getId())
                .boxId(pack.getBoosterBox().getId())
                .positionInBox(pack.getPositionInBox())
                .snapshotCardsPerPack(pack.getSnapshotCardsPerPack())
                .snapshotPackPrice(pack.getSnapshotPackPrice())
                .status(pack.getStatus())
                .buyerId(pack.getBuyerId())
                .purchasedAt(pack.getPurchasedAt())
                .openedAt(pack.getOpenedAt())
                .build();
    }
}
