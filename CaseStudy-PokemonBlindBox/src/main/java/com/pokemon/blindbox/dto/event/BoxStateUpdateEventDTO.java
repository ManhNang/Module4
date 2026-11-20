package com.pokemon.blindbox.dto.event;

import lombok.Builder;

/**
 * Sự kiện WebSocket gửi tới kênh /topic/boxes/{boxId}
 * ngay sau khi giao dịch mua Pack lẻ hoặc nguyên Box commit thành công.
 */
@Builder
public record BoxStateUpdateEventDTO(
        Long boxId,
        Integer soldPackPosition,
        Integer remainingPacks,
        boolean canBuyWholeBox
) {
}
