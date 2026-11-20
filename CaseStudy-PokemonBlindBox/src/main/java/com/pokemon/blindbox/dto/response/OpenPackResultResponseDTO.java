package com.pokemon.blindbox.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record OpenPackResultResponseDTO(
        Long packId,
        Long boxId,
        Integer positionInBox,
        Integer snapshotCardsPerPack,
        LocalDateTime openedAt,
        List<UserCardResponseDTO> drawnCards
) {
}
