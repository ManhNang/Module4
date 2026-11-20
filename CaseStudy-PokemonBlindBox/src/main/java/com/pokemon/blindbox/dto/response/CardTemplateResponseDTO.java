package com.pokemon.blindbox.dto.response;

import com.pokemon.blindbox.entity.CardTemplate;
import lombok.Builder;

/**
 * DTO trả về thông tin khuôn mẫu thẻ bài.
 * Sử dụng Java 21 Record kết hợp hàm format số thứ tự thẻ chuẩn "098 / 103" hoặc "141 / 103".
 */
@Builder
public record CardTemplateResponseDTO(
        Long id,
        String name,
        Integer cardNumber,
        Integer baseSetTotal,
        String formattedCardNumber,
        boolean secretRare,
        String imageUrl,
        Long setId,
        String setName,
        String setCode,
        Long rarityId,
        String rarityName,
        String rarityColorHex,
        Boolean rarityIsHolographic,
        Long speciesId,
        String speciesName,
        Boolean isBetaSpecies,
        Boolean isActive
) {

    /**
     * Format cardNumber và baseSetTotal thành chuỗi chuẩn "098 / 103" (hoặc "141 / 103" với thẻ Secret).
     * Tuyệt đối không lưu chuỗi này xuống Database mà tính toán ở tầng DTO/Mapper.
     */
    public static String formatCardNumber(Integer cardNumber, Integer baseSetTotal) {
        if (cardNumber == null || baseSetTotal == null) {
            return "";
        }
        return String.format("%03d / %03d", cardNumber, baseSetTotal);
    }

    /**
     * Factory Mapper chuyển đổi từ Entity CardTemplate sang CardTemplateResponseDTO.
     */
    public static CardTemplateResponseDTO fromEntity(CardTemplate entity) {
        Integer cardNum = entity.getCardNumber();
        Integer total = entity.getCardSet().getBaseSetTotal();

        return CardTemplateResponseDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .cardNumber(cardNum)
                .baseSetTotal(total)
                .formattedCardNumber(formatCardNumber(cardNum, total))
                .secretRare(cardNum != null && total != null && cardNum > total)
                .imageUrl(entity.getImageUrl())
                .setId(entity.getCardSet().getId())
                .setName(entity.getCardSet().getName())
                .setCode(entity.getCardSet().getCode())
                .rarityId(entity.getRarity().getId())
                .rarityName(entity.getRarity().getName())
                .rarityColorHex(entity.getRarity().getColorHex())
                .rarityIsHolographic(Boolean.TRUE.equals(entity.getRarity().getIsHolographic()))
                .speciesId(entity.getCardSpecies().getId())
                .speciesName(entity.getCardSpecies().getName())
                .isBetaSpecies(entity.getCardSpecies().getIsBeta())
                .isActive(entity.getIsActive() == null || entity.getIsActive())
                .build();
    }
}
