package com.pokemon.blindbox.dto.request;

import lombok.Builder;

@Builder
public record BuyMarketListingRequest(
                Long buyerId) {
}