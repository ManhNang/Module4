package com.pokemon.blindbox.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CreateCardTemplateRequest(
        @NotBlank(message = "Tên thẻ bài không được để trống")
        @Size(max = 150, message = "Tên thẻ bài tối đa 150 ký tự")
        String name,

        @NotNull(message = "Số thứ tự thẻ (cardNumber) không được để trống")
        @Min(value = 1, message = "Số thứ tự thẻ phải lớn hơn hoặc bằng 1")
        Integer cardNumber,

        @Size(max = 500, message = "Đường dẫn ảnh tối đa 500 ký tự")
        String imageUrl,

        @NotNull(message = "Mã độ hiếm (rarityId) không được để trống")
        Long rarityId,

        @NotNull(message = "Mã loài/phân loại (speciesId) không được để trống")
        Long speciesId
) {
}
