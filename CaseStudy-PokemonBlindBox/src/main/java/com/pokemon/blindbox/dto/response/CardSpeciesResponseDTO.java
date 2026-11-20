package com.pokemon.blindbox.dto.response;

import com.pokemon.blindbox.entity.CardSpecies;

import java.io.Serializable;

/**
 * DTO thay thế việc truyền trực tiếp Entity CardSpecies ra Thymeleaf View & Redis Cache (P3-04).
 */
public record CardSpeciesResponseDTO(
        Long id,
        String name,
        Boolean isBeta
) implements Serializable {

    public static CardSpeciesResponseDTO fromEntity(CardSpecies species) {
        return new CardSpeciesResponseDTO(
                species.getId(),
                species.getName(),
                species.getIsBeta()
        );
    }
}
