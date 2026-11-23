package com.pokemon.blindbox.repository;

import com.pokemon.blindbox.entity.SanctuaryRandomDrop;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface SanctuaryRandomDropRepository extends JpaRepository<SanctuaryRandomDrop, Long> {

    Optional<SanctuaryRandomDrop> findFirstByUserIdAndClaimedFalseAndExpiresAtAfterOrderBySpawnedAtDesc(
            Long userId, Instant now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM SanctuaryRandomDrop d WHERE d.id = :id")
    Optional<SanctuaryRandomDrop> findByIdForUpdate(@Param("id") Long id);
}
