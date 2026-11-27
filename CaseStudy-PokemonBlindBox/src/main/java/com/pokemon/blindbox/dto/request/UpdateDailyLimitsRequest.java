package com.pokemon.blindbox.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateDailyLimitsRequest(
        @Min(value = 0, message = "Hạn mức Pack mỗi ngày không được âm!")
        @Max(value = 100000, message = "Hạn mức Pack mỗi ngày không vượt quá 100.000!")
        Integer dailyPackLimit,
        @Min(value = 0, message = "Hạn mức Box mỗi ngày không được âm!")
        @Max(value = 10000, message = "Hạn mức Box mỗi ngày không vượt quá 10.000!")
        Integer dailyBoxLimit
) {}
