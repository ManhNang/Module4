package com.pokemon.blindbox.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record StartExpeditionRequest(
        @NotBlank(message = "Vui lòng chọn vùng đất thám hiểm")
        String zoneCode,

        @NotEmpty(message = "Cần chọn ít nhất 1 thẻ bài để đi thám hiểm")
        @Size(max = 3, message = "Mỗi chuyến thám hiểm tối đa 3 thẻ bài")
        List<Long> userCardIds
) {
}
