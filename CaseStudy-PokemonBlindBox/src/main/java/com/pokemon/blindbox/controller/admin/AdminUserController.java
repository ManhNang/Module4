package com.pokemon.blindbox.controller.admin;

import com.pokemon.blindbox.dto.request.AdjustUserWalletRequest;
import com.pokemon.blindbox.dto.request.UpdateUserLockRequest;
import com.pokemon.blindbox.dto.request.UpdateUserRoleRequest;
import com.pokemon.blindbox.dto.response.AdminUserSummaryDTO;
import com.pokemon.blindbox.dto.response.WalletTransactionResponseDTO;
import com.pokemon.blindbox.entity.enums.Role;
import com.pokemon.blindbox.security.AuthenticatedUserProvider;
import com.pokemon.blindbox.service.AdminUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "1B. Admin Users", description = "API Quản lý Người dùng, Phân quyền, Khóa tài khoản & Đối soát Ví (ROLE_ADMIN)")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final AuthenticatedUserProvider authUserProvider;

    @GetMapping
    public ResponseEntity<Page<AdminUserSummaryDTO>> listUsers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "role", required = false) Role role,
            @RequestParam(value = "locked", required = false) Boolean locked,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        return ResponseEntity.ok(adminUserService.searchUsers(keyword, role, locked, PageRequest.of(safePage, safeSize)));
    }

    @PatchMapping("/{userId}/lock")
    public ResponseEntity<AdminUserSummaryDTO> updateUserLock(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserLockRequest request
    ) {
        Long currentAdminId = authUserProvider.getRequiredUserId();
        return ResponseEntity.ok(adminUserService.updateUserLock(userId, currentAdminId, request.locked()));
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<AdminUserSummaryDTO> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        Long currentAdminId = authUserProvider.getRequiredUserId();
        return ResponseEntity.ok(adminUserService.updateUserRole(userId, currentAdminId, request.role()));
    }

    @GetMapping("/{userId}/transactions")
    public ResponseEntity<List<WalletTransactionResponseDTO>> getUserTransactions(
            @PathVariable Long userId,
            @RequestParam(value = "limit", defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(adminUserService.getUserTransactions(userId, limit));
    }

    @PostMapping("/{userId}/wallet/adjust")
    public ResponseEntity<Map<String, Object>> adjustUserWallet(
            @PathVariable Long userId,
            @Valid @RequestBody AdjustUserWalletRequest request
    ) {
        String adminUsername = authUserProvider.getCurrentUsername();
        return ResponseEntity.ok(adminUserService.adjustUserWallet(userId, adminUsername, request));
    }
}
