package com.pokemon.blindbox.dto.response;

import com.pokemon.blindbox.entity.Auction;
import com.pokemon.blindbox.entity.UserSealedItem;
import com.pokemon.blindbox.entity.enums.AuctionStatus;
import com.pokemon.blindbox.entity.enums.MarketItemType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record AuctionResponseDTO(
        Long id,
        Long sellerId,
        MarketItemType itemType,
        Long userCardId,
        Long sealedItemId,
        Long sealedSetId,
        String sealedSetName,
        String sealedSetCode,
        String sealedPackImageUrl,
        String sealedBoxImageUrl,
        Integer sealedCardsPerPack,
        Integer sealedPacksPerBox,
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
        MarketItemType type = auction.getItemType() != null ? auction.getItemType() : MarketItemType.CARD;
        UserSealedItem sealed = auction.getSealedItem();
        CardTemplateResponseDTO cardDto = (auction.getUserCard() != null && auction.getUserCard().getCardTemplate() != null)
                ? CardTemplateResponseDTO.fromEntity(auction.getUserCard().getCardTemplate())
                : null;

        return AuctionResponseDTO.builder()
                .id(auction.getId())
                .sellerId(auction.getSellerId())
                .itemType(type)
                .userCardId(auction.getUserCard() != null ? auction.getUserCard().getId() : null)
                .sealedItemId(sealed != null ? sealed.getId() : null)
                .sealedSetId(sealed != null && sealed.getCardSet() != null ? sealed.getCardSet().getId() : null)
                .sealedSetName(sealed != null && sealed.getCardSet() != null ? sealed.getCardSet().getName() : null)
                .sealedSetCode(sealed != null && sealed.getCardSet() != null ? sealed.getCardSet().getCode() : null)
                .sealedPackImageUrl(sealed != null && sealed.getCardSet() != null ? sealed.getCardSet().getPackImageUrl() : null)
                .sealedBoxImageUrl(sealed != null && sealed.getCardSet() != null ? sealed.getCardSet().getBoxImageUrl() : null)
                .sealedCardsPerPack(sealed != null ? sealed.getCardsPerPack() : null)
                .sealedPacksPerBox(sealed != null ? sealed.getPacksPerBox() : null)
                .startingPrice(auction.getStartingPrice())
                .minBidIncrement(auction.getMinBidIncrement())
                .currentHighestBid(auction.getCurrentHighestBid())
                .highestBidderId(auction.getHighestBidderId())
                .startTime(auction.getStartTime())
                .endTime(auction.getEndTime())
                .status(auction.getStatus())
                .card(cardDto)
                .build();
    }
}
