package com.pokemon.blindbox.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CreateMarketListingRequest(
        Long sellerId,

        @NotNull(message = "userCardId không được để trống")
        Long userCardId,

        @NotNull(message = "Giá bán cố định (price) không được để trống")
        @DecimalMin(value = "1000.00", message = "Giá bán tối thiểu là 1,000 VND")
        @DecimalMax(value = "1000000000.00", message = "Giá bán tối đa là 1,000,000,000 VND")
        @Digits(integer = 13, fraction = 2, message = "Giá bán không hợp lệ")
        BigDecimal price
) {
}
