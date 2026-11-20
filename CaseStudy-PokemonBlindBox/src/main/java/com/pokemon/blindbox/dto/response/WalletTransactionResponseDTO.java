package com.pokemon.blindbox.dto.response;

import com.pokemon.blindbox.entity.WalletTransaction;
import com.pokemon.blindbox.entity.enums.TransactionStatus;
import com.pokemon.blindbox.entity.enums.TransactionType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record WalletTransactionResponseDTO(
        Long id,
        String vnpTxnRef,
        String vnpTransactionNo,
        String vnpBankCode,
        String vnpResponseCode,
        TransactionType type,
        TransactionStatus status,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String description,
        LocalDateTime createdAt
) {
    public static WalletTransactionResponseDTO fromEntity(WalletTransaction tx) {
        return WalletTransactionResponseDTO.builder()
                .id(tx.getId())
                .vnpTxnRef(tx.getVnpTxnRef())
                .vnpTransactionNo(tx.getVnpTransactionNo())
                .vnpBankCode(tx.getVnpBankCode())
                .vnpResponseCode(tx.getVnpResponseCode())
                .type(tx.getType())
                .status(tx.getStatus())
                .amount(tx.getAmount())
                .balanceAfter(tx.getBalanceAfter())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
