package com.pokemon.blindbox.service;

import com.pokemon.blindbox.config.RedisCacheConfig;
import com.pokemon.blindbox.dto.request.*;
import com.pokemon.blindbox.dto.response.*;
import com.pokemon.blindbox.entity.*;
import com.pokemon.blindbox.entity.enums.BoxStatus;
import com.pokemon.blindbox.entity.enums.PackStatus;
import com.pokemon.blindbox.entity.enums.RevealEffect;
import com.pokemon.blindbox.exception.BusinessValidationException;
import com.pokemon.blindbox.exception.DuplicateResourceException;
import com.pokemon.blindbox.exception.ResourceNotFoundException;
import com.pokemon.blindbox.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service xử lý nghiệp vụ Admin Dashboard:
 * 1. Quản lý đa Bộ sưu tập (CardSet CRUD + Soft Delete).
 * 2. Thêm/Sửa/Xóa (Hard/Soft Delete) CardTemplate (Upload Cloudinary ngoài TransactionTemplate).
 * 3. Quản lý Độ hiếm (Rarity CRUD + dropWeight + isHolographic) và Loài (CardSpecies CRUD).
 * 4. Phát hành & Thu hồi lô BoosterBox (PESSIMISTIC_WRITE, chỉ thu hồi Box nguyên seal).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminCardSetService {

    private final CardSetRepository cardSetRepository;
    private final RarityRepository rarityRepository;
    private final CardSpeciesRepository cardSpeciesRepository;
    private final CardTemplateRepository cardTemplateRepository;
    private final UserCardRepository userCardRepository;
    private final BoosterBoxRepository boosterBoxRepository;
    private final CloudinaryService cloudinaryService;
    private final TransactionTemplate transactionTemplate;

    // ==================== 1. QUẢN LÝ ĐA BỘ SƯU TẬP (CARD SET CRUD) ====================

    @Transactional
    @CacheEvict(value = {RedisCacheConfig.CACHE_CARD_SETS}, allEntries = true)
    public CardSetResponseDTO createCardSet(CreateCardSetRequest request) {
        String cleanCode = request.code().trim().toUpperCase();
        if (cardSetRepository.existsByCodeIgnoreCase(cleanCode)) {
            throw new DuplicateResourceException("Mã bộ sưu tập '" + cleanCode + "' đã tồn tại. Vui lòng chọn mã khác!");
        }
        CardSet saved = cardSetRepository.save(CardSet.builder()
                .name(request.name().trim())
                .code(cleanCode)
                .baseSetTotal(request.baseSetTotal())
                .cardsPerPack(request.cardsPerPack())
                .packsPerBox(request.packsPerBox())
                .packPrice(request.packPrice())
                .boxPrice(SealedStoreVaultService.computeDiscountedBoxPrice(request.packPrice(), request.packsPerBox()))
                .packImageUrl(normalizeNullableImageUrl(request.packImageUrl()))
                .boxImageUrl(normalizeNullableImageUrl(request.boxImageUrl()))
                .isActive(true)
                .build());
        log.info("Created new CardSet id={}, code={}, name={}", saved.getId(), saved.getCode(), saved.getName());
        return CardSetResponseDTO.fromEntity(saved);
    }

    @Transactional
    @CacheEvict(value = {RedisCacheConfig.CACHE_CARD_SETS, RedisCacheConfig.CACHE_CARD_TEMPLATES}, allEntries = true)
    public CardSetResponseDTO updateCardSet(Long setId, UpdateCardSetRequest request) {
        CardSet cardSet = cardSetRepository.findById(setId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Bộ thẻ với id = " + setId));

        String cleanCode = request.code().trim().toUpperCase();
        if (cardSetRepository.existsByCodeIgnoreCaseAndIdNot(cleanCode, setId)) {
            throw new DuplicateResourceException("Mã bộ sưu tập '" + cleanCode + "' đã thuộc về bộ khác!");
        }

        cardSet.setName(request.name().trim());
        cardSet.setCode(cleanCode);
        cardSet.setBaseSetTotal(request.baseSetTotal());
        cardSet.setCardsPerPack(request.cardsPerPack());
        cardSet.setPacksPerBox(request.packsPerBox());
        cardSet.setPackPrice(request.packPrice());
        cardSet.setBoxPrice(SealedStoreVaultService.computeDiscountedBoxPrice(request.packPrice(), request.packsPerBox()));
        if (request.isActive() != null) {
            cardSet.setIsActive(request.isActive());
        }
        if (request.packImageUrl() != null) {
            cardSet.setPackImageUrl(normalizeNullableImageUrl(request.packImageUrl()));
        }
        if (request.boxImageUrl() != null) {
            cardSet.setBoxImageUrl(normalizeNullableImageUrl(request.boxImageUrl()));
        }

        CardSet saved = cardSetRepository.save(cardSet);
        log.info("Updated CardSet id={}, code={}, isActive={}", saved.getId(), saved.getCode(), saved.getIsActive());
        return CardSetResponseDTO.fromEntity(saved);
    }

    /**
     * Upload ảnh PNG/WebP tùy chỉnh cho Pack hoặc Box của Bộ Thẻ (tối đa 2MB).
     * Lưu trực tiếp vào thư mục ./uploads/sets/ (phục vụ ngay qua WebMvcConfig tại /uploads/**).
     */
    public String uploadSetMediaImage(MultipartFile file, String setCode, String mediaType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessValidationException("Vui lòng chọn file ảnh PNG hoặc WebP!");
        }
        long maxBytes = 2L * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new BusinessValidationException("Dung lượng ảnh tối đa là 2MB!");
        }

        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : "";
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (java.io.IOException e) {
            throw new BusinessValidationException("Không thể đọc dữ liệu file ảnh tải lên!");
        }

        boolean isPng = bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47;
        boolean isWebp = bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';

        if (!((isPng && (contentType.isEmpty() || contentType.equals("image/png")))
                || (isWebp && (contentType.isEmpty() || contentType.equals("image/webp"))))) {
            throw new BusinessValidationException("Chỉ chấp nhận định dạng ảnh PNG (.png) hoặc WebP (.webp) hợp lệ!");
        }

        String ext = isPng ? ".png" : ".webp";
        String safeCode = (setCode != null ? setCode.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "") : "set");
        if (safeCode.isEmpty()) safeCode = "set";
        String safeKind = "box".equalsIgnoreCase(mediaType) ? "box" : "pack";
        String fileName = safeCode + "-" + safeKind + "-" + UUID.randomUUID().toString().substring(0, 8) + ext;

        try {
            java.nio.file.Path uploadDir = java.nio.file.Paths.get("uploads", "sets").toAbsolutePath().normalize();
            java.nio.file.Files.createDirectories(uploadDir);
            java.nio.file.Path targetFile = uploadDir.resolve(fileName);
            java.nio.file.Files.write(targetFile, bytes);

            // Sao chép thêm vào src/main/resources/static/uploads/sets (nếu chạy môi trường dev) để lưu bền vững
            java.nio.file.Path staticBase = java.nio.file.Paths.get("src", "main", "resources", "static");
            if (java.nio.file.Files.isDirectory(staticBase)) {
                java.nio.file.Path staticSetsDir = staticBase.resolve("uploads").resolve("sets");
                java.nio.file.Files.createDirectories(staticSetsDir);
                java.nio.file.Files.write(staticSetsDir.resolve(fileName), bytes);
            }
        } catch (java.io.IOException e) {
            log.error("Failed to save uploaded set media image {}", fileName, e);
            throw new BusinessValidationException("Không thể lưu file ảnh lên máy chủ: " + e.getMessage());
        }

        String publicUrl = "/uploads/sets/" + fileName;
        log.info("Uploaded custom {} image for set {}: {}", safeKind, safeCode, publicUrl);
        return publicUrl;
    }

    private String normalizeNullableImageUrl(String rawUrl) {
        if (rawUrl == null) return null;
        String trimmed = rawUrl.trim();
        if (trimmed.isEmpty()) return null;
        String lower = trimmed.toLowerCase();
        if (!(lower.startsWith("/uploads/") || lower.startsWith("/images/")
                || lower.startsWith("https://") || lower.startsWith("http://"))) {
            throw new BusinessValidationException("Đường dẫn ảnh phải bắt đầu bằng /uploads/, /images/, http:// hoặc https://");
        }
        return trimmed;
    }

    @Transactional
    @CacheEvict(value = {RedisCacheConfig.CACHE_CARD_SETS, RedisCacheConfig.CACHE_CARD_TEMPLATES}, allEntries = true)
    public Map<String, Object> deleteCardSet(Long setId) {
        CardSet cardSet = cardSetRepository.findById(setId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Bộ thẻ với id = " + setId));

        boolean hasReferences = boosterBoxRepository.existsByCardSetId(setId)
                || cardTemplateRepository.existsByCardSetId(setId);

        if (!hasReferences) {
            cardSetRepository.delete(cardSet);
            log.info("Hard deleted CardSet id={}, code={}", setId, cardSet.getCode());
            return Map.of(
                    "deleted", true,
                    "softDeleted", false,
                    "message", "Đã xóa vĩnh viễn bộ sưu tập '" + cardSet.getName() + "'."
            );
        }

        cardSet.setIsActive(false);
        cardSetRepository.save(cardSet);
        log.info("Soft deleted CardSet id={}, code={} (has existing boxes/templates)", setId, cardSet.getCode());
        return Map.of(
                "deleted", true,
                "softDeleted", true,
                "message", "Bộ sưu tập '" + cardSet.getName() + "' đã có dữ liệu hộp/thẻ nên đã được ẩn (Soft Delete)."
        );
    }

    @Transactional
    @CacheEvict(value = {RedisCacheConfig.CACHE_CARD_SETS, RedisCacheConfig.CACHE_CARD_TEMPLATES}, allEntries = true)
    public CardSetResponseDTO toggleCardSetActive(Long setId, boolean active) {
        CardSet cardSet = cardSetRepository.findById(setId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Bộ thẻ với id = " + setId));
        cardSet.setIsActive(active);
        CardSet saved = cardSetRepository.save(cardSet);
        log.info("Toggled CardSet id={}, code={}, isActive={}", saved.getId(), saved.getCode(), saved.getIsActive());
        return CardSetResponseDTO.fromEntity(saved);
    }

    @Transactional
    @CacheEvict(value = {RedisCacheConfig.CACHE_CARD_TEMPLATES, RedisCacheConfig.CACHE_CARD_SETS}, allEntries = true)
    public CardTemplateResponseDTO toggleCardTemplateActive(Long setId, Long cardId, boolean active) {
        CardTemplate card = cardTemplateRepository.findWithDetailsById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thẻ bài với id = " + cardId));
        if (!card.getCardSet().getId().equals(setId)) {
            throw new BusinessValidationException("Thẻ bài không thuộc bộ sưu tập đang chọn.");
        }
        card.setIsActive(active);
        CardTemplate saved = cardTemplateRepository.save(card);
        log.info("Toggled CardTemplate id={}, setId={}, isActive={}", saved.getId(), setId, saved.getIsActive());
        return CardTemplateResponseDTO.fromEntity(saved);
    }

    /**
     * Thêm mới CardTemplate vào một CardSet.
     * Upload ảnh lên Cloudinary thực thi NGOÀI @Transactional để không chiếm giữ HikariCP connection (P2-02).
     */
    @CacheEvict(
            value = {RedisCacheConfig.CACHE_CARD_TEMPLATES, RedisCacheConfig.CACHE_CARD_SETS},
            allEntries = true
    )
    public CardTemplateResponseDTO createCardTemplate(
            Long setId,
            CreateCardTemplateRequest request,
            MultipartFile imageFile
    ) {
        CardSet cardSet = cardSetRepository.findById(setId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Bộ thẻ với id = " + setId));

        if (cardTemplateRepository.existsByCardSetIdAndCardNumber(setId, request.cardNumber())) {
            throw new DuplicateResourceException(String.format(
                    "Số thứ tự thẻ #%d (%s) đã tồn tại trong bộ '%s' (%s). Vui lòng chọn số thứ tự khác!",
                    request.cardNumber(),
                    CardTemplateResponseDTO.formatCardNumber(request.cardNumber(), cardSet.getBaseSetTotal()),
                    cardSet.getName(),
                    cardSet.getCode()
            ));
        }

        // Gọi mạng ngoài Cloudinary NGOÀI phạm vi transaction DB
        String finalImageUrl = resolveImageUrl(request, imageFile, cardSet.getCode());

        return transactionTemplate.execute(status -> {
            CardSet managedSet = cardSetRepository.findById(setId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Bộ thẻ với id = " + setId));
            Rarity rarity = rarityRepository.findById(request.rarityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Độ hiếm với id = " + request.rarityId()));
            CardSpecies species = cardSpeciesRepository.findById(request.speciesId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Loài (CardSpecies) với id = " + request.speciesId()));

            CardTemplate cardTemplate = CardTemplate.builder()
                    .name(request.name().trim())
                    .cardNumber(request.cardNumber())
                    .imageUrl(finalImageUrl)
                    .cardSet(managedSet)
                    .rarity(rarity)
                    .cardSpecies(species)
                    .build();

            CardTemplate saved = cardTemplateRepository.save(cardTemplate);
            log.info("Created new CardTemplate id={}, set={}, number={}",
                    saved.getId(), managedSet.getCode(),
                    CardTemplateResponseDTO.formatCardNumber(saved.getCardNumber(), managedSet.getBaseSetTotal()));

            return CardTemplateResponseDTO.fromEntity(saved);
        });
    }

    /**
     * Cập nhật động cấu hình cardsPerPack, packsPerBox (và giá) của CardSet.
     * Chỉ áp dụng cho các lô BoosterBox được sinh ra sau thời điểm cập nhật;
     * các BoosterPack cũ giữ nguyên snapshotCardsPerPack.
     */
    @Transactional
    @CacheEvict(
            value = {RedisCacheConfig.CACHE_CARD_SETS, RedisCacheConfig.CACHE_CARD_TEMPLATES},
            allEntries = true
    )
    public CardSetResponseDTO updateCardSetConfig(Long setId, UpdateCardSetConfigRequest request) {
        CardSet cardSet = cardSetRepository.findById(setId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Bộ thẻ với id = " + setId));

        cardSet.setCardsPerPack(request.cardsPerPack());
        cardSet.setPacksPerBox(request.packsPerBox());

        if (request.packPrice() != null) {
            cardSet.setPackPrice(request.packPrice());
            cardSet.setBoxPrice(SealedStoreVaultService.computeDiscountedBoxPrice(request.packPrice(), request.packsPerBox()));
        } else if (request.boxPrice() != null) {
            cardSet.setBoxPrice(request.boxPrice());
        }
        if (request.dailyPackLimit() != null && request.dailyPackLimit() >= 0) {
            cardSet.setDailyPackLimit(request.dailyPackLimit());
        }
        if (request.dailyBoxLimit() != null && request.dailyBoxLimit() >= 0) {
            cardSet.setDailyBoxLimit(request.dailyBoxLimit());
        }
        if (request.packImageUrl() != null) {
            cardSet.setPackImageUrl(normalizeNullableImageUrl(request.packImageUrl()));
        }
        if (request.boxImageUrl() != null) {
            cardSet.setBoxImageUrl(normalizeNullableImageUrl(request.boxImageUrl()));
        }

        CardSet saved = cardSetRepository.save(cardSet);
        log.info("Updated CardSet config setId={}: cardsPerPack={}, packsPerBox={}",
                setId, saved.getCardsPerPack(), saved.getPacksPerBox());

        return CardSetResponseDTO.fromEntity(saved);
    }

    /**
     * Cập nhật động trọng số rơi (dropWeight) của từng Rarity phục vụ Gacha Engine.
     * Bắt buộc xóa vùng nhớ cache rarities và card_templates.
     */
    @Transactional
    @CacheEvict(
            value = {RedisCacheConfig.CACHE_RARITIES, RedisCacheConfig.CACHE_CARD_TEMPLATES},
            allEntries = true
    )
    public List<RarityResponseDTO> updateRarityDropWeights(UpdateRarityDropWeightsRequest request) {
        int totalWeight = request.items().stream()
                .mapToInt(UpdateRarityDropWeightsRequest.RarityWeightItem::dropWeight)
                .sum();
        if (totalWeight <= 0) {
            throw new BusinessValidationException("Tổng trọng số rơi (dropWeight) của các độ hiếm phải lớn hơn 0.");
        }

        for (UpdateRarityDropWeightsRequest.RarityWeightItem item : request.items()) {
            Rarity rarity = rarityRepository.findById(item.rarityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Độ hiếm với id = " + item.rarityId()));
            rarity.setDropWeight(item.dropWeight());
            rarityRepository.save(rarity);
        }

        log.info("Updated dropWeights for {} rarities", request.items().size());
        return rarityRepository.findAllByOrderByDropWeightDesc()
                .stream()
                .map(RarityResponseDTO::fromEntity)
                .toList();
    }

    /**
     * Khởi tạo lô BoosterBox mới cho một CardSet.
     * Quy tắc Configuration Snapshot (.agent/rules.md):
     * Ngay tại thời điểm khởi tạo BoosterBox và sinh ra các BoosterPack con,
     * bắt buộc đọc giá trị cardsPerPack hiện tại của CardSet và gán cứng vào snapshotCardsPerPack
     * của từng BoosterPack.
     */
    @Transactional
    public List<BoosterBoxSummaryResponseDTO> createBoosterBoxBatch(
            Long setId,
            CreateBoosterBoxBatchRequest request
    ) {
        CardSet cardSet = cardSetRepository.findById(setId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Bộ thẻ với id = " + setId));

        int currentCardsPerPack = cardSet.getCardsPerPack();
        int currentPacksPerBox = cardSet.getPacksPerBox();

        List<BoosterBox> boxesToSave = new ArrayList<>(request.quantity());

        for (int i = 0; i < request.quantity(); i++) {
            String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String boxCode = String.format("%s-BOX-%s", cardSet.getCode(), uniqueSuffix);

            BoosterBox box = BoosterBox.builder()
                    .boxCode(boxCode)
                    .cardSet(cardSet)
                    .totalPacks(currentPacksPerBox)
                    .remainingPacks(currentPacksPerBox)
                    .canBuyWholeBox(true)
                    .snapshotBoxPrice(cardSet.getBoxPrice())
                    .snapshotCardsPerPack(currentCardsPerPack)
                    .build();

            for (int position = 1; position <= currentPacksPerBox; position++) {
                BoosterPack pack = BoosterPack.builder()
                        .boosterBox(box)
                        .positionInBox(position)
                        .snapshotCardsPerPack(currentCardsPerPack) // Đóng băng cứng cardsPerPack hiện tại
                        .snapshotPackPrice(cardSet.getPackPrice())
                        .status(PackStatus.AVAILABLE)
                        .build();
                box.getPacks().add(pack);
            }

            boxesToSave.add(box);
        }

        List<BoosterBox> savedBoxes = boosterBoxRepository.saveAll(boxesToSave);
        log.info("Initialized {} BoosterBoxes for CardSet {} ({} packs/box, snapshotCardsPerPack={})",
                savedBoxes.size(), cardSet.getCode(), currentPacksPerBox, currentCardsPerPack);

        return savedBoxes.stream()
                .map(BoosterBoxSummaryResponseDTO::fromEntity)
                .toList();
    }

    /**
     * Thu hồi / Xóa 1 BoosterBox còn nguyên seal (PESSIMISTIC_WRITE).
     * Chỉ cho phép xóa khi status == AVAILABLE và remainingPacks == totalPacks.
     */
    @Transactional
    public Map<String, Object> deleteSealedBoosterBox(Long boxId) {
        BoosterBox box = boosterBoxRepository.findByIdForUpdate(boxId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hộp bài với id = " + boxId));

        if (box.getStatus() != BoxStatus.AVAILABLE || !box.getRemainingPacks().equals(box.getTotalPacks())) {
            throw new BusinessValidationException(
                    "Không thể xóa hộp '" + box.getBoxCode() + "' vì đã có gói bài được mua hoặc hộp không còn nguyên seal!"
            );
        }

        String boxCode = box.getBoxCode();
        boosterBoxRepository.delete(box);
        log.info("Deleted sealed BoosterBox id={}, boxCode={}", boxId, boxCode);
        return Map.of(
                "deleted", true,
                "boxId", boxId,
                "boxCode", boxCode,
                "message", "Đã thu hồi hộp bài nguyên seal " + boxCode + "."
        );
    }

    /**
     * Thu hồi hàng loạt N hộp bài còn nguyên seal thuộc 1 Bộ sưu tập (PESSIMISTIC_WRITE).
     */
    @Transactional
    public Map<String, Object> reduceSealedBoosterBoxes(Long setId, ReduceBoosterBoxesRequest request) {
        if (!cardSetRepository.existsById(setId)) {
            throw new ResourceNotFoundException("Không tìm thấy Bộ thẻ với id = " + setId);
        }
        List<BoosterBox> sealedBoxes = boosterBoxRepository.findSealedAvailableBoxesBySetIdForUpdate(
                setId, PageRequest.of(0, request.quantity())
        );
        if (sealedBoxes.isEmpty()) {
            throw new BusinessValidationException("Không có hộp bài nguyên seal (AVAILABLE) nào để thu hồi trong bộ này.");
        }
        List<String> removedCodes = sealedBoxes.stream().map(BoosterBox::getBoxCode).toList();
        boosterBoxRepository.deleteAll(sealedBoxes);
        log.info("Reduced {} sealed BoosterBoxes for setId={}: {}", removedCodes.size(), setId, removedCodes);
        return Map.of(
                "removedCount", removedCodes.size(),
                "removedBoxCodes", removedCodes,
                "message", "Đã thu hồi " + removedCodes.size() + " hộp bài nguyên seal."
        );
    }

    // ==================== 3. SỬA & XÓA KHUÔN MẪU THẺ BÀI (CARD TEMPLATE UPDATE/DELETE) ====================

    /**
     * Cập nhật CardTemplate (Upload Cloudinary ngoài TransactionTemplate để không giữ DB connection).
     */
    @CacheEvict(value = {RedisCacheConfig.CACHE_CARD_TEMPLATES, RedisCacheConfig.CACHE_CARD_SETS}, allEntries = true)
    public CardTemplateResponseDTO updateCardTemplate(
            Long setId,
            Long cardId,
            CreateCardTemplateRequest request,
            MultipartFile imageFile
    ) {
        CardSet cardSet = cardSetRepository.findById(setId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Bộ thẻ với id = " + setId));

        CardTemplate existing = cardTemplateRepository.findWithDetailsById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thẻ bài với id = " + cardId));
        if (!existing.getCardSet().getId().equals(setId)) {
            throw new BusinessValidationException("Thẻ bài không thuộc Bộ sưu tập đã chọn.");
        }

        if (cardTemplateRepository.existsByCardSetIdAndCardNumberAndIdNot(setId, request.cardNumber(), cardId)) {
            throw new DuplicateResourceException(String.format(
                    "Số thứ tự thẻ #%d (%s) đã tồn tại trong bộ '%s'!",
                    request.cardNumber(),
                    CardTemplateResponseDTO.formatCardNumber(request.cardNumber(), cardSet.getBaseSetTotal()),
                    cardSet.getName()
            ));
        }

        // Upload ảnh mới NGOÀI transaction DB nếu người dùng gửi kèm file
        String finalImageUrl = (imageFile != null && !imageFile.isEmpty())
                ? cloudinaryService.uploadCardImage(imageFile, cardSet.getCode(), request.cardNumber())
                : (request.imageUrl() != null && !request.imageUrl().isBlank() ? request.imageUrl().trim() : existing.getImageUrl());

        return transactionTemplate.execute(status -> {
            CardTemplate managed = cardTemplateRepository.findWithDetailsById(cardId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thẻ bài với id = " + cardId));
            Rarity rarity = rarityRepository.findById(request.rarityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Độ hiếm với id = " + request.rarityId()));
            CardSpecies species = cardSpeciesRepository.findById(request.speciesId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Loài với id = " + request.speciesId()));

            managed.setName(request.name().trim());
            managed.setCardNumber(request.cardNumber());
            managed.setImageUrl(finalImageUrl);
            managed.setRarity(rarity);
            managed.setCardSpecies(species);
            managed.setIsActive(true);

            CardTemplate saved = cardTemplateRepository.save(managed);
            log.info("Updated CardTemplate id={}, set={}, number={}", saved.getId(), cardSet.getCode(), saved.getCardNumber());
            return CardTemplateResponseDTO.fromEntity(saved);
        });
    }

    /**
     * Xóa CardTemplate: Hard Delete nếu chưa có UserCard tham chiếu; Soft Delete (isActive = false) nếu đã có người chơi sở hữu.
     */
    @Transactional
    @CacheEvict(value = {RedisCacheConfig.CACHE_CARD_TEMPLATES, RedisCacheConfig.CACHE_CARD_SETS}, allEntries = true)
    public Map<String, Object> deleteCardTemplate(Long setId, Long cardId) {
        CardTemplate card = cardTemplateRepository.findWithDetailsById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thẻ bài với id = " + cardId));
        if (!card.getCardSet().getId().equals(setId)) {
            throw new BusinessValidationException("Thẻ bài không thuộc Bộ sưu tập đã chọn.");
        }

        if (!userCardRepository.existsByCardTemplateId(cardId)) {
            cardTemplateRepository.delete(card);
            log.info("Hard deleted CardTemplate id={}, name={}", cardId, card.getName());
            return Map.of(
                    "deleted", true,
                    "softDeleted", false,
                    "message", "Đã xóa vĩnh viễn thẻ bài '" + card.getName() + "'."
            );
        }

        card.setIsActive(false);
        cardTemplateRepository.save(card);
        log.info("Soft deleted CardTemplate id={}, name={} (owned by players)", cardId, card.getName());
        return Map.of(
                "deleted", true,
                "softDeleted", true,
                "message", "Thẻ '" + card.getName() + "' đã được ẩn khỏi Gacha (Soft Delete) nhưng vẫn giữ nguyên trong Kho đồ người chơi."
        );
    }

    // ==================== 4. CRUD ĐỘ HIẾM (RARITY) & LOÀI (CARD SPECIES) ====================

    @Transactional
    @CacheEvict(value = {RedisCacheConfig.CACHE_RARITIES, RedisCacheConfig.CACHE_CARD_TEMPLATES}, allEntries = true)
    public RarityResponseDTO createRarity(UpsertRarityRequest request) {
        String cleanName = request.name().trim();
        if (rarityRepository.existsByNameIgnoreCase(cleanName)) {
            throw new DuplicateResourceException("Độ hiếm '" + cleanName + "' đã tồn tại!");
        }
        Rarity saved = rarityRepository.save(Rarity.builder()
                .name(cleanName)
                .dropWeight(request.dropWeight())
                .colorHex(request.colorHex().trim().toUpperCase())
                .isHolographic(Boolean.TRUE.equals(request.isHolographic()))
                .revealEffect(request.revealEffect() != null ? request.revealEffect() : RevealEffect.NONE)
                .sanctuaryCoinsPerMin(request.sanctuaryCoinsPerMin() != null ? request.sanctuaryCoinsPerMin() : 2)
                .buybackPrice(request.buybackPrice() != null ? request.buybackPrice() : new java.math.BigDecimal("5.00"))
                .build());
        log.info("Created new Rarity id={}, name={}, weight={}, holo={}, effect={}, coinsPerMin={}, buybackPrice={}",
                saved.getId(), saved.getName(), saved.getDropWeight(), saved.getIsHolographic(), saved.getRevealEffect(), saved.getSanctuaryCoinsPerMin(), saved.getBuybackPrice());
        return RarityResponseDTO.fromEntity(saved);
    }

    @Transactional
    @CacheEvict(value = {RedisCacheConfig.CACHE_RARITIES, RedisCacheConfig.CACHE_CARD_TEMPLATES}, allEntries = true)
    public RarityResponseDTO updateRarity(Long rarityId, UpsertRarityRequest request) {
        Rarity rarity = rarityRepository.findById(rarityId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Độ hiếm với id = " + rarityId));

        String cleanName = request.name().trim();
        if (rarityRepository.existsByNameIgnoreCaseAndIdNot(cleanName, rarityId)) {
            throw new DuplicateResourceException("Độ hiếm '" + cleanName + "' đã tồn tại!");
        }
        rarity.setName(cleanName);
        rarity.setDropWeight(request.dropWeight());
        rarity.setColorHex(request.colorHex().trim().toUpperCase());
        if (request.isHolographic() != null) {
            rarity.setIsHolographic(request.isHolographic());
        }
        if (request.revealEffect() != null) {
            rarity.setRevealEffect(request.revealEffect());
        }
        if (request.sanctuaryCoinsPerMin() != null) {
            rarity.setSanctuaryCoinsPerMin(request.sanctuaryCoinsPerMin());
        }
        if (request.buybackPrice() != null) {
            rarity.setBuybackPrice(request.buybackPrice());
        }
        Rarity saved = rarityRepository.save(rarity);
        log.info("Updated Rarity id={}, name={}, holo={}, effect={}, coinsPerMin={}, buybackPrice={}",
                saved.getId(), saved.getName(), saved.getIsHolographic(), saved.getRevealEffect(), saved.getSanctuaryCoinsPerMin(), saved.getBuybackPrice());
        return RarityResponseDTO.fromEntity(saved);
    }

    @Transactional
    @CacheEvict(value = {RedisCacheConfig.CACHE_RARITIES, RedisCacheConfig.CACHE_CARD_TEMPLATES}, allEntries = true)
    public Map<String, Object> deleteRarity(Long rarityId) {
        Rarity rarity = rarityRepository.findById(rarityId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Độ hiếm với id = " + rarityId));
        if (cardTemplateRepository.existsByRarityId(rarityId)) {
            throw new BusinessValidationException(
                    "Không thể xóa độ hiếm '" + rarity.getName() + "' vì đang có thẻ bài tham chiếu tới độ hiếm này!"
            );
        }
        rarityRepository.delete(rarity);
        log.info("Deleted Rarity id={}, name={}", rarityId, rarity.getName());
        return Map.of("deleted", true, "message", "Đã xóa độ hiếm '" + rarity.getName() + "'.");
    }

    @Transactional
    @CacheEvict(value = {RedisCacheConfig.CACHE_CARD_SPECIES, RedisCacheConfig.CACHE_CARD_TEMPLATES}, allEntries = true)
    public CardSpeciesResponseDTO createCardSpecies(UpsertCardSpeciesRequest request) {
        String cleanName = request.name().trim();
        if (cardSpeciesRepository.existsByNameIgnoreCase(cleanName)) {
            throw new DuplicateResourceException("Loài '" + cleanName + "' đã tồn tại!");
        }
        CardSpecies saved = cardSpeciesRepository.save(CardSpecies.builder()
                .name(cleanName)
                .isBeta(request.isBeta() == null || request.isBeta())
                .build());
        log.info("Created new CardSpecies id={}, name={}, isBeta={}", saved.getId(), saved.getName(), saved.getIsBeta());
        return CardSpeciesResponseDTO.fromEntity(saved);
    }

    @Transactional
    @CacheEvict(value = {RedisCacheConfig.CACHE_CARD_SPECIES, RedisCacheConfig.CACHE_CARD_TEMPLATES}, allEntries = true)
    public CardSpeciesResponseDTO updateCardSpecies(Long speciesId, UpsertCardSpeciesRequest request) {
        CardSpecies species = cardSpeciesRepository.findById(speciesId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Loài với id = " + speciesId));

        String cleanName = request.name().trim();
        if (cardSpeciesRepository.existsByNameIgnoreCaseAndIdNot(cleanName, speciesId)) {
            throw new DuplicateResourceException("Loài '" + cleanName + "' đã tồn tại!");
        }
        species.setName(cleanName);
        if (request.isBeta() != null) {
            species.setIsBeta(request.isBeta());
        }
        CardSpecies saved = cardSpeciesRepository.save(species);
        log.info("Updated CardSpecies id={}, name={}", saved.getId(), saved.getName());
        return CardSpeciesResponseDTO.fromEntity(saved);
    }

    @Transactional
    @CacheEvict(value = {RedisCacheConfig.CACHE_CARD_SPECIES, RedisCacheConfig.CACHE_CARD_TEMPLATES}, allEntries = true)
    public Map<String, Object> deleteCardSpecies(Long speciesId) {
        CardSpecies species = cardSpeciesRepository.findById(speciesId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Loài với id = " + speciesId));
        if (cardTemplateRepository.existsByCardSpeciesId(speciesId)) {
            throw new BusinessValidationException(
                    "Không thể xóa loài '" + species.getName() + "' vì đang có thẻ bài thuộc loài này!"
            );
        }
        cardSpeciesRepository.delete(species);
        log.info("Deleted CardSpecies id={}, name={}", speciesId, species.getName());
        return Map.of("deleted", true, "message", "Đã xóa loài '" + species.getName() + "'.");
    }

    private String resolveImageUrl(
            CreateCardTemplateRequest request,
            MultipartFile imageFile,
            String setCode
    ) {
        if (imageFile != null && !imageFile.isEmpty()) {
            return cloudinaryService.uploadCardImage(imageFile, setCode, request.cardNumber());
        }
        if (request.imageUrl() != null && !request.imageUrl().isBlank()) {
            return request.imageUrl().trim();
        }
        throw new BusinessValidationException("Phải cung cấp file ảnh thẻ bài 2D (imageFile) hoặc đường dẫn ảnh (imageUrl).");
    }
}
