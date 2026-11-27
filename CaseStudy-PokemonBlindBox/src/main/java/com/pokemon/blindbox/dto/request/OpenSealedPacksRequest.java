package com.pokemon.blindbox.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OpenSealedPacksRequest(
        @NotNull(message = "Vui lòng chọn Bộ sưu tập (setId) cần mở Pack!")
        Long setId,
        @Min(value = 1, message = "Số lượng Pack mở tối thiểu là 1!")
        @Max(value = 5, message = "Số lượng Pack mở tối đa mỗi lượt là 5!")
        Integer count
) {}
