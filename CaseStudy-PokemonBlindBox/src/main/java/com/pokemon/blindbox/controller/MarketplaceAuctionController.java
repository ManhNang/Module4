package com.pokemon.blindbox.controller;

import com.pokemon.blindbox.dto.request.BuyMarketListingRequest;
import com.pokemon.blindbox.dto.request.CreateAuctionRequest;
import com.pokemon.blindbox.dto.request.CreateMarketListingRequest;
import com.pokemon.blindbox.dto.request.PlaceAuctionBidRequest;
import com.pokemon.blindbox.dto.response.AuctionBidResponseDTO;
import com.pokemon.blindbox.dto.response.AuctionResponseDTO;
import com.pokemon.blindbox.dto.response.MarketListingResponseDTO;
import com.pokemon.blindbox.dto.response.MarketplaceCrossLookupResponseDTO;
import com.pokemon.blindbox.security.AuthenticatedUserProvider;
import com.pokemon.blindbox.service.AuctionService;
import com.pokemon.blindbox.service.MarketplaceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
@Tag(name = "4. Marketplace/Auction", description = "API Sàn Giao Dịch P2P Giá cố định, Đấu Giá Thời Gian Thực (@Version) & Tra cứu chéo theo Đặt / Loài")
public class MarketplaceAuctionController {

    private final MarketplaceService marketplaceService;
    private final AuctionService auctionService;
    private final AuthenticatedUserProvider authUserProvider;

    @PostMapping("/listings")
    public ResponseEntity<MarketListingResponseDTO> createFixedPriceListing(
            @Valid @RequestBody CreateMarketListingRequest request
    ) {
        Long currentUserId = authUserProvider.resolveCurrentUserId(request.sellerId());
        CreateMarketListingRequest resolved = CreateMarketListingRequest.builder()
                .sellerId(currentUserId)
                .userCardId(request.userCardId())
                .price(request.price())
                .build();
        MarketListingResponseDTO created = marketplaceService.createFixedPriceListing(resolved);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/listings/{listingId}/buy")
    public ResponseEntity<MarketListingResponseDTO> buyFixedPriceListing(
            @PathVariable Long listingId,
            @RequestBody(required = false) BuyMarketListingRequest request
    ) {
        Long fallbackId = request != null ? request.buyerId() : null;
        Long currentUserId = authUserProvider.resolveCurrentUserId(fallbackId);
        BuyMarketListingRequest resolved = BuyMarketListingRequest.builder()
                .buyerId(currentUserId)
                .build();
        return ResponseEntity.ok(marketplaceService.buyFixedPriceListing(listingId, resolved));
    }

    @PostMapping("/auctions")
    public ResponseEntity<AuctionResponseDTO> createAuction(
            @Valid @RequestBody CreateAuctionRequest request
    ) {
        Long currentUserId = authUserProvider.resolveCurrentUserId(request.sellerId());
        CreateAuctionRequest resolved = CreateAuctionRequest.builder()
                .sellerId(currentUserId)
                .userCardId(request.userCardId())
                .startingPrice(request.startingPrice())
                .minBidIncrement(request.minBidIncrement())
                .durationMinutes(request.durationMinutes())
                .build();
        AuctionResponseDTO created = auctionService.createAuction(resolved);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/auctions/{auctionId}/bids")
    public ResponseEntity<AuctionBidResponseDTO> placeBid(
            @PathVariable Long auctionId,
            @Valid @RequestBody PlaceAuctionBidRequest request
    ) {
        Long currentUserId = authUserProvider.getRequiredUserId();
        // P3-03: Luôn lấy tên hiển thị từ tài khoản đang đăng nhập, không cho phép client giả mạo
        String displayName = authUserProvider.getCurrentUsername();
        PlaceAuctionBidRequest resolved = PlaceAuctionBidRequest.builder()
                .bidderId(currentUserId)
                .bidderDisplayName(displayName)
                .bidAmount(request.bidAmount())
                .build();
        return ResponseEntity.ok(auctionService.placeBid(auctionId, resolved));
    }

    @GetMapping("/lookup/by-set")
    public ResponseEntity<MarketplaceCrossLookupResponseDTO> lookupByCardSet(
            @RequestParam(required = false) Long setId,
            @RequestParam(required = false) String setCode,
            @RequestParam(required = false) String setName
    ) {
        return ResponseEntity.ok(marketplaceService.lookupMarketByCardSet(setId, setCode, setName));
    }

    @GetMapping("/lookup/by-species")
    public ResponseEntity<MarketplaceCrossLookupResponseDTO> lookupBySpecies(
            @RequestParam(required = false) Long speciesId,
            @RequestParam(required = false) String speciesName
    ) {
        return ResponseEntity.ok(marketplaceService.lookupMarketBySpecies(speciesId, speciesName));
    }
}
