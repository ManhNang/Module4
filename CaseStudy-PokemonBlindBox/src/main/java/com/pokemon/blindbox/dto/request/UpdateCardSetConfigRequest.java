package com.pokemon.blindbox.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record UpdateCardSetConfigRequest(
        @NotNull(message = "Số lượng thẻ mỗi gói (cardsPerPack) không được để trống")
        @Min(value = 1, message = "Số lượng thẻ mỗi gói phải >= 1")
        Integer cardsPerPack,

        @NotNull(message = "Số lượng gói mỗi hộp (packsPerBox) không được để trống")
        @Min(value = 1, message = "Số lượng gói mỗi hộp phải >= 1")
        Integer packsPerBox,

        @DecimalMin(value = "0.0", inclusive = false, message = "Giá gói lẻ phải lớn hơn 0")
        BigDecimal packPrice,

        @DecimalMin(value = "0.0", inclusive = false, message = "Giá nguyên hộp phải lớn hơn 0")
        BigDecimal boxPrice,

        @Min(value = 0, message = "Hạn mức Pack/ngày phải >= 0")
        Integer dailyPackLimit,

        @Min(value = 0, message = "Hạn mức Box/ngày phải >= 0")
        Integer dailyBoxLimit
) {
}
