package com.pokemon.blindbox.dto.request;

import lombok.Builder;

/**
 * Bộ tiêu chí lọc thẻ bài động trên Pokedex & Sổ Sưu Tập (Digital Binder).
 * Hỗ trợ lọc kết hợp theo Tên thẻ, Số thứ tự (cardNumber), Độ Hiếm (ID hoặc tên như "Không phổ biến"),
 * Bộ sưu tập/Đặt (ID, mã "DLR" hoặc tên "Delta Reign"), và Loài (ID hoặc tên như "Hầm hào huyền thoại").
 */
@Builder
public record CardTemplateFilterRequest(
        String name,
        Integer cardNumber,
        Long rarityId,
        String rarityName,
        Long setId,
        String setCode,
        String setName,
        Long speciesId,
        String speciesName,
        Boolean secretRareOnly
) {
}
