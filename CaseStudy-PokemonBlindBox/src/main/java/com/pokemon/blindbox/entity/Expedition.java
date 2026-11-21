package com.pokemon.blindbox.entity;

import com.pokemon.blindbox.entity.enums.ExpeditionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "expeditions",
    indexes = {
        @Index(name = "idx_expeditions_user_status", columnList = "user_id, status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expedition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "zone_code", nullable = false, length = 30)
    private String zoneCode;

    @Column(name = "zone_name", nullable = false, length = 100)
    private String zoneName;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "reward_coins", nullable = false, precision = 15, scale = 2)
    private BigDecimal rewardCoins;

    @Column(name = "bonus_pack_chance", nullable = false)
    @Builder.Default
    private Integer bonusPackChance = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ExpeditionStatus status = ExpeditionStatus.IN_PROGRESS;

    @Column(name = "reward_pack_id")
    private Long rewardPackId;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    @OneToMany(mappedBy = "expedition", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExpeditionCard> cards = new ArrayList<>();
}
