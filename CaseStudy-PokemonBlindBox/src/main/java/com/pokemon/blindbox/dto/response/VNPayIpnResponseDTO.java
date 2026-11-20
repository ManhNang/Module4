package com.pokemon.blindbox.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * Phản hồi chuẩn định dạng JSON bắt buộc của VNPay IPN Webhook:
 * {"RspCode": "00", "Message": "Confirm Success"}
 */
@Builder
public record VNPayIpnResponseDTO(
        @JsonProperty("RspCode")
        String rspCode,

        @JsonProperty("Message")
        String message
) {
    public static VNPayIpnResponseDTO of(String rspCode, String message) {
        return new VNPayIpnResponseDTO(rspCode, message);
    }
}
