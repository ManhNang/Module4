package com.pokemon.blindbox.entity;

import com.pokemon.blindbox.entity.enums.BoxStatus;
import com.pokemon.blindbox.entity.enums.PackStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "booster_boxes",
    indexes = {
        @Index(name = "idx_booster_boxes_set_status", columnList = "set_id, status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoosterBox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "box_code", nullable = false, unique = true, length = 50)
    private String boxCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "set_id", nullable = false, foreignKey = @ForeignKey(name = "fk_booster_box_set"))
    private CardSet cardSet;

    @Column(name = "total_packs", nullable = false)
    private Integer totalPacks;

    @Column(name = "remaining_packs", nullable = false)
    private Integer remainingPacks;

    @Column(name = "can_buy_whole_box", nullable = false)
    @Builder.Default
    private boolean canBuyWholeBox = true;

    @Column(name = "snapshot_box_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal snapshotBoxPrice;

    @Column(name = "snapshot_cards_per_pack", nullable = false)
    @Builder.Default
    private Integer snapshotCardsPerPack = 5;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private BoxStatus status = BoxStatus.AVAILABLE;

    /**
     * Optimistic Locking chống Race Condition khi nhiều người chơi cùng mua Pack lẻ
     * hoặc mua nguyên Box cùng lúc.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(mappedBy = "boosterBox", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("positionInBox ASC")
    @Builder.Default
    private List<BoosterPack> packs = new ArrayList<>();

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

    /**
     * Factory method khởi tạo BoosterBox và đóng băng cấu hình cardsPerPack của CardSet
     * vào từng BoosterPack con (Configuration Snapshot).
     */
    public static BoosterBox createFromCardSet(CardSet cardSet, String boxCode) {
        int packsCount = cardSet.getPacksPerBox();
        int snapshotCardsCount = cardSet.getCardsPerPack();

        BoosterBox box = BoosterBox.builder()
                .boxCode(boxCode)
                .cardSet(cardSet)
                .totalPacks(packsCount)
                .remainingPacks(packsCount)
                .canBuyWholeBox(true)
                .snapshotBoxPrice(cardSet.getBoxPrice())
                .snapshotCardsPerPack(snapshotCardsCount)
                .status(BoxStatus.AVAILABLE)
                .build();

        for (int pos = 1; pos <= packsCount; pos++) {
            BoosterPack pack = BoosterPack.builder()
                    .boosterBox(box)
                    .positionInBox(pos)
                    .snapshotCardsPerPack(snapshotCardsCount)
                    .snapshotPackPrice(cardSet.getPackPrice())
                    .status(PackStatus.AVAILABLE)
                    .build();
            box.getPacks().add(pack);
        }

        return box;
    }
}
