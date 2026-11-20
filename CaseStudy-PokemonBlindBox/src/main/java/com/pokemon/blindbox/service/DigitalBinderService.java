package com.pokemon.blindbox.service;

import com.pokemon.blindbox.dto.request.CardTemplateFilterRequest;
import com.pokemon.blindbox.dto.response.BinderSlotDTO;
import com.pokemon.blindbox.dto.response.CardTemplateResponseDTO;
import com.pokemon.blindbox.dto.response.MasterSetProgressDTO;
import com.pokemon.blindbox.entity.CardSet;
import com.pokemon.blindbox.entity.CardTemplate;
import com.pokemon.blindbox.exception.ResourceNotFoundException;
import com.pokemon.blindbox.repository.CardSetRepository;
import com.pokemon.blindbox.repository.CardTemplateRepository;
import com.pokemon.blindbox.repository.UserCardRepository;
import com.pokemon.blindbox.repository.specification.CardTemplateSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DigitalBinderService xử lý nghiệp vụ:
 * 1. Lọc và tra cứu thẻ bài trên Pokedex bằng CardTemplateSpecification + @EntityGraph (chống N+1 Query),
 *    mặc định sắp xếp ORDER BY cardNumber ASC.
 * 2. Thống kê Thanh tiến độ sưu tầm (MasterSetProgressDTO) theo setId:
 *    - Đếm số thẻ chuẩn không trùng lặp đã mở trúng (cardNumber <= baseSetTotal, VD: 72/103).
 *    - Đếm số thẻ Secret vượt khung đã mở trúng (cardNumber > baseSetTotal, VD: 141/103).
 *    - Trả về đầy đủ danh sách các ô từ 001 đến baseSetTotal (103) sắp xếp ORDER BY cardNumber ASC
 *      kèm cờ isOwned để Frontend hiển thị sáng hoặc bóng mờ.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DigitalBinderService {

    private final CardSetRepository cardSetRepository;
    private final CardTemplateRepository cardTemplateRepository;
    private final UserCardRepository userCardRepository;

    /**
     * Lọc danh sách khuôn mẫu thẻ theo Tên, Số thứ tự, Độ Hiếm, Đặt (CardSet) và Loài (CardSpecies).
     * Sử dụng @EntityGraph trên CardTemplateRepository.findAll(spec, pageable) để lấy trọn vẹn trong 1 câu SQL.
     */
    public Page<CardTemplateResponseDTO> searchCardTemplates(
            CardTemplateFilterRequest filter,
            Pageable pageable
    ) {
        Pageable sortedPageable = ensureSortedByCardNumberAsc(pageable);
        Specification<CardTemplate> spec = CardTemplateSpecification.withFilter(filter);

        return cardTemplateRepository.findAll(spec, sortedPageable)
                .map(CardTemplateResponseDTO::fromEntity);
    }

    /**
     * Thống kê Thanh tiến độ sưu tầm (MasterSetProgressDTO) và lưới ô Sổ Sưu Tập từ 001..baseSetTotal.
     */
    public MasterSetProgressDTO getMasterSetProgress(Long userId, Long setId) {
        CardSet cardSet = cardSetRepository.findById(setId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Bộ thẻ với setId = " + setId));

        int baseSetTotal = cardSet.getBaseSetTotal();

        // 1. Lấy tất cả CardTemplate của bộ thẻ đã sắp xếp ORDER BY cardNumber ASC (1 câu SQL nhờ @EntityGraph)
        List<CardTemplate> templates = cardTemplateRepository.findByCardSetIdOrderByCardNumberAsc(setId);

        // 2. Lấy số lượng sở hữu theo từng cardTemplateId của userId trong bộ thẻ bằng 1 câu SQL GROUP BY
        Map<Long, Long> ownedCountByTemplateId = userCardRepository.countOwnedCardsByUserAndSet(userId, setId)
                .stream()
                .collect(Collectors.toMap(
                        UserCardRepository.OwnedCardCountProjection::getCardTemplateId,
                        UserCardRepository.OwnedCardCountProjection::getOwnedQuantity
                ));

        Map<Integer, CardTemplate> baseTemplateByNumber = new HashMap<>();
        List<CardTemplate> secretTemplates = new ArrayList<>();

        for (CardTemplate template : templates) {
            if (template.getCardNumber() <= baseSetTotal) {
                baseTemplateByNumber.put(template.getCardNumber(), template);
            } else {
                secretTemplates.add(template);
            }
        }

        // 3. Dựng danh sách các ô chuẩn từ 001 đến baseSetTotal (ví dụ: 001 / 103 -> 103 / 103) theo thứ tự tăng dần
        List<BinderSlotDTO> baseSetSlots = new ArrayList<>(baseSetTotal);
        int ownedBaseCardCount = 0;

        for (int num = 1; num <= baseSetTotal; num++) {
            CardTemplate template = baseTemplateByNumber.get(num);
            long qty = (template != null) ? ownedCountByTemplateId.getOrDefault(template.getId(), 0L) : 0L;
            boolean isOwned = qty > 0;
            if (isOwned) {
                ownedBaseCardCount++;
            }

            BinderSlotDTO slot = BinderSlotDTO.builder()
                    .cardNumber(num)
                    .formattedCardNumber(CardTemplateResponseDTO.formatCardNumber(num, baseSetTotal))
                    .isOwned(isOwned)
                    .ownedQuantity(qty)
                    .secretRare(false)
                    .cardTemplate(template != null ? CardTemplateResponseDTO.fromEntity(template) : null)
                    .build();
            baseSetSlots.add(slot);
        }

        // 4. Dựng danh sách các thẻ Secret vượt khung (cardNumber > baseSetTotal, ví dụ: 141 / 103)
        List<BinderSlotDTO> secretRareSlots = new ArrayList<>(secretTemplates.size());
        int ownedSecretRareCount = 0;

        for (CardTemplate secretTemplate : secretTemplates) {
            long qty = ownedCountByTemplateId.getOrDefault(secretTemplate.getId(), 0L);
            boolean isOwned = qty > 0;
            if (isOwned) {
                ownedSecretRareCount++;
            }

            secretRareSlots.add(BinderSlotDTO.builder()
                    .cardNumber(secretTemplate.getCardNumber())
                    .formattedCardNumber(CardTemplateResponseDTO.formatCardNumber(secretTemplate.getCardNumber(), baseSetTotal))
                    .isOwned(isOwned)
                    .ownedQuantity(qty)
                    .secretRare(true)
                    .cardTemplate(CardTemplateResponseDTO.fromEntity(secretTemplate))
                    .build());
        }

        BigDecimal completionPercentage = baseSetTotal > 0
                ? BigDecimal.valueOf(ownedBaseCardCount)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(baseSetTotal), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        String formattedBaseProgress = String.format("%d / %d", ownedBaseCardCount, baseSetTotal);

        return MasterSetProgressDTO.builder()
                .userId(userId)
                .setId(cardSet.getId())
                .setName(cardSet.getName())
                .setCode(cardSet.getCode())
                .baseSetTotal(baseSetTotal)
                .ownedBaseCardCount(ownedBaseCardCount)
                .formattedBaseProgress(formattedBaseProgress)
                .baseCompletionPercentage(completionPercentage)
                .totalSecretRareInSet(secretTemplates.size())
                .ownedSecretRareCount(ownedSecretRareCount)
                .baseSetSlots(baseSetSlots)
                .secretRareSlots(secretRareSlots)
                .build();
    }

    private Pageable ensureSortedByCardNumberAsc(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return PageRequest.of(0, 50, Sort.by(Sort.Direction.ASC, "cardNumber"));
        }
        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.ASC, "cardNumber")
            );
        }
        return pageable;
    }
}
