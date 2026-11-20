package com.pokemon.blindbox.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateUserLockRequest(
        @NotNull(message = "Trạng thái khóa (locked) không được để trống")
        Boolean locked
) {
}
