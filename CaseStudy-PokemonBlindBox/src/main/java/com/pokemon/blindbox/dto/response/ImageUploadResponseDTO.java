package com.pokemon.blindbox.dto.response;

import lombok.Builder;

@Builder
public record ImageUploadResponseDTO(
        String imageUrl
) {
}
