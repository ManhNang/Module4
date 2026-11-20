package com.pokemon.blindbox.entity;

import com.pokemon.blindbox.exception.BusinessValidationException;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "wallets",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_wallets_user_id", columnNames = {"user_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "locked_balance", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal lockedBalance = BigDecimal.ZERO;

    /**
     * Optimistic Locking (@Version) bắt buộc theo .agent/rules.md
     * để chống Race Condition khi nạp điểm VNPay, mua Pack/Box hoặc đặt giá đấu giá.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void credit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("Số tiền cộng vào ví phải lớn hơn 0.");
        }
        this.balance = this.balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("Số tiền trừ khỏi ví phải lớn hơn 0.");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new BusinessValidationException("Số dư ví không đủ để thực hiện giao dịch.");
        }
        this.balance = this.balance.subtract(amount);
    }

    /**
     * Tạm giữ tiền trong ví khi người chơi đặt bước giá mới hợp lệ trên phiên đấu giá.
     */
    public void holdForBid(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("Số tiền đặt giá phải lớn hơn 0.");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new BusinessValidationException("Số dư khả dụng trong ví không đủ để đặt giá.");
        }
        this.balance = this.balance.subtract(amount);
        this.lockedBalance = this.lockedBalance.add(amount);
    }

    /**
     * Hoàn trả lại số tiền tạm giữ về số dư khả dụng khi bị người chơi khác trả giá cao hơn.
     */
    public void refundHeldBid(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("Số tiền hoàn cọc phải lớn hơn 0.");
        }
        if (this.lockedBalance.compareTo(amount) < 0) {
            throw new BusinessValidationException("Số dư tạm giữ không đủ để hoàn trả.");
        }
        this.lockedBalance = this.lockedBalance.subtract(amount);
        this.balance = this.balance.add(amount);
    }

    /**
     * Tất toán số tiền đang tạm giữ (lockedBalance) khi người chơi thắng phiên đấu giá.
     */
    public void settleHeldBid(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("Số tiền tất toán phải lớn hơn 0.");
        }
        if (this.lockedBalance.compareTo(amount) < 0) {
            throw new BusinessValidationException("Số dư tạm giữ không đủ để tất toán phiên đấu giá.");
        }
        this.lockedBalance = this.lockedBalance.subtract(amount);
    }
}
