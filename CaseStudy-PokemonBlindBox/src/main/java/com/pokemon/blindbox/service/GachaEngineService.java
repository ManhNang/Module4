package com.pokemon.blindbox.service;

import com.pokemon.blindbox.dto.response.CardTemplateResponseDTO;
import com.pokemon.blindbox.dto.response.OpenPackResultResponseDTO;
import com.pokemon.blindbox.dto.response.RarityResponseDTO;
import com.pokemon.blindbox.dto.response.UserCardResponseDTO;
import com.pokemon.blindbox.entity.BoosterPack;
import com.pokemon.blindbox.entity.CardTemplate;
import com.pokemon.blindbox.entity.UserCard;
import com.pokemon.blindbox.entity.enums.PackStatus;
import com.pokemon.blindbox.exception.BusinessValidationException;
import com.pokemon.blindbox.exception.ResourceNotFoundException;
import com.pokemon.blindbox.repository.BoosterPackRepository;
import com.pokemon.blindbox.repository.CardTemplateRepository;
import com.pokemon.blindbox.repository.UserCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

/**
 * GachaEngineService thực thi thuật toán mở gói bài (openPack):
 * 1. BẮT BUỘC đọc số lượng thẻ cần rút từ BoosterPack.getSnapshotCardsPerPack(),
 *    tuyệt đối không đọc ngược lại từ CardSet.getCardsPerPack() (.agent/rules.md).
 * 2. Đọc danh sách độ hiếm + trọng số dropWeight và danh sách CardTemplate từ Redis Cache
 *    thông qua CatalogCacheService (tránh Self-invocation).
 * 3. Quay ngẫu nhiên theo trọng số bằng CSPRNG (SecureRandom — P1-09), lưu kết quả vào UserCard
 *    và cập nhật trạng thái BoosterPack sang OPENED (có @Version chống mở trùng).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GachaEngineService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Long DEFAULT_SYSTEM_USER_ID = 1L;

    private final BoosterPackRepository boosterPackRepository;
    private final CardTemplateRepository cardTemplateRepository;
    private final UserCardRepository userCardRepository;
    private final CatalogCacheService catalogCacheService;

    /**
     * Mở gói bài theo packId (sử dụng buyerId đã ghi trên BoosterPack).
     */
    @Transactional
    public OpenPackResultResponseDTO openPack(Long packId) {
        return openPack(null, packId);
    }

    /**
     * Mở gói bài theo packId và kiểm tra quyền sở hữu nếu truyền userId.
     */
    @Transactional
    public OpenPackResultResponseDTO openPack(Long userId, Long packId) {
        BoosterPack pack = boosterPackRepository.findWithBoxAndSetById(packId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gói bài với packId = " + packId));

        if (pack.getStatus() == PackStatus.OPENED) {
            throw new BusinessValidationException("Gói bài này đã được mở trước đó!");
        }
        if (pack.getStatus() != PackStatus.SOLD) {
            throw new BusinessValidationException("Gói bài phải được mua (trạng thái SOLD) trước khi mở!");
        }

        Long ownerUserId = pack.getBuyerId() != null
                ? pack.getBuyerId()
                : (userId != null ? userId : DEFAULT_SYSTEM_USER_ID);

        if (userId != null && pack.getBuyerId() != null && !pack.getBuyerId().equals(userId)) {
            throw new BusinessValidationException("Bạn không phải là chủ sở hữu của gói bài này!");
        }

        // QUY TẮC BẮT BUỘC: Chỉ đọc số lượng thẻ cần rút từ snapshotCardsPerPack của BoosterPack
        int cardsToDraw = pack.getSnapshotCardsPerPack();
        if (cardsToDraw <= 0) {
            throw new BusinessValidationException("Cấu hình snapshotCardsPerPack của gói bài không hợp lệ.");
        }

        Long setId = pack.getBoosterBox().getCardSet().getId();

        // Đọc danh sách Rarity (kèm dropWeight) và CardTemplate của từng Rarity từ Redis Cache
        List<RarityResponseDTO> cachedRarities = catalogCacheService.getAllRaritiesWithWeights();
        List<WeightedRarityPool> activePools = buildActiveRarityPools(setId, cachedRarities);

        if (activePools.isEmpty()) {
            throw new BusinessValidationException(
                    "Bộ sưu tập chưa có khuôn mẫu thẻ bài (CardTemplate) hợp lệ để quay Gacha."
            );
        }

        int totalWeight = activePools.stream().mapToInt(WeightedRarityPool::dropWeight).sum();
        LocalDateTime now = LocalDateTime.now();

        List<CardTemplateResponseDTO> selectedDtos = new ArrayList<>(cardsToDraw);
        List<UserCard> userCardsToSave = new ArrayList<>(cardsToDraw);

        for (int i = 0; i < cardsToDraw; i++) {
            WeightedRarityPool chosenPool = rollRarityPool(activePools, totalWeight);
            List<CardTemplateResponseDTO> templatesInPool = chosenPool.templates();
            CardTemplateResponseDTO chosenCardDto = templatesInPool.get(
                    SECURE_RANDOM.nextInt(templatesInPool.size())
            );

            CardTemplate templateRef = cardTemplateRepository.getReferenceById(chosenCardDto.id());
            UserCard userCard = UserCard.builder()
                    .userId(ownerUserId)
                    .cardTemplate(templateRef)
                    .boosterPack(pack)
                    .isLocked(false)
                    .acquiredAt(now)
                    .build();

            selectedDtos.add(chosenCardDto);
            userCardsToSave.add(userCard);
        }

        pack.setStatus(PackStatus.OPENED);
        pack.setOpenedAt(now);
        boosterPackRepository.saveAndFlush(pack);

        List<UserCard> savedUserCards = userCardRepository.saveAll(userCardsToSave);

        List<UserCardResponseDTO> drawnCardResponses = new ArrayList<>(savedUserCards.size());
        for (int i = 0; i < savedUserCards.size(); i++) {
            drawnCardResponses.add(UserCardResponseDTO.fromEntity(savedUserCards.get(i), selectedDtos.get(i)));
        }

        log.info("Opened packId={} (boxId={}, pos={}) for userId={}: drew {} cards using snapshotCardsPerPack={}",
                packId, pack.getBoosterBox().getId(), pack.getPositionInBox(),
                ownerUserId, drawnCardResponses.size(), cardsToDraw);

        return OpenPackResultResponseDTO.builder()
                .packId(pack.getId())
                .boxId(pack.getBoosterBox().getId())
                .positionInBox(pack.getPositionInBox())
                .snapshotCardsPerPack(cardsToDraw)
                .openedAt(now)
                .drawnCards(drawnCardResponses)
                .build();
    }

    private List<WeightedRarityPool> buildActiveRarityPools(Long setId, List<RarityResponseDTO> rarities) {
        if (rarities == null || rarities.isEmpty()) {
            return List.of();
        }
        List<WeightedRarityPool> pools = new ArrayList<>();
        for (RarityResponseDTO rarity : rarities) {
            if (rarity.dropWeight() != null && rarity.dropWeight() > 0) {
                List<CardTemplateResponseDTO> templates =
                        catalogCacheService.getCardTemplatesBySetAndRarity(setId, rarity.id());
                if (templates != null && !templates.isEmpty()) {
                    pools.add(new WeightedRarityPool(rarity, rarity.dropWeight(), templates));
                }
            }
        }
        return pools;
    }

    private WeightedRarityPool rollRarityPool(List<WeightedRarityPool> pools, int totalWeight) {
        int randomRoll = SECURE_RANDOM.nextInt(totalWeight);
        int cumulative = 0;
        for (WeightedRarityPool pool : pools) {
            cumulative += pool.dropWeight();
            if (randomRoll < cumulative) {
                return pool;
            }
        }
        return pools.getLast();
    }

    private record WeightedRarityPool(
            RarityResponseDTO rarity,
            int dropWeight,
            List<CardTemplateResponseDTO> templates
    ) {
    }
}
