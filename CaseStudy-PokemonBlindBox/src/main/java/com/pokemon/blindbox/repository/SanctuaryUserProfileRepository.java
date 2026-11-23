package com.pokemon.blindbox.repository;

import com.pokemon.blindbox.entity.SanctuaryUserProfile;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SanctuaryUserProfileRepository extends JpaRepository<SanctuaryUserProfile, Long> {

    Optional<SanctuaryUserProfile> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM SanctuaryUserProfile p WHERE p.userId = :userId")
    Optional<SanctuaryUserProfile> findByUserIdForUpdate(@Param("userId") Long userId);
}
