package com.pokemon.blindbox.dto.response;

import com.pokemon.blindbox.entity.MarketListing;
import com.pokemon.blindbox.entity.enums.ListingStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record MarketListingResponseDTO(
        Long id,
        Long sellerId,
        Long userCardId,
        BigDecimal price,
        ListingStatus status,
        Long buyerId,
        LocalDateTime soldAt,
        LocalDateTime createdAt,
        CardTemplateResponseDTO card
) {
    public static MarketListingResponseDTO fromEntity(MarketListing listing) {
        return MarketListingResponseDTO.builder()
                .id(listing.getId())
                .sellerId(listing.getSellerId())
                .userCardId(listing.getUserCard().getId())
                .price(listing.getPrice())
                .status(listing.getStatus())
                .buyerId(listing.getBuyerId())
                .soldAt(listing.getSoldAt())
                .createdAt(listing.getCreatedAt())
                .card(CardTemplateResponseDTO.fromEntity(listing.getUserCard().getCardTemplate()))
                .build();
    }
}
