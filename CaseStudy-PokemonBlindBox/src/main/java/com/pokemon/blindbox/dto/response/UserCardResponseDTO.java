package com.pokemon.blindbox.dto.response;

import com.pokemon.blindbox.entity.UserCard;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record UserCardResponseDTO(
        Long userCardId,
        Long userId,
        Long boosterPackId,
        Boolean isLocked,
        LocalDateTime acquiredAt,
        CardTemplateResponseDTO card
) {
    public static UserCardResponseDTO fromEntity(UserCard userCard, CardTemplateResponseDTO cardDto) {
        return UserCardResponseDTO.builder()
                .userCardId(userCard.getId())
                .userId(userCard.getUserId())
                .boosterPackId(userCard.getBoosterPack().getId())
                .isLocked(userCard.getIsLocked())
                .acquiredAt(userCard.getAcquiredAt())
                .card(cardDto)
                .build();
    }
}
