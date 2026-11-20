package com.pokemon.blindbox.service;

import com.pokemon.blindbox.dto.response.WalletResponseDTO;
import com.pokemon.blindbox.dto.response.WalletTransactionResponseDTO;
import com.pokemon.blindbox.entity.Wallet;
import com.pokemon.blindbox.entity.WalletTransaction;
import com.pokemon.blindbox.entity.enums.TransactionStatus;
import com.pokemon.blindbox.entity.enums.TransactionType;
import com.pokemon.blindbox.exception.ResourceNotFoundException;
import com.pokemon.blindbox.repository.WalletRepository;
import com.pokemon.blindbox.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * WalletService chuyên biệt quản lý Ví điện tử nội bộ & Audit Log giao dịch (SOLID SRP & DRY, P2-01):
 * - Tách rời hoàn toàn khỏi VNPayService.
 * - Đọc số dư ở chế độ @Transactional(readOnly = true) (P0-09).
 * - Khóa 2 ví theo thứ tự userId tăng dần (PESSIMISTIC_WRITE) để chống InnoDB Deadlock (P1-03).
 */
@Service
@RequiredArgsConstructor
public class WalletService {

    private static final int DEFAULT_TRANSACTION_LIMIT = 50;

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Transactional(readOnly = true)
    public WalletResponseDTO getWalletByUserId(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví của người dùng với userId = " + userId));
        return WalletResponseDTO.fromEntity(wallet);
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionResponseDTO> getRecentTransactionsByUserId(Long userId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return walletTransactionRepository
                .findByWalletUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, safeLimit))
                .getContent()
                .stream()
                .map(WalletTransactionResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionResponseDTO> getRecentTransactionsByUserId(Long userId) {
        return getRecentTransactionsByUserId(userId, DEFAULT_TRANSACTION_LIMIT);
    }

    @Transactional
    public Wallet createDefaultWallet(Long userId, BigDecimal initialBalance) {
        BigDecimal balance = (initialBalance != null ? initialBalance : BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        return walletRepository.save(Wallet.builder()
                .userId(userId)
                .balance(balance)
                .lockedBalance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .build());
    }

    /**
     * Khóa 2 ví bằng PESSIMISTIC_WRITE luôn theo thứ tự userId tăng dần để triệt tiêu 100% nguy cơ Deadlock (P1-03).
     */
    public Map<Long, Wallet> lockTwoWalletsInDeterministicOrder(Long userIdA, Long userIdB) {
        if (userIdA.equals(userIdB)) {
            Wallet single = walletRepository.findByUserIdForUpdate(userIdA)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví của người chơi với userId = " + userIdA));
            return Map.of(userIdA, single);
        }
        Long firstId = Math.min(userIdA, userIdB);
        Long secondId = Math.max(userIdA, userIdB);

        Wallet first = walletRepository.findByUserIdForUpdate(firstId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví của người chơi với userId = " + firstId));
        Wallet second = walletRepository.findByUserIdForUpdate(secondId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví của người chơi với userId = " + secondId));

        return Map.of(firstId, first, secondId, second);
    }

    /**
     * Ghi nhận lịch sử biến động ví dùng chung (DRY) cho mọi luồng nghiệp vụ:
     * Mua Pack/Box, Mua/Bán P2P, Tạm giữ/Hoàn cọc/Tất toán Đấu giá.
     */
    public WalletTransaction recordTransaction(
            Wallet wallet,
            TransactionType type,
            BigDecimal amount,
            String description
    ) {
        String txnRef = String.format(
                "%s_%d_%s",
                type.name(),
                wallet.getUserId(),
                UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase()
        );
        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .vnpTxnRef(txnRef)
                .type(type)
                .status(TransactionStatus.SUCCESS)
                .amount(amount)
                .balanceAfter(wallet.getBalance())
                .description(description)
                .build();
        return walletTransactionRepository.save(tx);
    }
}
