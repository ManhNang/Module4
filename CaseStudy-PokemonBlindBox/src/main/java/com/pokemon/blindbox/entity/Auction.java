package com.pokemon.blindbox.entity;

import com.pokemon.blindbox.entity.enums.AuctionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "auctions",
    indexes = {
        @Index(name = "idx_auctions_status_end_time", columnList = "status, end_time"),
        @Index(name = "idx_auctions_user_card", columnList = "user_card_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_card_id", nullable = false, foreignKey = @ForeignKey(name = "fk_auction_user_card"))
    private UserCard userCard;

    @Column(name = "starting_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal startingPrice;

    @Column(name = "min_bid_increment", nullable = false, precision = 15, scale = 2)
    private BigDecimal minBidIncrement;

    @Column(name = "current_highest_bid", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentHighestBid;

    @Column(name = "highest_bidder_id")
    private Long highestBidderId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AuctionStatus status = AuctionStatus.ACTIVE;

    /**
     * Optimistic Locking (@Version) bắt buộc theo .agent/rules.md
     * để quản lý giá cao nhất hiện tại (currentHighestBid) và chống đè giá đồng thời.
     */
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
