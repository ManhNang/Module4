package com.pokemon.blindbox.controller;

import com.pokemon.blindbox.security.AuthenticatedUserProvider;
import com.pokemon.blindbox.service.SealedStoreVaultService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "6. Store & Sealed Vault", description = "API Cửa hàng đa Bộ sưu tập (Pack/Box Daily Stock) & Kho chứa Pack/Box")
public class SealedStoreVaultController {

    private final SealedStoreVaultService sealedStoreVaultService;
    private final AuthenticatedUserProvider authUserProvider;

    public record StoreBuyRequest(Long setId, Integer quantity) {}
    public record UnsealBoxRequest(Long setId) {}
    public record OpenSealedPacksRequest(Long setId, Integer count) {}
    public record UpdateDailyLimitsRequest(Integer dailyPackLimit, Integer dailyBoxLimit) {}

    @PostMapping("/api/v1/store/buy-pack")
    public ResponseEntity<Map<String, Object>> buyPack(@RequestBody StoreBuyRequest req) {
        Long userId = authUserProvider.getRequiredUserId();
        int qty = req.quantity() != null ? req.quantity() : 1;
        return ResponseEntity.ok(sealedStoreVaultService.buyPacksFromStore(userId, req.setId(), qty));
    }

    @PostMapping("/api/v1/store/buy-box")
    public ResponseEntity<Map<String, Object>> buyBox(@RequestBody StoreBuyRequest req) {
        Long userId = authUserProvider.getRequiredUserId();
        int qty = req.quantity() != null ? req.quantity() : 1;
        return ResponseEntity.ok(sealedStoreVaultService.buyBoxesFromStore(userId, req.setId(), qty));
    }

    @PostMapping("/api/v1/inventory/sealed/unseal-box")
    public ResponseEntity<Map<String, Object>> unsealBox(@RequestBody UnsealBoxRequest req) {
        Long userId = authUserProvider.getRequiredUserId();
        return ResponseEntity.ok(sealedStoreVaultService.unsealBox(userId, req.setId()));
    }

    @PostMapping("/api/v1/inventory/sealed/open-packs")
    public ResponseEntity<Map<String, Object>> openPacks(@RequestBody OpenSealedPacksRequest req) {
        Long userId = authUserProvider.getRequiredUserId();
        int count = req.count() != null ? req.count() : 1;
        return ResponseEntity.ok(sealedStoreVaultService.openSealedPacks(userId, req.setId(), count));
    }

    @PatchMapping("/api/v1/admin/card-sets/{setId}/daily-limits")
    public ResponseEntity<Map<String, Object>> updateDailyLimits(
            @PathVariable Long setId,
            @RequestBody UpdateDailyLimitsRequest req
    ) {
        return ResponseEntity.ok(sealedStoreVaultService.updateDailyLimits(setId, req.dailyPackLimit(), req.dailyBoxLimit()));
    }

    @PostMapping("/api/v1/admin/card-sets/{setId}/reset-daily-sales")
    public ResponseEntity<Map<String, Object>> resetDailySales(@PathVariable Long setId) {
        return ResponseEntity.ok(sealedStoreVaultService.resetDailySales(setId));
    }
}
