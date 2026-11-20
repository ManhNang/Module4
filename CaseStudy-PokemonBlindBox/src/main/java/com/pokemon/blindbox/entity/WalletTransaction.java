package com.pokemon.blindbox.entity;

import com.pokemon.blindbox.entity.enums.TransactionStatus;
import com.pokemon.blindbox.entity.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "wallet_transactions",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_wallet_tx_vnp_txn_ref", columnNames = {"vnp_txn_ref"})
    },
    indexes = {
        @Index(name = "idx_wallet_tx_wallet_status", columnList = "wallet_id, status"),
        @Index(name = "idx_wallet_tx_created_at", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false, foreignKey = @ForeignKey(name = "fk_wallet_tx_wallet"))
    private Wallet wallet;

    /**
     * Mã tham chiếu giao dịch duy nhất (vnp_TxnRef) đảm bảo tính Idempotency
     * khi nhận Webhook IPN từ VNPay.
     */
    @Column(name = "vnp_txn_ref", nullable = false, length = 64)
    private String vnpTxnRef;

    @Column(name = "vnp_transaction_no", length = 64)
    private String vnpTransactionNo;

    @Column(name = "vnp_bank_code", length = 32)
    private String vnpBankCode;

    @Column(name = "vnp_response_code", length = 16)
    private String vnpResponseCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "description", length = 255)
    private String description;

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
}
