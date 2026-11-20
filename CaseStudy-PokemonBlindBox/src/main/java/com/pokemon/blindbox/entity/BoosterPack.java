package com.pokemon.blindbox.entity;

import com.pokemon.blindbox.entity.enums.PackStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "booster_packs",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_box_pack_position", columnNames = {"box_id", "position_in_box"})
    },
    indexes = {
        @Index(name = "idx_booster_packs_box_status", columnList = "box_id, status"),
        @Index(name = "idx_booster_packs_buyer_status", columnList = "buyer_id, status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoosterPack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "box_id", nullable = false, foreignKey = @ForeignKey(name = "fk_booster_pack_box"))
    private BoosterBox boosterBox;

    /**
     * Vị trí của gói bài trên khay Booster Box (ví dụ: 1..30)
     */
    @Column(name = "position_in_box", nullable = false)
    private Integer positionInBox;

    /**
     * Đóng băng số lượng thẻ mỗi gói ngay tại thời điểm sinh Pack từ CardSet.
     * Khi mở Pack (openPack), Gacha Engine chỉ đọc từ trường này.
     */
    @Column(name = "snapshot_cards_per_pack", nullable = false)
    private Integer snapshotCardsPerPack;

    @Column(name = "snapshot_pack_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal snapshotPackPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PackStatus status = PackStatus.AVAILABLE;

    @Column(name = "buyer_id")
    private Long buyerId;

    @Column(name = "purchased_at")
    private LocalDateTime purchasedAt;

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    /**
     * Optimistic Locking chống 2 người chơi cùng mua 1 vị trí Pack (positionInBox)
     * hoặc mở cùng 1 Pack nhiều lần.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
