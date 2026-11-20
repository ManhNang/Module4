package com.pokemon.blindbox.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "auction_bids",
    indexes = {
        @Index(name = "idx_auction_bids_auction_time", columnList = "auction_id, bid_timestamp")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuctionBid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auction_id", nullable = false, foreignKey = @ForeignKey(name = "fk_auction_bid_auction"))
    private Auction auction;

    @Column(name = "bidder_id", nullable = false)
    private Long bidderId;

    @Column(name = "masked_bidder_name", nullable = false, length = 60)
    private String maskedBidderName;

    @Column(name = "bid_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal bidAmount;

    @Column(name = "bid_timestamp", nullable = false, updatable = false)
    private LocalDateTime bidTimestamp;

    @PrePersist
    protected void onCreate() {
        if (this.bidTimestamp == null) {
            this.bidTimestamp = LocalDateTime.now();
        }
    }
}
