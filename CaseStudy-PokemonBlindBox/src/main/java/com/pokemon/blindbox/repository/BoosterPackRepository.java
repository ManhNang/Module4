package com.pokemon.blindbox.repository;

import com.pokemon.blindbox.entity.BoosterPack;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoosterPackRepository extends JpaRepository<BoosterPack, Long> {

    @EntityGraph(attributePaths = {"boosterBox", "boosterBox.cardSet"})
    Optional<BoosterPack> findByBoosterBoxIdAndPositionInBox(Long boxId, Integer positionInBox);

    @EntityGraph(attributePaths = {"boosterBox", "boosterBox.cardSet"})
    Optional<BoosterPack> findWithBoxAndSetById(Long id);

    List<BoosterPack> findByBoosterBoxIdOrderByPositionInBoxAsc(Long boxId);
}
