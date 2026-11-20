package com.pokemon.blindbox.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(
    name = "card_species",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_card_species_name", columnNames = {"name"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardSpecies implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tên loài / phân loại: "Hầm hào huyền thoại", "Steelix", "Rayquaza", ...
     */
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "is_beta", nullable = false)
    @Builder.Default
    private Boolean isBeta = false;
}
