package com.pokemon.blindbox.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "sanctuary_user_profiles",
        uniqueConstraints = @UniqueConstraint(name = "uk_sanctuary_profile_user", columnNames = {"user_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SanctuaryUserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "pack_shards", nullable = false)
    @Builder.Default
    private Integer packShards = 0;

    @Column(name = "daily_drop_date")
    private LocalDate dailyDropDate;

    @Column(name = "daily_drops_claimed", nullable = false)
    @Builder.Default
    private Integer dailyDropsClaimed = 0;

    @Column(name = "last_drop_at")
    private Instant lastDropAt;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;
}
