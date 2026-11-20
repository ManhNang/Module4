package com.pokemon.blindbox.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record PurchaseSinglePackRequest(
                Long userId,

                @NotNull(message = "Vị trí gói bài trong hộp (positionInBox) không được để trống") @Min(value = 1, message = "Vị trí gói bài phải >= 1") Integer positionInBox) {
}