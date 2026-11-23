package com.pokemon.blindbox.controller.admin;

import com.pokemon.blindbox.dto.request.*;
import com.pokemon.blindbox.dto.response.*;
import com.pokemon.blindbox.service.AdminCardSetService;
import com.pokemon.blindbox.service.CatalogCacheService;
import com.pokemon.blindbox.service.CloudinaryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * REST Controller cho phân hệ Admin Dashboard quản lý Bộ thẻ (CardSet),
 * Khuôn mẫu thẻ (CardTemplate), Trọng số Độ hiếm (Rarity dropWeight)
 * và Khởi tạo lô Hộp bài (BoosterBox Batch).
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "1. Admin", description = "API Quản trị Bộ thẻ (CardSet), Khuôn mẫu thẻ (CardTemplate), Trọng số Độ hiếm (Rarity) & Lô Hộp bài (BoosterBox)")
public class AdminCardSetController {

    private final AdminCardSetService adminCardSetService;
    private final CatalogCacheService catalogCacheService;
    private final CloudinaryService cloudinaryService;

    /**
     * 1. Upload độc lập ảnh thẻ bài 2D lên Cloudinary và trả về imageUrl.
     * POST /api/v1/admin/cards/upload-image
     */
    @PostMapping(value = "/cards/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageUploadResponseDTO> uploadCardImage(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "setCode", required = false, defaultValue = "DLR") String setCode,
            @RequestParam(value = "cardNumber", required = false) Integer cardNumber
    ) {
        String imageUrl = cloudinaryService.uploadCardImage(file, setCode, cardNumber);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ImageUploadResponseDTO.builder().imageUrl(imageUrl).build());
    }

    /**
     * 1B. Upload ảnh PNG/WebP tùy chỉnh cho Pack hoặc Box của Bộ Thẻ (≤ 2MB).
     * POST /api/v1/admin/card-sets/upload-media
     */
    @PostMapping(value = "/card-sets/upload-media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageUploadResponseDTO> uploadCardSetMedia(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "setCode", required = false, defaultValue = "set") String setCode,
            @RequestParam(value = "mediaType", required = false, defaultValue = "pack") String mediaType
    ) {
        String imageUrl = adminCardSetService.uploadSetMediaImage(file, setCode, mediaType);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ImageUploadResponseDTO.builder().imageUrl(imageUrl).build());
    }

    /**
     * 2. Thêm mới CardTemplate bằng JSON (khi đã có imageUrl từ bước upload ảnh).
     * Nếu trùng (setId, cardNumber) -> trả về HTTP 409 CONFLICT.
     * POST /api/v1/admin/card-sets/{setId}/cards
     */
    @PostMapping(value = "/card-sets/{setId}/cards", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CardTemplateResponseDTO> createCardTemplateJson(
            @PathVariable Long setId,
            @Valid @RequestBody CreateCardTemplateRequest request
    ) {
        CardTemplateResponseDTO created = adminCardSetService.createCardTemplate(setId, request, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * 3. Thêm mới CardTemplate bằng Multipart (gửi kèm cả metadata và file ảnh 2D trong 1 request).
     * Nếu trùng (setId, cardNumber) -> trả về HTTP 409 CONFLICT.
     * POST /api/v1/admin/card-sets/{setId}/cards (multipart/form-data)
     */
    @PostMapping(value = "/card-sets/{setId}/cards", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CardTemplateResponseDTO> createCardTemplateMultipart(
            @PathVariable Long setId,
            @Valid @RequestPart("data") CreateCardTemplateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile imageFile
    ) {
        CardTemplateResponseDTO created = adminCardSetService.createCardTemplate(setId, request, imageFile);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * 4. Cập nhật động cấu hình cardsPerPack, packsPerBox (và giá) của CardSet.
     * PATCH /api/v1/admin/card-sets/{setId}/config
     */
    @PatchMapping("/card-sets/{setId}/config")
    public ResponseEntity<CardSetResponseDTO> updateCardSetConfig(
            @PathVariable Long setId,
            @Valid @RequestBody UpdateCardSetConfigRequest request
    ) {
        CardSetResponseDTO updated = adminCardSetService.updateCardSetConfig(setId, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * 5. Cập nhật động trọng số rơi (dropWeight) của từng Rarity.
     * PUT /api/v1/admin/rarities/drop-weights
     */
    @PutMapping("/rarities/drop-weights")
    public ResponseEntity<List<RarityResponseDTO>> updateRarityDropWeights(
            @Valid @RequestBody UpdateRarityDropWeightsRequest request
    ) {
        List<RarityResponseDTO> updatedRarities = adminCardSetService.updateRarityDropWeights(request);
        return ResponseEntity.ok(updatedRarities);
    }

    /**
     * 6. Khởi tạo lô BoosterBox mới cho CardSet.
     * Tự động đóng băng giá trị cardsPerPack hiện tại của CardSet vào snapshotCardsPerPack của từng BoosterPack con.
     * POST /api/v1/admin/card-sets/{setId}/booster-boxes/batch
     */
    @PostMapping("/card-sets/{setId}/booster-boxes/batch")
    public ResponseEntity<List<BoosterBoxSummaryResponseDTO>> createBoosterBoxBatch(
            @PathVariable Long setId,
            @Valid @RequestBody CreateBoosterBoxBatchRequest request
    ) {
        List<BoosterBoxSummaryResponseDTO> createdBoxes = adminCardSetService.createBoosterBoxBatch(setId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBoxes);
    }

    /**
     * 7. Các Endpoint đọc danh mục từ Redis Cache (phục vụ kiểm tra nhanh trên Admin Dashboard).
     */
    @GetMapping("/card-sets/{setId}")
    public ResponseEntity<CardSetResponseDTO> getCardSetById(@PathVariable Long setId) {
        return ResponseEntity.ok(catalogCacheService.getCardSetById(setId));
    }

    @GetMapping("/rarities")
    public ResponseEntity<List<RarityResponseDTO>> getAllRarities() {
        return ResponseEntity.ok(catalogCacheService.getAllRaritiesWithWeights());
    }

    @GetMapping("/card-sets/{setId}/rarities/{rarityId}/cards")
    public ResponseEntity<List<CardTemplateResponseDTO>> getCardsBySetAndRarity(
            @PathVariable Long setId,
            @PathVariable Long rarityId
    ) {
        return ResponseEntity.ok(catalogCacheService.getCardTemplatesBySetAndRarity(setId, rarityId));
    }

    // ==================== 8. QUẢN LÝ ĐA BỘ SƯU TẬP (CARD SET CRUD) ====================

    @PostMapping("/card-sets")
    public ResponseEntity<CardSetResponseDTO> createCardSet(@Valid @RequestBody CreateCardSetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminCardSetService.createCardSet(request));
    }

    @PutMapping("/card-sets/{setId}")
    public ResponseEntity<CardSetResponseDTO> updateCardSet(
            @PathVariable Long setId,
            @Valid @RequestBody UpdateCardSetRequest request
    ) {
        return ResponseEntity.ok(adminCardSetService.updateCardSet(setId, request));
    }

    @DeleteMapping("/card-sets/{setId}")
    public ResponseEntity<Map<String, Object>> deleteCardSet(@PathVariable Long setId) {
        return ResponseEntity.ok(adminCardSetService.deleteCardSet(setId));
    }

    @PatchMapping("/card-sets/{setId}/active")
    public ResponseEntity<CardSetResponseDTO> toggleCardSetActive(
            @PathVariable Long setId,
            @RequestBody Map<String, Boolean> body
    ) {
        boolean active = Boolean.TRUE.equals(body.get("active"));
        return ResponseEntity.ok(adminCardSetService.toggleCardSetActive(setId, active));
    }

    // ==================== 9. THU HỒI / XÓA BOOSTER BOX NGUYÊN SEAL ====================

    @DeleteMapping("/booster-boxes/{boxId}")
    public ResponseEntity<Map<String, Object>> deleteSealedBoosterBox(@PathVariable Long boxId) {
        return ResponseEntity.ok(adminCardSetService.deleteSealedBoosterBox(boxId));
    }

    @PostMapping("/card-sets/{setId}/booster-boxes/reduce")
    public ResponseEntity<Map<String, Object>> reduceSealedBoosterBoxes(
            @PathVariable Long setId,
            @Valid @RequestBody ReduceBoosterBoxesRequest request
    ) {
        return ResponseEntity.ok(adminCardSetService.reduceSealedBoosterBoxes(setId, request));
    }

    // ==================== 10. CẬP NHẬT & XÓA THẺ BÀI (CARD TEMPLATE UPDATE/DELETE) ====================

    @PutMapping(value = "/card-sets/{setId}/cards/{cardId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CardTemplateResponseDTO> updateCardTemplateJson(
            @PathVariable Long setId,
            @PathVariable Long cardId,
            @Valid @RequestBody CreateCardTemplateRequest request
    ) {
        return ResponseEntity.ok(adminCardSetService.updateCardTemplate(setId, cardId, request, null));
    }

    @PutMapping(value = "/card-sets/{setId}/cards/{cardId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CardTemplateResponseDTO> updateCardTemplateMultipart(
            @PathVariable Long setId,
            @PathVariable Long cardId,
            @Valid @RequestPart("data") CreateCardTemplateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile imageFile
    ) {
        return ResponseEntity.ok(adminCardSetService.updateCardTemplate(setId, cardId, request, imageFile));
    }

    @DeleteMapping("/card-sets/{setId}/cards/{cardId}")
    public ResponseEntity<Map<String, Object>> deleteCardTemplate(
            @PathVariable Long setId,
            @PathVariable Long cardId
    ) {
        return ResponseEntity.ok(adminCardSetService.deleteCardTemplate(setId, cardId));
    }

    @PatchMapping("/card-sets/{setId}/cards/{cardId}/active")
    public ResponseEntity<CardTemplateResponseDTO> toggleCardTemplateActive(
            @PathVariable Long setId,
            @PathVariable Long cardId,
            @RequestBody Map<String, Boolean> body
    ) {
        boolean active = Boolean.TRUE.equals(body.get("active"));
        return ResponseEntity.ok(adminCardSetService.toggleCardTemplateActive(setId, cardId, active));
    }

    // ==================== 11. CRUD ĐỘ HIẾM (RARITY) & LOÀI (CARD SPECIES) ====================

    @PostMapping("/rarities")
    public ResponseEntity<RarityResponseDTO> createRarity(@Valid @RequestBody UpsertRarityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminCardSetService.createRarity(request));
    }

    @PutMapping("/rarities/{id}")
    public ResponseEntity<RarityResponseDTO> updateRarity(
            @PathVariable Long id,
            @Valid @RequestBody UpsertRarityRequest request
    ) {
        return ResponseEntity.ok(adminCardSetService.updateRarity(id, request));
    }

    @DeleteMapping("/rarities/{id}")
    public ResponseEntity<Map<String, Object>> deleteRarity(@PathVariable Long id) {
        return ResponseEntity.ok(adminCardSetService.deleteRarity(id));
    }

    @GetMapping("/species")
    public ResponseEntity<List<CardSpeciesResponseDTO>> getAllSpecies() {
        return ResponseEntity.ok(catalogCacheService.getAllCardSpecies());
    }

    @PostMapping("/species")
    public ResponseEntity<CardSpeciesResponseDTO> createSpecies(@Valid @RequestBody UpsertCardSpeciesRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminCardSetService.createCardSpecies(request));
    }

    @PutMapping("/species/{id}")
    public ResponseEntity<CardSpeciesResponseDTO> updateSpecies(
            @PathVariable Long id,
            @Valid @RequestBody UpsertCardSpeciesRequest request
    ) {
        return ResponseEntity.ok(adminCardSetService.updateCardSpecies(id, request));
    }

    @DeleteMapping("/species/{id}")
    public ResponseEntity<Map<String, Object>> deleteSpecies(@PathVariable Long id) {
        return ResponseEntity.ok(adminCardSetService.deleteCardSpecies(id));
    }
}
