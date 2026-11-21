package com.pokemon.blindbox.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PlaceAuctionBidRequest(
        Long bidderId,

        @Size(max = 60, message = "Tên hiển thị người đặt giá tối đa 60 ký tự")
        String bidderDisplayName,

        @NotNull(message = "Mức giá đặt (Xu) không được để trống")
        @DecimalMin(value = "1.00", message = "Mức giá đặt tối thiểu là 1 Xu")
        @DecimalMax(value = "1000000000.00", message = "Mức giá đặt tối đa là 1.000.000.000 Xu")
        @Digits(integer = 13, fraction = 2, message = "Mức giá đặt không hợp lệ")
        BigDecimal bidAmount
) {
}
