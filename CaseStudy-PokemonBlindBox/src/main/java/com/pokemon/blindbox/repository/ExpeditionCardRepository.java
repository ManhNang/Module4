package com.pokemon.blindbox.repository;

import com.pokemon.blindbox.entity.ExpeditionCard;
import com.pokemon.blindbox.entity.enums.ExpeditionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpeditionCardRepository extends JpaRepository<ExpeditionCard, Long> {

    @Query("""
            SELECT COUNT(ec) > 0 FROM ExpeditionCard ec
            WHERE ec.userCard.id = :userCardId AND ec.expedition.status = :status
            """)
    boolean existsByUserCardIdAndExpeditionStatus(
            @Param("userCardId") Long userCardId,
            @Param("status") ExpeditionStatus status
    );
}
