package com.pokemon.blindbox.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record VNPayPaymentUrlResponseDTO(
        String paymentUrl,
        String vnpTxnRef,
        BigDecimal amount,
        LocalDateTime expiresAt
) {
}
