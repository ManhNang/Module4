package com.pokemon.blindbox.dto.response;

import com.pokemon.blindbox.entity.AuctionBid;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO thay thế việc truyền trực tiếp Entity AuctionBid ra Thymeleaf View (P3-04).
 */
public record AuctionBidSummaryDTO(
        Long id,
        String maskedBidderName,
        BigDecimal bidAmount,
        LocalDateTime bidTimestamp
) {
    public static AuctionBidSummaryDTO fromEntity(AuctionBid bid) {
        return new AuctionBidSummaryDTO(
                bid.getId(),
                bid.getMaskedBidderName(),
                bid.getBidAmount(),
                bid.getBidTimestamp()
        );
    }
}
