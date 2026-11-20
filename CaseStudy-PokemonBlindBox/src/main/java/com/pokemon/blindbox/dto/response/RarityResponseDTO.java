package com.pokemon.blindbox.dto.response;

import com.pokemon.blindbox.entity.Rarity;
import lombok.Builder;

@Builder
public record RarityResponseDTO(
        Long id,
        String name,
        Integer dropWeight,
        String colorHex,
        Boolean isHolographic
) {
    public static RarityResponseDTO fromEntity(Rarity entity) {
        return RarityResponseDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .dropWeight(entity.getDropWeight())
                .colorHex(entity.getColorHex())
                .isHolographic(Boolean.TRUE.equals(entity.getIsHolographic()))
                .build();
    }
}
