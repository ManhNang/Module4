package com.pokemon.blindbox.dto.request;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record UpdateCardSetRequest(
        @NotBlank(message = "Tên bộ sưu tập không được để trống")
        @Size(max = 150, message = "Tên bộ sưu tập tối đa 150 ký tự")
        String name,

        @NotBlank(message = "Mã bộ sưu tập (code) không được để trống")
        @Pattern(regexp = "^[A-Za-z0-9_-]{2,20}$", message = "Mã bộ chỉ gồm chữ, số, _, - (2–20 ký tự)")
        String code,

        @NotNull(message = "Tổng số thẻ chuẩn không được để trống")
        @Min(value = 1, message = "Tổng số thẻ chuẩn phải >= 1")
        @Max(value = 10000, message = "Tổng số thẻ chuẩn tối đa 10,000")
        Integer baseSetTotal,

        @NotNull(message = "Số thẻ mỗi gói không được để trống")
        @Min(value = 1, message = "Số thẻ mỗi gói phải >= 1")
        @Max(value = 50, message = "Số thẻ mỗi gói tối đa 50")
        Integer cardsPerPack,

        @NotNull(message = "Số gói mỗi hộp không được để trống")
        @Min(value = 1, message = "Số gói mỗi hộp phải >= 1")
        @Max(value = 200, message = "Số gói mỗi hộp tối đa 200")
        Integer packsPerBox,

        @NotNull(message = "Giá 1 gói không được để trống")
        @DecimalMin(value = "1000.00", message = "Giá 1 gói tối thiểu 1,000 VND")
        @DecimalMax(value = "1000000000.00", message = "Giá 1 gói tối đa 1,000,000,000 VND")
        @Digits(integer = 13, fraction = 2, message = "Giá 1 gói không hợp lệ")
        BigDecimal packPrice,

        @NotNull(message = "Giá nguyên hộp không được để trống")
        @DecimalMin(value = "1000.00", message = "Giá nguyên hộp tối thiểu 1,000 VND")
        @DecimalMax(value = "1000000000.00", message = "Giá nguyên hộp tối đa 1,000,000,000 VND")
        @Digits(integer = 13, fraction = 2, message = "Giá nguyên hộp không hợp lệ")
        BigDecimal boxPrice,

        Boolean isActive
) {
}
