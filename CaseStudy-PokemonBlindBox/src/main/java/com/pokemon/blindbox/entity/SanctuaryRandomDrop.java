package com.pokemon.blindbox.entity;

import com.pokemon.blindbox.entity.enums.SanctuaryDropType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "sanctuary_random_drops",
        indexes = @Index(name = "idx_sanctuary_drops_user_active", columnList = "user_id, claimed, expires_at")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SanctuaryRandomDrop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "drop_type", nullable = false, length = 32)
    private SanctuaryDropType dropType;

    @Column(name = "pos_x", nullable = false)
    private Double posX;

    @Column(name = "pos_y", nullable = false)
    private Double posY;

    @Column(name = "spawned_at", nullable = false)
    private Instant spawnedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "claimed", nullable = false)
    @Builder.Default
    private Boolean claimed = false;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "reward_type", length = 20)
    private String rewardType;

    @Column(name = "reward_amount")
    private Integer rewardAmount;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;
}
