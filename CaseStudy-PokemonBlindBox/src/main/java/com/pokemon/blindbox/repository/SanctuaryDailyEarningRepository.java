package com.pokemon.blindbox.repository;

import com.pokemon.blindbox.entity.SanctuaryDailyEarning;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface SanctuaryDailyEarningRepository extends JpaRepository<SanctuaryDailyEarning, Long> {

    Optional<SanctuaryDailyEarning> findByUserIdAndEarningDate(Long userId, LocalDate earningDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM SanctuaryDailyEarning e WHERE e.userId = :userId AND e.earningDate = :earningDate")
    Optional<SanctuaryDailyEarning> findByUserIdAndEarningDateForUpdate(
            @Param("userId") Long userId,
            @Param("earningDate") LocalDate earningDate
    );

    @Modifying
    @Query(value = """
            INSERT IGNORE INTO sanctuary_daily_earnings (user_id, earning_date, coins_earned)
            VALUES (:userId, :earningDate, 0.00)
            """, nativeQuery = true)
    int insertIgnoreDailyEarning(
            @Param("userId") Long userId,
            @Param("earningDate") LocalDate earningDate
    );
}
