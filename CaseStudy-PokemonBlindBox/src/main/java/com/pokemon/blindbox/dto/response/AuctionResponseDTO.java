package com.pokemon.blindbox.dto.response;

import com.pokemon.blindbox.entity.Auction;
import com.pokemon.blindbox.entity.enums.AuctionStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record AuctionResponseDTO(
        Long id,
        Long sellerId,
        Long userCardId,
        BigDecimal startingPrice,
        BigDecimal minBidIncrement,
        BigDecimal currentHighestBid,
        Long highestBidderId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        AuctionStatus status,
        CardTemplateResponseDTO card
) {
    public static AuctionResponseDTO fromEntity(Auction auction) {
        return AuctionResponseDTO.builder()
                .id(auction.getId())
                .sellerId(auction.getSellerId())
                .userCardId(auction.getUserCard().getId())
                .startingPrice(auction.getStartingPrice())
                .minBidIncrement(auction.getMinBidIncrement())
                .currentHighestBid(auction.getCurrentHighestBid())
                .highestBidderId(auction.getHighestBidderId())
                .startTime(auction.getStartTime())
                .endTime(auction.getEndTime())
                .status(auction.getStatus())
                .card(CardTemplateResponseDTO.fromEntity(auction.getUserCard().getCardTemplate()))
                .build();
    }
}
