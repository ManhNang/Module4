package com.pokemon.blindbox.repository;

import com.pokemon.blindbox.entity.CardSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardSetRepository extends JpaRepository<CardSet, Long> {
    Optional<CardSet> findByCode(String code);
    boolean existsByCode(String code);
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
