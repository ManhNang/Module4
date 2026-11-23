package com.pokemon.blindbox.entity;

import com.pokemon.blindbox.entity.enums.RevealEffect;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(
    name = "rarities",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_rarities_name", columnNames = {"name"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rarity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tên độ hiếm: "Phổ biến", "Không phổ biến", "Hiếm", "EX", "Hyper Rare"
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Trọng số rơi thẻ phục vụ thuật toán Weighted Random Gacha
     */
    @Column(name = "drop_weight", nullable = false)
    private Integer dropWeight;

    /**
     * Mã màu hiển thị hiệu ứng UI (ví dụ: #9CA3AF, #10B981, #3B82F6, #F59E0B, #EC4899)
     */
    @Column(name = "color_hex", nullable = false, length = 7)
    private String colorHex;

    @Column(name = "is_holographic", nullable = false)
    @Builder.Default
    private Boolean isHolographic = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "reveal_effect", nullable = false, length = 32)
    @Builder.Default
    private RevealEffect revealEffect = RevealEffect.NONE;

    @Column(name = "sanctuary_coins_per_min", nullable = false)
    @Builder.Default
    private Integer sanctuaryCoinsPerMin = 2;

    @Column(name = "buyback_price", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal buybackPrice = new BigDecimal("5.00");
}
