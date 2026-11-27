package com.pokemon.blindbox.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StoreBuyRequest(
        @NotNull(message = "Vui lòng chọn Bộ sưu tập (setId)!")
        Long setId,
        @Min(value = 1, message = "Số lượng mua tối thiểu là 1!")
        @Max(value = 20, message = "Số lượng mua tối đa mỗi lượt là 20!")
        Integer quantity
) {}
