package com.pokemon.blindbox.repository;

import com.pokemon.blindbox.entity.Rarity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RarityRepository extends JpaRepository<Rarity, Long> {
    List<Rarity> findAllByOrderByDropWeightDesc();
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
