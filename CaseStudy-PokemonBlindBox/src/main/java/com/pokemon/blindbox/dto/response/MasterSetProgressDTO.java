package com.pokemon.blindbox.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO thống kê Thanh tiến độ sưu tầm (Master Set Progress) và toàn bộ danh sách ô thẻ
 * của một Bộ sưu tập (ví dụ: "Delta Reign", baseSetTotal = 103).
 */
@Builder
public record MasterSetProgressDTO(
        Long userId,
        Long setId,
        String setName,
        String setCode,
        Integer baseSetTotal,
        Integer ownedBaseCardCount,
        String formattedBaseProgress,
        BigDecimal baseCompletionPercentage,
        Integer totalSecretRareInSet,
        Integer ownedSecretRareCount,
        List<BinderSlotDTO> baseSetSlots,
        List<BinderSlotDTO> secretRareSlots
) {
}
