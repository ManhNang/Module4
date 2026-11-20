package com.pokemon.blindbox.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        int status,
        String errorCode,
        String message,
        String path,
        LocalDateTime timestamp,
        Map<String, String> fieldErrors
) {
}
