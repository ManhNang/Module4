package com.pokemon.blindbox.dto.response;

import com.pokemon.blindbox.entity.enums.BoxStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record BoxPurchaseResponseDTO(
        Long boxId,
        String boxCode,
        boolean canBuyWholeBox,
        Integer remainingPacks,
        BoxStatus boxStatus,
        BigDecimal totalCharged,
        BigDecimal remainingWalletBalance,
        List<BoosterPackResponseDTO> purchasedPacks
) {
}
