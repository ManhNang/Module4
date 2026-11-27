package com.pokemon.blindbox.dto.request;

import jakarta.validation.constraints.NotNull;

public record UnsealBoxRequest(
        @NotNull(message = "Vui lòng chọn Bộ sưu tập (setId) cần bóc Box!")
        Long setId
) {}
