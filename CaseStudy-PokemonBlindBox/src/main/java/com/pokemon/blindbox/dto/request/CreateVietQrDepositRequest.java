package com.pokemon.blindbox.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateVietQrDepositRequest(
        @NotNull(message = "Vui lòng nhập số tiền cần nạp!")
        @DecimalMin(value = "10000", message = "Số tiền nạp tối thiểu là 10.000 ₫!")
        @DecimalMax(value = "100000000", message = "Số tiền nạp tối đa mỗi giao dịch là 100.000.000 ₫!")
        BigDecimal amount
) {}
