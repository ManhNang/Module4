package com.pokemon.blindbox.repository;

import com.pokemon.blindbox.entity.Expedition;
import com.pokemon.blindbox.entity.enums.ExpeditionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpeditionRepository extends JpaRepository<Expedition, Long> {

    @EntityGraph(attributePaths = {
            "cards", "cards.userCard", "cards.userCard.cardTemplate",
            "cards.userCard.cardTemplate.cardSet", "cards.userCard.cardTemplate.rarity",
            "cards.userCard.cardTemplate.cardSpecies"
    })
    List<Expedition> findByUserIdAndStatusOrderByStartedAtDesc(Long userId, ExpeditionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e FROM Expedition e
            LEFT JOIN FETCH e.cards ec
            LEFT JOIN FETCH ec.userCard uc
            WHERE e.id = :id
            """)
    Optional<Expedition> findByIdForUpdate(@Param("id") Long id);
}
