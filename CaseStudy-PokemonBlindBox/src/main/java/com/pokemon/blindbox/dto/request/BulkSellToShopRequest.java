package com.pokemon.blindbox.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request bán nhanh hàng loạt (hoặc chọn số lượng x2+) thẻ bài cho Cửa hàng (NPC Buyback).
 */
public record BulkSellToShopRequest(
        @NotEmpty(message = "Danh sách thẻ cần bán không được để trống")
        @Valid
        List<Item> items
) {
    public record Item(
            @NotNull(message = "Thiếu mã khuôn mẫu thẻ bài")
            Long cardTemplateId,

            @NotNull(message = "Thiếu số lượng thẻ cần bán")
            @Min(value = 1, message = "Số lượng bán phải từ 1 trở lên")
            Integer quantity
    ) {}
}
