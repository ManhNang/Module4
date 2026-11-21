package com.pokemon.blindbox.entity;

import com.pokemon.blindbox.entity.enums.SealedItemType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "user_sealed_items",
    indexes = {
        @Index(name = "idx_user_sealed_owner", columnList = "user_id, is_consumed, is_locked"),
        @Index(name = "idx_user_sealed_set_type", columnList = "user_id, card_set_id, item_type, is_consumed")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSealedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_set_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_sealed_set"))
    private CardSet cardSet;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 10)
    private SealedItemType itemType;

    @Column(name = "cards_per_pack", nullable = false)
    @Builder.Default
    private Integer cardsPerPack = 6;

    @Column(name = "packs_per_box", nullable = false)
    @Builder.Default
    private Integer packsPerBox = 20;

    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private Boolean isLocked = false;

    @Column(name = "is_consumed", nullable = false)
    @Builder.Default
    private Boolean isConsumed = false;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    @Column(name = "acquired_at", nullable = false, updatable = false)
    private LocalDateTime acquiredAt;

    @PrePersist
    protected void onCreate() {
        if (this.acquiredAt == null) {
            this.acquiredAt = LocalDateTime.now();
        }
    }
}
