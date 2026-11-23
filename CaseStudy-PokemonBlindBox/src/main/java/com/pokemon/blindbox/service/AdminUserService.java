package com.pokemon.blindbox.service;

import com.pokemon.blindbox.dto.request.AdjustUserWalletRequest;
import com.pokemon.blindbox.dto.response.AdminUserSummaryDTO;
import com.pokemon.blindbox.dto.response.WalletTransactionResponseDTO;
import com.pokemon.blindbox.entity.User;
import com.pokemon.blindbox.entity.Wallet;
import com.pokemon.blindbox.entity.WalletTransaction;
import com.pokemon.blindbox.entity.enums.Role;
import com.pokemon.blindbox.entity.enums.TransactionType;
import com.pokemon.blindbox.exception.BusinessValidationException;
import com.pokemon.blindbox.exception.ResourceNotFoundException;
import com.pokemon.blindbox.repository.UserCardRepository;
import com.pokemon.blindbox.repository.UserRepository;
import com.pokemon.blindbox.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final UserCardRepository userCardRepository;
    private final WalletService walletService;
    private final com.pokemon.blindbox.security.LockedUserKickFilter lockedUserKickFilter;

    /**
     * Danh sách người dùng phân trang kèm số dư ví & tổng số thẻ sở hữu.
     * Tối ưu theo lô (Batch IN query + GROUP BY) -> tuyệt đối không gây N+1 Query.
     */
    @Transactional(readOnly = true)
    public Page<AdminUserSummaryDTO> searchUsers(String keyword, Role role, Boolean locked, Pageable pageable) {
        String cleanKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        Page<User> userPage = userRepository.searchAdminUsers(cleanKeyword, role, locked, pageable);

        if (userPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Long> userIds = userPage.getContent().stream().map(User::getId).toList();

        Map<Long, Wallet> walletByUserId = walletRepository.findByUserIdIn(userIds)
                .stream()
                .collect(Collectors.toMap(Wallet::getUserId, Function.identity(), (a, b) -> a));

        Map<Long, Long> cardCountByUserId = userCardRepository.countTotalCardsByUserIds(userIds)
                .stream()
                .collect(Collectors.toMap(
                        UserCardRepository.UserCardCountProjection::getUserId,
                        UserCardRepository.UserCardCountProjection::getTotalCards,
                        (a, b) -> a
                ));

        List<AdminUserSummaryDTO> summaries = userPage.getContent().stream()
                .map(user -> toSummaryDto(user, walletByUserId.get(user.getId()), cardCountByUserId.getOrDefault(user.getId(), 0L)))
                .toList();

        return new PageImpl<>(summaries, pageable, userPage.getTotalElements());
    }

    /**
     * Khóa / Mở khóa tài khoản người dùng (không bao giờ xóa cứng User).
     */
    @Transactional
    public AdminUserSummaryDTO updateUserLock(Long targetUserId, Long currentAdminId, boolean locked) {
        if (targetUserId.equals(currentAdminId)) {
            throw new BusinessValidationException("Bạn không thể tự khóa tài khoản Quản trị của chính mình!");
        }
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id = " + targetUserId));

        user.setIsLocked(locked);
        User saved = userRepository.save(user);
        if (lockedUserKickFilter != null) {
            lockedUserKickFilter.evictUserLockCache(saved.getId(), locked);
        }
        log.info("Admin userId={} set isLocked={} for target userId={} ({})",
                currentAdminId, locked, saved.getId(), saved.getUsername());

        return buildSingleSummary(saved);
    }

    /**
     * Đổi quyền người dùng (ROLE_USER <-> ROLE_ADMIN).
     * Chặn tự hạ quyền và chặn hạ quyền Quản trị viên cuối cùng trong hệ thống.
     */
    @Transactional
    public AdminUserSummaryDTO updateUserRole(Long targetUserId, Long currentAdminId, Role newRole) {
        if (targetUserId.equals(currentAdminId) && newRole != Role.ROLE_ADMIN) {
            throw new BusinessValidationException("Bạn không thể tự hạ quyền tài khoản Quản trị của chính mình!");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id = " + targetUserId));

        if (user.getRole() == Role.ROLE_ADMIN && newRole != Role.ROLE_ADMIN) {
            long adminCount = userRepository.countByRole(Role.ROLE_ADMIN);
            if (adminCount <= 1) {
                throw new BusinessValidationException("Không thể hạ quyền tài khoản Quản trị viên (ROLE_ADMIN) cuối cùng trong hệ thống!");
            }
        }

        user.setRole(newRole);
        User saved = userRepository.save(user);
        log.info("Admin userId={} changed role of target userId={} ({}) to {}",
                currentAdminId, saved.getId(), saved.getUsername(), newRole);

        return buildSingleSummary(saved);
    }

    /**
     * Xem lịch sử giao dịch ví gần nhất của 1 người dùng (phục vụ CSKH / đối soát VNPay & đấu giá).
     */
    @Transactional(readOnly = true)
    public List<WalletTransactionResponseDTO> getUserTransactions(Long targetUserId, int limit) {
        if (!userRepository.existsById(targetUserId)) {
            throw new ResourceNotFoundException("Không tìm thấy người dùng với id = " + targetUserId);
        }
        return walletService.getRecentTransactionsByUserId(targetUserId, limit);
    }

    /**
     * Cộng / Trừ bù ví thủ công có khóa PESSIMISTIC_WRITE trên Wallet và bắt buộc ghi Audit Log ADMIN_ADJUSTMENT.
     */
    @Transactional
    public Map<String, Object> adjustUserWallet(Long targetUserId, String adminUsername, AdjustUserWalletRequest request) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id = " + targetUserId));

        Wallet wallet = walletRepository.findByUserIdForUpdate(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví của người dùng với userId = " + targetUserId));

        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);
        boolean isCredit = Boolean.TRUE.equals(request.credit());

        if (isCredit) {
            wallet.setBalance(wallet.getBalance().add(amount));
        } else {
            if (wallet.getBalance().compareTo(amount) < 0) {
                throw new BusinessValidationException("Số dư khả dụng của người dùng không đủ để trừ bù số tiền này!");
            }
            wallet.setBalance(wallet.getBalance().subtract(amount));
        }

        Wallet savedWallet = walletRepository.save(wallet);
        String actionLabel = isCredit ? "Cộng bù" : "Trừ bù";
        String description = String.format("[Admin %s - %s] %s", adminUsername, actionLabel, request.reason().trim());

        WalletTransaction tx = walletService.recordTransaction(
                savedWallet,
                TransactionType.ADMIN_ADJUSTMENT,
                amount,
                description
        );

        log.info("Admin {} performed {} {} VND for userId={} ({}), newBalance={}",
                adminUsername, actionLabel, amount, targetUserId, user.getUsername(), savedWallet.getBalance());

        return Map.of(
                "user", buildSingleSummary(user, savedWallet),
                "transaction", WalletTransactionResponseDTO.fromEntity(tx),
                "message", String.format("Đã %s %s ₫ cho tài khoản %s.", actionLabel.toLowerCase(), amount.toPlainString(), user.getUsername())
        );
    }

    private AdminUserSummaryDTO buildSingleSummary(User user) {
        Wallet wallet = walletRepository.findByUserId(user.getId()).orElse(null);
        return buildSingleSummary(user, wallet);
    }

    private AdminUserSummaryDTO buildSingleSummary(User user, Wallet wallet) {
        long totalCards = userCardRepository.countTotalCardsByUserIds(List.of(user.getId()))
                .stream()
                .findFirst()
                .map(UserCardRepository.UserCardCountProjection::getTotalCards)
                .orElse(0L);
        return toSummaryDto(user, wallet, totalCards);
    }

    private AdminUserSummaryDTO toSummaryDto(User user, Wallet wallet, long totalCardsOwned) {
        return AdminUserSummaryDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .isLocked(Boolean.TRUE.equals(user.getIsLocked()))
                .createdAt(user.getCreatedAt())
                .balance(wallet != null ? wallet.getBalance() : BigDecimal.ZERO)
                .lockedBalance(wallet != null ? wallet.getLockedBalance() : BigDecimal.ZERO)
                .totalCardsOwned(totalCardsOwned)
                .build();
    }
}
