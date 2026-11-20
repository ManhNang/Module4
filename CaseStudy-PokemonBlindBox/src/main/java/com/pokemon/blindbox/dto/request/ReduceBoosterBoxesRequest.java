package com.pokemon.blindbox.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record ReduceBoosterBoxesRequest(
        @NotNull(message = "Số lượng hộp cần thu hồi không được để trống")
        @Min(value = 1, message = "Số lượng thu hồi phải >= 1")
        @Max(value = 100, message = "Số lượng thu hồi mỗi lần tối đa 100 hộp")
        Integer quantity
) {
}
