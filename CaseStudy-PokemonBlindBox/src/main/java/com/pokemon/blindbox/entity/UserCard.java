package com.pokemon.blindbox.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "user_cards",
    indexes = {
        @Index(name = "idx_user_cards_user_template", columnList = "user_id, card_template_id"),
        @Index(name = "idx_user_cards_pack", columnList = "booster_pack_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_template_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_card_template"))
    private CardTemplate cardTemplate;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "booster_pack_id", nullable = true, foreignKey = @ForeignKey(name = "fk_user_card_pack"))
    private BoosterPack boosterPack;

    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private Boolean isLocked = false;

    /**
     * Optimistic Locking (@Version) chống bán đúp thẻ (Double-selling) khi spam tạo Listing/Auction đồng thời.
     */
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
