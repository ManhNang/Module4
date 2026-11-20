package com.pokemon.blindbox.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record CreateBoosterBoxBatchRequest(
        @NotNull(message = "Số lượng hộp cần khởi tạo (quantity) không được để trống")
        @Min(value = 1, message = "Số lượng hộp khởi tạo tối thiểu là 1")
        @Max(value = 100, message = "Số lượng hộp khởi tạo mỗi lô tối đa là 100")
        Integer quantity
) {
}
