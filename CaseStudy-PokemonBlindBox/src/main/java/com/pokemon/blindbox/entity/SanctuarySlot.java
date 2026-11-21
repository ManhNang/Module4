package com.pokemon.blindbox.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
    name = "sanctuary_slots",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_sanctuary_slot_card", columnNames = {"user_card_id"})
    },
    indexes = {
        @Index(name = "idx_sanctuary_slots_user", columnList = "user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SanctuarySlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_card_id", nullable = false, foreignKey = @ForeignKey(name = "fk_sanctuary_slot_user_card"))
    private UserCard userCard;

    @Column(name = "last_harvest_at", nullable = false)
    private Instant lastHarvestAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) this.createdAt = now;
        if (this.lastHarvestAt == null) this.lastHarvestAt = now;
    }
}
