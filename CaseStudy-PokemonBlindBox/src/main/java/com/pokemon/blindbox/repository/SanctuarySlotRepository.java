package com.pokemon.blindbox.repository;

import com.pokemon.blindbox.entity.SanctuarySlot;
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
public interface SanctuarySlotRepository extends JpaRepository<SanctuarySlot, Long> {

    @EntityGraph(attributePaths = {
            "userCard", "userCard.cardTemplate", "userCard.cardTemplate.cardSet",
            "userCard.cardTemplate.rarity", "userCard.cardTemplate.cardSpecies"
    })
    List<SanctuarySlot> findByUserIdOrderByCreatedAtAsc(Long userId);

    long countByUserId(Long userId);

    boolean existsByUserCardId(Long userCardId);

    void deleteByUserCardId(Long userCardId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s FROM SanctuarySlot s
            JOIN FETCH s.userCard uc
            JOIN FETCH uc.cardTemplate ct
            JOIN FETCH ct.rarity
            WHERE s.id = :slotId
            """)
    Optional<SanctuarySlot> findByIdForUpdate(@Param("slotId") Long slotId);
}
