package com.pokemon.blindbox.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
    name = "daily_quests",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_daily_quest_user_date_code", columnNames = {"user_id", "quest_date", "quest_code"})
    },
    indexes = {
        @Index(name = "idx_daily_quests_user_date", columnList = "user_id, quest_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyQuest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "quest_date", nullable = false)
    private LocalDate questDate;

    @Column(name = "quest_code", nullable = false, length = 40)
    private String questCode;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "target_count", nullable = false)
    @Builder.Default
    private Integer targetCount = 1;

    @Column(name = "current_count", nullable = false)
    @Builder.Default
    private Integer currentCount = 0;

    @Column(name = "reward_coins", nullable = false, precision = 15, scale = 2)
    private BigDecimal rewardCoins;

    @Column(name = "is_claimed", nullable = false)
    @Builder.Default
    private Boolean isClaimed = false;
}
