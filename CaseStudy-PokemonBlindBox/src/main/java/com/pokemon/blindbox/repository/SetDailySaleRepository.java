package com.pokemon.blindbox.repository;

import com.pokemon.blindbox.entity.SetDailySale;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SetDailySaleRepository extends JpaRepository<SetDailySale, Long> {

    Optional<SetDailySale> findByCardSetIdAndSaleDate(Long cardSetId, LocalDate saleDate);

    List<SetDailySale> findBySaleDate(LocalDate saleDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SetDailySale s WHERE s.cardSet.id = :cardSetId AND s.saleDate = :saleDate")
    Optional<SetDailySale> findByCardSetIdAndSaleDateForUpdate(
            @Param("cardSetId") Long cardSetId,
            @Param("saleDate") LocalDate saleDate
    );
}
