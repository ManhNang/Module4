package com.pokemon.blindbox.dto.request;

import jakarta.validation.constraints.*;
import lombok.Builder;

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

        Boolean isHolographic
) {
}
