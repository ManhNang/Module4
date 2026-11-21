package com.pokemon.blindbox.controller;

import com.pokemon.blindbox.dto.request.BulkSellToShopRequest;
import com.pokemon.blindbox.security.AuthenticatedUserProvider;
import com.pokemon.blindbox.service.InventoryBuybackService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Tag(name = "6. Inventory Buyback", description = "API bán thẻ trực tiếp cho Cửa hàng (NPC Buyback)")
public class InventoryBuybackController {

    private final InventoryBuybackService inventoryBuybackService;
    private final AuthenticatedUserProvider authUserProvider;

    @PostMapping("/{userCardId}/sell-to-shop")
    public ResponseEntity<Map<String, Object>> sellToShop(@PathVariable Long userCardId) {
        Long userId = authUserProvider.getRequiredUserId();
        return ResponseEntity.ok(inventoryBuybackService.sellCardToShop(userId, userCardId));
    }

    @PostMapping("/sell-to-shop/bulk")
    public ResponseEntity<Map<String, Object>> sellBulkToShop(@Valid @RequestBody BulkSellToShopRequest request) {
        Long userId = authUserProvider.getRequiredUserId();
        return ResponseEntity.ok(inventoryBuybackService.sellCardsBulkToShop(userId, request));
    }
}
