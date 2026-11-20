package com.pokemon.blindbox.controller.admin;

import com.pokemon.blindbox.dto.response.VNPayIpnResponseDTO;
import com.pokemon.blindbox.service.VNPayService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint giả lập VNPay IPN chỉ kích hoạt trên môi trường dev/local và yêu cầu quyền ROLE_ADMIN (P0-01).
 */
@RestController
@Profile({"dev", "local"})
@RequestMapping("/api/v1/admin/dev")
@RequiredArgsConstructor
@Tag(name = "1. Admin", description = "Công cụ kiểm thử nội bộ (chỉ kích hoạt ở môi trường dev/local)")
public class DevSimulatePaymentController {

    private final VNPayService vnPayService;

    @PostMapping("/simulate-ipn/{vnpTxnRef}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VNPayIpnResponseDTO> simulateIpnCallback(@PathVariable String vnpTxnRef) {
        return ResponseEntity.ok(vnPayService.simulateSignedIpnForLocalTest(vnpTxnRef));
    }
}
