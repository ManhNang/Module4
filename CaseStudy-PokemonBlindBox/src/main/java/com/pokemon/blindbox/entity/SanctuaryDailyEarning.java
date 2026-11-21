package com.pokemon.blindbox.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
    name = "sanctuary_daily_earnings",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_sanctuary_earnings_user_date", columnNames = {"user_id", "earning_date"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SanctuaryDailyEarning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "earning_date", nullable = false)
    private LocalDate earningDate;

    @Column(name = "coins_earned", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal coinsEarned = BigDecimal.ZERO;
}
