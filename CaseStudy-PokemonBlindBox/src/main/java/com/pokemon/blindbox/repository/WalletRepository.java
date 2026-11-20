package com.pokemon.blindbox.repository;

import com.pokemon.blindbox.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserId(Long userId);

    List<Wallet> findByUserIdIn(Collection<Long> userIds);

    /**
     * Pessimistic Write Lock (SELECT ... FOR UPDATE) dùng kết hợp khóa theo thứ tự userId tăng dần
     * để triệt tiêu 100% nguy cơ InnoDB Deadlock khi giao dịch giữa 2 ví (P1-03).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.userId = :userId")
    Optional<Wallet> findByUserIdForUpdate(@Param("userId") Long userId);
}
