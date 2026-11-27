package com.pokemon.blindbox.controller;

import com.pokemon.blindbox.dto.request.CreateVietQrDepositRequest;
import com.pokemon.blindbox.security.AuthenticatedUserProvider;
import com.pokemon.blindbox.service.SePayDepositService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "4B. VietQR & SePay Deposit", description = "Nạp Xu tự động 24/7 về tài khoản ngân hàng cá nhân qua VietQR + SePay Webhook")
public class SePayController {

    private final SePayDepositService sePayDepositService;
    private final AuthenticatedUserProvider authUserProvider;

    @PostMapping("/vietqr/create")
    public ResponseEntity<Map<String, Object>> createVietQrDeposit(
            @Valid @RequestBody CreateVietQrDepositRequest request
    ) {
        Long userId = authUserProvider.getRequiredUserId();
        return ResponseEntity.ok(sePayDepositService.createVietQrOrder(userId, request.amount()));
    }

    @GetMapping("/vietqr/status/{transferCode}")
    public ResponseEntity<Map<String, Object>> getVietQrStatus(@PathVariable String transferCode) {
        Long userId = authUserProvider.getRequiredUserId();
        return ResponseEntity.ok(sePayDepositService.getOrderStatus(userId, transferCode));
    }

    @PostMapping("/sepay-webhook")
    public ResponseEntity<Map<String, Object>> handleSePayWebhook(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) Map<String, Object> payload
    ) {
        return ResponseEntity.ok(sePayDepositService.handleSePayWebhook(authorization, payload));
    }
}
