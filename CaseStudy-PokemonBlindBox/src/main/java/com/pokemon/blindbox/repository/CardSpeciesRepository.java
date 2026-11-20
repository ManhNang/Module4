package com.pokemon.blindbox.repository;

import com.pokemon.blindbox.entity.CardSpecies;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardSpeciesRepository extends JpaRepository<CardSpecies, Long> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
