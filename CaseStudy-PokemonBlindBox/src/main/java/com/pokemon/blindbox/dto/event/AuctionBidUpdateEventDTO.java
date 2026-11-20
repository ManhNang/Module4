package com.pokemon.blindbox.dto.event;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Sự kiện WebSocket gửi tới kênh /topic/auctions/{auctionId}
 * ngay sau khi bước giá đấu giá mới hợp lệ được commit thành công.
 */
@Builder
public record AuctionBidUpdateEventDTO(
        Long auctionId,
        BigDecimal newHighestBid,
        String maskedBidderName,
        LocalDateTime bidTimestamp,
        LocalDateTime endTime
) {
}
