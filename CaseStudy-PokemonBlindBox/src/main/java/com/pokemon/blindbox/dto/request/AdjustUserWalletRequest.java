package com.pokemon.blindbox.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record AdjustUserWalletRequest(
        @NotNull(message = "Số Xu điều chỉnh không được để trống")
        @DecimalMin(value = "1", message = "Số Xu điều chỉnh tối thiểu là 1 Xu")
        @DecimalMax(value = "500000000", message = "Số Xu điều chỉnh tối đa là 500.000.000 Xu")
        @Digits(integer = 12, fraction = 2, message = "Số Xu không hợp lệ")
        BigDecimal amount,

        @NotNull(message = "Loại điều chỉnh (cộng/trừ) không được để trống")
        Boolean credit,

        @NotBlank(message = "Lý do điều chỉnh ví không được để trống")
        @Size(max = 255, message = "Lý do tối đa 255 ký tự")
        String reason
) {
}
