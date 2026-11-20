package com.pokemon.blindbox.dto.response;

import com.pokemon.blindbox.entity.Wallet;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record WalletResponseDTO(
        Long id,
        Long userId,
        BigDecimal balance,
        BigDecimal lockedBalance,
        Long version,
        LocalDateTime updatedAt
) {
    public static WalletResponseDTO fromEntity(Wallet wallet) {
        return WalletResponseDTO.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .balance(wallet.getBalance())
                .lockedBalance(wallet.getLockedBalance())
                .version(wallet.getVersion())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }
}
