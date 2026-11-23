package com.pokemon.blindbox.dto.request;

import com.pokemon.blindbox.entity.enums.RevealEffect;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record UpsertRarityRequest(
        @NotBlank(message = "Tên độ hiếm không được để trống")
        @Size(max = 100, message = "Tên độ hiếm tối đa 100 ký tự")
        String name,

        @NotNull(message = "Trọng số rơi (dropWeight) không được để trống")
        @Min(value = 0, message = "Trọng số rơi phải >= 0")
        @Max(value = 1000000, message = "Trọng số rơi tối đa 1,000,000")
        Integer dropWeight,

        @NotBlank(message = "Mã màu colorHex không được để trống")
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Mã màu phải đúng định dạng Hex 6 ký tự (VD: #EC4899)")
        String colorHex,

        Boolean isHolographic,

        RevealEffect revealEffect,

        @Min(value = 1, message = "Tốc độ sinh Xu trên đảo phải >= 1 Xu/phút")
        @Max(value = 10000, message = "Tốc độ sinh Xu trên đảo tối đa 10,000 Xu/phút")
        Integer sanctuaryCoinsPerMin,

        @DecimalMin(value = "1.00", message = "Giá bán nhanh phải >= 1 Xu")
        @DecimalMax(value = "100000000.00", message = "Giá bán nhanh tối đa 100,000,000 Xu")
        BigDecimal buybackPrice
) {
}
