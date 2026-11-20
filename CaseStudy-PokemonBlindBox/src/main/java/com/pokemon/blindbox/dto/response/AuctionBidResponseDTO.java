package com.pokemon.blindbox.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record AuctionBidResponseDTO(
        Long bidId,
        Long auctionId,
        Long bidderId,
        String maskedBidderName,
        BigDecimal bidAmount,
        LocalDateTime bidTimestamp,
        LocalDateTime auctionEndTime
) {
}
