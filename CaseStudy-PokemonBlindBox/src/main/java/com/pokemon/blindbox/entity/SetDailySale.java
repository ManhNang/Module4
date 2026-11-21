package com.pokemon.blindbox.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
    name = "set_daily_sales",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_set_daily_sales_set_date", columnNames = {"card_set_id", "sale_date"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SetDailySale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_set_id", nullable = false, foreignKey = @ForeignKey(name = "fk_set_daily_sales_set"))
    private CardSet cardSet;

    @Column(name = "sale_date", nullable = false)
    private LocalDate saleDate;

    @Column(name = "packs_sold", nullable = false)
    @Builder.Default
    private Integer packsSold = 0;

    @Column(name = "boxes_sold", nullable = false)
    @Builder.Default
    private Integer boxesSold = 0;
}
