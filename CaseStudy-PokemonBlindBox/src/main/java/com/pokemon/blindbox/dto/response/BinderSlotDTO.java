package com.pokemon.blindbox.dto.response;

import lombok.Builder;

/**
 * Đại diện cho 1 ô (Slot) trong Sổ Sưu Tập (Digital Binder) từ 001..baseSetTotal (hoặc ô Secret Rare).
 * Cờ isOwned giúp Frontend quyết định hiển thị thẻ sáng rõ (isOwned = true) hay bóng mờ/silhouette (isOwned = false).
 */
@Builder
public record BinderSlotDTO(
        Integer cardNumber,
        String formattedCardNumber,
        boolean isOwned,
        long ownedQuantity,
        boolean secretRare,
        CardTemplateResponseDTO cardTemplate
) {
}
