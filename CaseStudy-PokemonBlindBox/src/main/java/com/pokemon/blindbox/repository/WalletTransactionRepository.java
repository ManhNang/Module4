package com.pokemon.blindbox.repository;

import com.pokemon.blindbox.entity.WalletTransaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    @EntityGraph(attributePaths = {"wallet"})
    Optional<WalletTransaction> findByVnpTxnRef(String vnpTxnRef);

    /**
     * Pessimistic Write Lock (SELECT ... FOR UPDATE) đảm bảo Idempotency tuyệt đối cho VNPay IPN
     * trên cả single-instance và multi-instance (P0-04).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT tx FROM WalletTransaction tx JOIN FETCH tx.wallet WHERE tx.vnpTxnRef = :vnpTxnRef")
    Optional<WalletTransaction> findByVnpTxnRefForUpdate(@Param("vnpTxnRef") String vnpTxnRef);

    boolean existsByVnpTxnRef(String vnpTxnRef);

    List<WalletTransaction> findByWalletUserIdOrderByCreatedAtDesc(Long userId);

    Page<WalletTransaction> findByWalletUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
