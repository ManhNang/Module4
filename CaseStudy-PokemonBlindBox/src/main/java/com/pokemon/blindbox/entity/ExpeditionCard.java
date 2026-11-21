package com.pokemon.blindbox.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "expedition_cards",
    indexes = {
        @Index(name = "idx_exp_cards_exp", columnList = "expedition_id"),
        @Index(name = "idx_exp_cards_card", columnList = "user_card_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpeditionCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expedition_id", nullable = false, foreignKey = @ForeignKey(name = "fk_exp_card_expedition"))
    private Expedition expedition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_card_id", nullable = false, foreignKey = @ForeignKey(name = "fk_exp_card_user_card"))
    private UserCard userCard;
}
