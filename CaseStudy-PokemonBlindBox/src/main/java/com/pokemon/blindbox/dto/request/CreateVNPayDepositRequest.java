package com.pokemon.blindbox.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CreateVNPayDepositRequest(
        Long userId,

        @NotNull(message = "Số tiền nạp (amount) không được để trống")
        @DecimalMin(value = "10000.00", message = "Số tiền nạp tối thiểu là 10,000 VND")
        @DecimalMax(value = "1000000000.00", message = "Số tiền nạp tối đa mỗi lần là 1,000,000,000 VND")
        @Digits(integer = 13, fraction = 2, message = "Số tiền nạp không hợp lệ")
        BigDecimal amount,

        @Size(max = 32, message = "Mã ngân hàng tối đa 32 ký tự") String bankCode,

        @Size(max = 10, message = "Ngôn ngữ hiển thị tối đa 10 ký tự") String locale) {
}
