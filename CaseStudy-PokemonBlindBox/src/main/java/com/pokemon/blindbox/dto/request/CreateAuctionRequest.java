package com.pokemon.blindbox.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CreateAuctionRequest(
        Long sellerId,

        @NotNull(message = "userCardId không được để trống") Long userCardId,

        @NotNull(message = "Giá khởi điểm (startingPrice) không được để trống")
        @DecimalMin(value = "1000.00", message = "Giá khởi điểm tối thiểu là 1,000 VND")
        @DecimalMax(value = "1000000000.00", message = "Giá khởi điểm tối đa là 1,000,000,000 VND")
        @Digits(integer = 13, fraction = 2, message = "Giá khởi điểm không hợp lệ")
        BigDecimal startingPrice,

        @NotNull(message = "Bước giá tối thiểu (minBidIncrement) không được để trống")
        @DecimalMin(value = "1000.00", message = "Bước giá tối thiểu là 1,000 VND")
        @DecimalMax(value = "100000000.00", message = "Bước giá tối đa là 100,000,000 VND")
        @Digits(integer = 13, fraction = 2, message = "Bước giá không hợp lệ")
        BigDecimal minBidIncrement,

        @NotNull(message = "Thời lượng phiên đấu giá (durationMinutes) không được để trống") @Min(value = 5, message = "Thời lượng phiên đấu giá tối thiểu là 5 phút") @Max(value = 10080, message = "Thời lượng phiên đấu giá tối đa là 7 ngày (10080 phút)") Integer durationMinutes) {
}
