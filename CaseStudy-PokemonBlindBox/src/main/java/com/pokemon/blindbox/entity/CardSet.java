package com.pokemon.blindbox.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "card_sets",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_card_sets_code", columnNames = {"code"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardSet implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name; // Ví dụ: "Delta Reign"

    @Column(name = "code", nullable = false, length = 20)
    private String code; // Ví dụ: "DLR"

    /**
     * Tổng số thẻ chuẩn của bộ (ví dụ: 103).
     * Bắt buộc lưu kiểu Integer để phân loại thẻ chuẩn (cardNumber <= baseSetTotal)
     * và thẻ Secret vượt khung (cardNumber > baseSetTotal).
     */
    @Column(name = "base_set_total", nullable = false)
    private Integer baseSetTotal;

    @Column(name = "cards_per_pack", nullable = false)
    private Integer cardsPerPack;

    @Column(name = "packs_per_box", nullable = false)
    private Integer packsPerBox;

    @Column(name = "pack_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal packPrice;

    @Column(name = "box_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal boxPrice;

    @Column(name = "daily_pack_limit", nullable = false)
    @Builder.Default
    private Integer dailyPackLimit = 200;

    @Column(name = "daily_box_limit", nullable = false)
    @Builder.Default
    private Integer dailyBoxLimit = 15;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

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
