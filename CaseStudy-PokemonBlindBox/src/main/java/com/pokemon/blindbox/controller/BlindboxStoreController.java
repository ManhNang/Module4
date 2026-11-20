package com.pokemon.blindbox.controller;

import com.pokemon.blindbox.dto.request.PurchaseSinglePackRequest;
import com.pokemon.blindbox.dto.request.PurchaseWholeBoxRequest;
import com.pokemon.blindbox.dto.response.BoxPurchaseResponseDTO;
import com.pokemon.blindbox.dto.response.OpenPackResultResponseDTO;
import com.pokemon.blindbox.security.AuthenticatedUserProvider;
import com.pokemon.blindbox.service.BoxPurchaseService;
import com.pokemon.blindbox.service.GachaEngineService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "2. Store/Gacha", description = "API Mua Pack lẻ, Mua nguyên Box (@Version Optimistic Locking) & Mở gói bài Gacha (Weighted Random)")
public class BlindboxStoreController {

    private final BoxPurchaseService boxPurchaseService;
    private final GachaEngineService gachaEngineService;
    private final AuthenticatedUserProvider authUserProvider;

    @PostMapping("/boxes/{boxId}/packs/buy")
    public ResponseEntity<BoxPurchaseResponseDTO> buySinglePack(
            @PathVariable Long boxId,
            @Valid @RequestBody PurchaseSinglePackRequest request
    ) {
        Long currentUserId = authUserProvider.resolveCurrentUserId(request.userId());
        BoxPurchaseResponseDTO result = boxPurchaseService.buySinglePack(
                currentUserId,
                boxId,
                request.positionInBox()
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/boxes/{boxId}/buy-whole")
    public ResponseEntity<BoxPurchaseResponseDTO> buyWholeBox(
            @PathVariable Long boxId,
            @RequestBody(required = false) PurchaseWholeBoxRequest request
    ) {
        Long fallbackId = request != null ? request.userId() : null;
        Long currentUserId = authUserProvider.resolveCurrentUserId(fallbackId);
        BoxPurchaseResponseDTO result = boxPurchaseService.buyWholeBox(currentUserId, boxId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/packs/{packId}/open")
    public ResponseEntity<OpenPackResultResponseDTO> openPack(
            @PathVariable Long packId
    ) {
        Long currentUserId = authUserProvider.getRequiredUserId();
        OpenPackResultResponseDTO result = gachaEngineService.openPack(currentUserId, packId);
        return ResponseEntity.ok(result);
    }
}
