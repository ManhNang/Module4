package com.pokemon.blindbox.dto.response;

import com.pokemon.blindbox.entity.enums.Role;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record AdminUserSummaryDTO(
        Long id,
        String username,
        String email,
        Role role,
        Boolean isLocked,
        LocalDateTime createdAt,
        BigDecimal balance,
        BigDecimal lockedBalance,
        Long totalCardsOwned
) {
}
