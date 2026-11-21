package com.pokemon.blindbox.entity;

import com.pokemon.blindbox.entity.enums.ListingStatus;
import com.pokemon.blindbox.entity.enums.MarketItemType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "market_listings",
    indexes = {
        @Index(name = "idx_market_listings_status_created", columnList = "status, created_at"),
        @Index(name = "idx_market_listings_user_card", columnList = "user_card_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 10)
    @Builder.Default
    private MarketItemType itemType = MarketItemType.CARD;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_card_id", nullable = true, foreignKey = @ForeignKey(name = "fk_market_listing_user_card"))
    private UserCard userCard;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "sealed_item_id", nullable = true)
    private UserSealedItem sealedItem;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ListingStatus status = ListingStatus.ACTIVE;

    @Column(name = "buyer_id")
    private Long buyerId;

    @Column(name = "sold_at")
    private LocalDateTime soldAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
