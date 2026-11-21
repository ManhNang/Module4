package com.pokemon.blindbox.dto.request;

import jakarta.validation.constraints.NotNull;

public record PlaceSanctuaryCardRequest(
        @NotNull(message = "Vui lòng chọn thẻ bài để thả lên đảo")
        Long userCardId
) {
}
