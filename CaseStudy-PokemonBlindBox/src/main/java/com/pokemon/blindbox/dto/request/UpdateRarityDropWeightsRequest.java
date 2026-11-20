package com.pokemon.blindbox.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

@Builder
public record UpdateRarityDropWeightsRequest(
        @NotEmpty(message = "Danh sách cập nhật trọng số độ hiếm không được rỗng")
        @Valid
        List<RarityWeightItem> items
) {
    @Builder
    public record RarityWeightItem(
            @NotNull(message = "rarityId không được để trống")
            Long rarityId,

            @NotNull(message = "dropWeight không được để trống")
            @Min(value = 0, message = "Trọng số rơi (dropWeight) phải >= 0")
            Integer dropWeight
    ) {
    }
}
