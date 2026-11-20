package com.pokemon.blindbox.dto.request;

import com.pokemon.blindbox.entity.enums.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
        @NotNull(message = "Quyền người dùng (role) không được để trống")
        Role role
) {
}
