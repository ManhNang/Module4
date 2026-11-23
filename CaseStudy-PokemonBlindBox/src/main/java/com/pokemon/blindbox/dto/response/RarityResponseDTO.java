package com.pokemon.blindbox.dto.response;

import com.pokemon.blindbox.entity.Rarity;
import com.pokemon.blindbox.entity.enums.RevealEffect;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record RarityResponseDTO(
        Long id,
        String name,
        Integer dropWeight,
        String colorHex,
        Boolean isHolographic,
        String revealEffect,
        Integer sanctuaryCoinsPerMin,
        BigDecimal buybackPrice
) {
    public static RarityResponseDTO fromEntity(Rarity entity) {
        return RarityResponseDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .dropWeight(entity.getDropWeight())
                .colorHex(entity.getColorHex())
                .isHolographic(Boolean.TRUE.equals(entity.getIsHolographic()))
                .revealEffect(entity.getRevealEffect() != null ? entity.getRevealEffect().name() : RevealEffect.NONE.name())
                .sanctuaryCoinsPerMin(entity.getSanctuaryCoinsPerMin() != null ? entity.getSanctuaryCoinsPerMin() : 2)
                .buybackPrice(entity.getBuybackPrice() != null ? entity.getBuybackPrice() : new BigDecimal("5.00"))
                .build();
    }
}
