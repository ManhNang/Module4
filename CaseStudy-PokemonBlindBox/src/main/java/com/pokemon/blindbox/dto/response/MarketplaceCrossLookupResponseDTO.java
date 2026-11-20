package com.pokemon.blindbox.dto.response;

import lombok.Builder;

import java.util.List;

/**
 * Phản hồi API Tra cứu chéo trên Sàn P2P & Đấu Giá khi người dùng bấm vào
 * liên kết Đặt ("Delta Reign") hoặc Loài ("Hầm hào huyền thoại").
 */
@Builder
public record MarketplaceCrossLookupResponseDTO(
        String filterType,
        String filterValue,
        int totalActiveItems,
        List<MarketListingResponseDTO> fixedPriceListings,
        List<AuctionResponseDTO> activeAuctions
) {
}
