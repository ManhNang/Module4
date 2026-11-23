package com.pokemon.blindbox.repository;

import com.pokemon.blindbox.entity.DailyQuest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyQuestRepository extends JpaRepository<DailyQuest, Long> {

    List<DailyQuest> findByUserIdAndQuestDateOrderByIdAsc(Long userId, LocalDate questDate);

    Optional<DailyQuest> findByUserIdAndQuestDateAndQuestCode(Long userId, LocalDate questDate, String questCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM DailyQuest q WHERE q.id = :id")
    Optional<DailyQuest> findByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Query(value = """
            INSERT IGNORE INTO daily_quests
                (user_id, quest_date, quest_code, title, target_count, current_count, reward_coins, is_claimed)
            VALUES (:userId, :questDate, :questCode, :title, :targetCount, 0, :rewardCoins, 0)
            """, nativeQuery = true)
    int insertIgnoreQuest(
            @Param("userId") Long userId,
            @Param("questDate") LocalDate questDate,
            @Param("questCode") String questCode,
            @Param("title") String title,
            @Param("targetCount") int targetCount,
            @Param("rewardCoins") BigDecimal rewardCoins
    );
}
