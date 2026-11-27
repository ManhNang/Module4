package com.pokemon.blindbox.controller;

import com.pokemon.blindbox.dto.response.WalletResponseDTO;
import com.pokemon.blindbox.dto.response.WalletTransactionResponseDTO;
import com.pokemon.blindbox.security.AuthenticatedUserProvider;
import com.pokemon.blindbox.service.WalletService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "5. Wallet", description = "API Ví Điện Tử Nội Bộ (@Version)")
public class WalletPaymentController {

    private final WalletService walletService;
    private final AuthenticatedUserProvider authUserProvider;

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
}
