package com.pokemon.blindbox.controller;

import com.pokemon.blindbox.security.AuthenticatedUserProvider;
import com.pokemon.blindbox.service.SealedStoreVaultService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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

    public record StoreBuyRequest(
            @NotNull(message = "Vui lòng chọn Bộ sưu tập (setId)!")
            Long setId,
            @Min(value = 1, message = "Số lượng mua tối thiểu là 1!")
            @Max(value = 20, message = "Số lượng mua tối đa mỗi lượt là 20!")
            Integer quantity
    ) {}

    public record UnsealBoxRequest(
            @NotNull(message = "Vui lòng chọn Bộ sưu tập (setId) cần bóc Box!")
            Long setId
    ) {}

    public record OpenSealedPacksRequest(
            @NotNull(message = "Vui lòng chọn Bộ sưu tập (setId) cần mở Pack!")
            Long setId,
            @Min(value = 1, message = "Số lượng Pack mở tối thiểu là 1!")
            @Max(value = 5, message = "Số lượng Pack mở tối đa mỗi lượt là 5!")
            Integer count
    ) {}

    public record UpdateDailyLimitsRequest(
            @Min(value = 0, message = "Hạn mức Pack mỗi ngày không được âm!")
            @Max(value = 100000, message = "Hạn mức Pack mỗi ngày không vượt quá 100.000!")
            Integer dailyPackLimit,
            @Min(value = 0, message = "Hạn mức Box mỗi ngày không được âm!")
            @Max(value = 10000, message = "Hạn mức Box mỗi ngày không vượt quá 10.000!")
            Integer dailyBoxLimit
    ) {}

    @PostMapping("/api/v1/store/buy-pack")
    public ResponseEntity<Map<String, Object>> buyPack(@Valid @RequestBody StoreBuyRequest req) {
        Long userId = authUserProvider.getRequiredUserId();
        int qty = req.quantity() != null ? req.quantity() : 1;
        return ResponseEntity.ok(sealedStoreVaultService.buyPacksFromStore(userId, req.setId(), qty));
    }

    @PostMapping("/api/v1/store/buy-box")
    public ResponseEntity<Map<String, Object>> buyBox(@Valid @RequestBody StoreBuyRequest req) {
        Long userId = authUserProvider.getRequiredUserId();
        int qty = req.quantity() != null ? req.quantity() : 1;
        return ResponseEntity.ok(sealedStoreVaultService.buyBoxesFromStore(userId, req.setId(), qty));
    }

    @PostMapping("/api/v1/inventory/sealed/unseal-box")
    public ResponseEntity<Map<String, Object>> unsealBox(@Valid @RequestBody UnsealBoxRequest req) {
        Long userId = authUserProvider.getRequiredUserId();
        return ResponseEntity.ok(sealedStoreVaultService.unsealBox(userId, req.setId()));
    }

    @PostMapping("/api/v1/inventory/sealed/open-packs")
    public ResponseEntity<Map<String, Object>> openPacks(@Valid @RequestBody OpenSealedPacksRequest req) {
        Long userId = authUserProvider.getRequiredUserId();
        int count = req.count() != null ? req.count() : 1;
        return ResponseEntity.ok(sealedStoreVaultService.openSealedPacks(userId, req.setId(), count));
    }

    @PatchMapping("/api/v1/admin/card-sets/{setId}/daily-limits")
    public ResponseEntity<Map<String, Object>> updateDailyLimits(
            @PathVariable Long setId,
            @Valid @RequestBody UpdateDailyLimitsRequest req
    ) {
        return ResponseEntity.ok(sealedStoreVaultService.updateDailyLimits(setId, req.dailyPackLimit(), req.dailyBoxLimit()));
    }

    @PostMapping("/api/v1/admin/card-sets/{setId}/reset-daily-sales")
    public ResponseEntity<Map<String, Object>> resetDailySales(@PathVariable Long setId) {
        return ResponseEntity.ok(sealedStoreVaultService.resetDailySales(setId));
    }
}
