package com.pokemon.blindbox.controller;

import com.pokemon.blindbox.dto.request.CreateVNPayDepositRequest;
import com.pokemon.blindbox.dto.response.VNPayIpnResponseDTO;
import com.pokemon.blindbox.dto.response.VNPayPaymentUrlResponseDTO;
import com.pokemon.blindbox.dto.response.WalletResponseDTO;
import com.pokemon.blindbox.dto.response.WalletTransactionResponseDTO;
import com.pokemon.blindbox.security.AuthenticatedUserProvider;
import com.pokemon.blindbox.service.VNPayService;
import com.pokemon.blindbox.service.WalletService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "5. Wallet", description = "API Ví Điện Tử Nội Bộ (@Version) & Tích hợp cổng thanh toán VNPay Sandbox (HMAC-SHA512, Idempotent IPN)")
public class WalletPaymentController {

    private final VNPayService vnPayService;
    private final WalletService walletService;
    private final AuthenticatedUserProvider authUserProvider;

    @PostMapping("/vnpay/create-url")
    public ResponseEntity<VNPayPaymentUrlResponseDTO> createVNPayPaymentUrl(
            @Valid @RequestBody CreateVNPayDepositRequest request,
            HttpServletRequest servletRequest
    ) {
        Long currentUserId = authUserProvider.getRequiredUserId();
        CreateVNPayDepositRequest resolved = CreateVNPayDepositRequest.builder()
                .userId(currentUserId)
                .amount(request.amount())
                .bankCode(request.bankCode())
                .locale(request.locale())
                .build();
        String clientIp = extractClientIp(servletRequest);
        VNPayPaymentUrlResponseDTO response = vnPayService.createPaymentUrl(resolved, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @RequestMapping(value = "/vnpay-ipn", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<VNPayIpnResponseDTO> handleVNPayIpn(@RequestParam Map<String, String> vnpParams) {
        VNPayIpnResponseDTO ipnResponse = vnPayService.processIpnWebhook(vnpParams);
        return ResponseEntity.ok(ipnResponse);
    }

    @GetMapping("/me")
    public ResponseEntity<WalletResponseDTO> getCurrentUserWallet() {
        Long currentUserId = authUserProvider.getRequiredUserId();
        return ResponseEntity.ok(walletService.getWalletByUserId(currentUserId));
    }

    /**
     * Khóa IDOR: chỉ chính chủ hoặc ADMIN mới được xem số dư ví theo userId (P0-06).
     */
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == principal.id")
    public ResponseEntity<WalletResponseDTO> getWalletByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(walletService.getWalletByUserId(userId));
    }

    @GetMapping("/me/transactions")
    public ResponseEntity<List<WalletTransactionResponseDTO>> getCurrentUserTransactions(
            @RequestParam(defaultValue = "50") int limit
    ) {
        Long currentUserId = authUserProvider.getRequiredUserId();
        return ResponseEntity.ok(walletService.getRecentTransactionsByUserId(currentUserId, limit));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
