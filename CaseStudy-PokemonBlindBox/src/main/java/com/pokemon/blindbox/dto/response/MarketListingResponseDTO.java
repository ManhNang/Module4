package com.pokemon.blindbox.dto.response;

import com.pokemon.blindbox.entity.MarketListing;
import com.pokemon.blindbox.entity.UserSealedItem;
import com.pokemon.blindbox.entity.enums.ListingStatus;
import com.pokemon.blindbox.entity.enums.MarketItemType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record MarketListingResponseDTO(
        Long id,
        Long sellerId,
        MarketItemType itemType,
        Long userCardId,
        Long sealedItemId,
        Long sealedSetId,
        String sealedSetName,
        String sealedSetCode,
        Integer sealedCardsPerPack,
        Integer sealedPacksPerBox,
        BigDecimal price,
        ListingStatus status,
        Long buyerId,
        LocalDateTime soldAt,
        LocalDateTime createdAt,
        CardTemplateResponseDTO card
) {
    public static MarketListingResponseDTO fromEntity(MarketListing listing) {
        MarketItemType type = listing.getItemType() != null ? listing.getItemType() : MarketItemType.CARD;
        UserSealedItem sealed = listing.getSealedItem();
        CardTemplateResponseDTO cardDto = (listing.getUserCard() != null && listing.getUserCard().getCardTemplate() != null)
                ? CardTemplateResponseDTO.fromEntity(listing.getUserCard().getCardTemplate())
                : null;

        return MarketListingResponseDTO.builder()
                .id(listing.getId())
                .sellerId(listing.getSellerId())
                .itemType(type)
                .userCardId(listing.getUserCard() != null ? listing.getUserCard().getId() : null)
                .sealedItemId(sealed != null ? sealed.getId() : null)
                .sealedSetId(sealed != null && sealed.getCardSet() != null ? sealed.getCardSet().getId() : null)
                .sealedSetName(sealed != null && sealed.getCardSet() != null ? sealed.getCardSet().getName() : null)
                .sealedSetCode(sealed != null && sealed.getCardSet() != null ? sealed.getCardSet().getCode() : null)
                .sealedCardsPerPack(sealed != null ? sealed.getCardsPerPack() : null)
                .sealedPacksPerBox(sealed != null ? sealed.getPacksPerBox() : null)
                .price(listing.getPrice())
                .status(listing.getStatus())
                .buyerId(listing.getBuyerId())
                .soldAt(listing.getSoldAt())
                .createdAt(listing.getCreatedAt())
                .card(cardDto)
                .build();
    }
}
