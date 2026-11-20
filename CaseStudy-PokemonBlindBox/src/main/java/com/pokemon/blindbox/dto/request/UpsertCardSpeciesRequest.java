package com.pokemon.blindbox.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UpsertCardSpeciesRequest(
        @NotBlank(message = "Tên loài không được để trống")
        @Size(max = 150, message = "Tên loài tối đa 150 ký tự")
        String name,

        Boolean isBeta
) {
}
