package com.pokemon.blindbox.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "card_templates",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_card_set_number", columnNames = {"set_id", "card_number"})
    },
    indexes = {
        @Index(name = "idx_card_templates_set_rarity", columnList = "set_id, rarity_id"),
        @Index(name = "idx_card_templates_species", columnList = "species_id"),
        @Index(name = "idx_card_templates_set_number", columnList = "set_id, card_number")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardTemplate implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /**
     * Số thứ tự thẻ trong bộ (kiểu Integer: 52, 98, 141...) để ORDER BY chính xác.
     * Tuyệt đối không lưu dạng chuỗi "098 / 103" trong DB.
     */
    @Column(name = "card_number", nullable = false)
    private Integer cardNumber;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "set_id", nullable = false, foreignKey = @ForeignKey(name = "fk_card_template_set"))
    private CardSet cardSet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rarity_id", nullable = false, foreignKey = @ForeignKey(name = "fk_card_template_rarity"))
    private Rarity rarity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "species_id", nullable = false, foreignKey = @ForeignKey(name = "fk_card_template_species"))
    private CardSpecies cardSpecies;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
