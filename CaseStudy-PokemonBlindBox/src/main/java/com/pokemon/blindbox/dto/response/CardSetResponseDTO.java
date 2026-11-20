package com.pokemon.blindbox.dto.response;

import com.pokemon.blindbox.entity.CardSet;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record CardSetResponseDTO(
        Long id,
        String name,
        String code,
        Integer baseSetTotal,
        Integer cardsPerPack,
        Integer packsPerBox,
        BigDecimal packPrice,
        BigDecimal boxPrice,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CardSetResponseDTO fromEntity(CardSet entity) {
        return CardSetResponseDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .baseSetTotal(entity.getBaseSetTotal())
                .cardsPerPack(entity.getCardsPerPack())
                .packsPerBox(entity.getPacksPerBox())
                .packPrice(entity.getPackPrice())
                .boxPrice(entity.getBoxPrice())
                .isActive(entity.getIsActive() == null || entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
