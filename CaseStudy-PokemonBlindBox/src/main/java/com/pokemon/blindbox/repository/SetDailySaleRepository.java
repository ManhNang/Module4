package com.pokemon.blindbox.repository;

import com.pokemon.blindbox.entity.SetDailySale;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
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

    @Modifying
    @Query(value = """
            INSERT IGNORE INTO set_daily_sales (card_set_id, sale_date, packs_sold, boxes_sold)
            VALUES (:setId, :saleDate, 0, 0)
            """, nativeQuery = true)
    int initDailySaleIfAbsent(@Param("setId") Long setId, @Param("saleDate") LocalDate saleDate);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE SetDailySale s
            SET s.packsSold = s.packsSold + :qty
            WHERE s.cardSet.id = :setId
              AND s.saleDate = :saleDate
              AND s.packsSold + :qty <= :limit
            """)
    int incrementPacksSoldAtomically(
            @Param("setId") Long setId,
            @Param("saleDate") LocalDate saleDate,
            @Param("qty") int qty,
            @Param("limit") int limit
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE SetDailySale s
            SET s.boxesSold = s.boxesSold + :qty
            WHERE s.cardSet.id = :setId
              AND s.saleDate = :saleDate
              AND s.boxesSold + :qty <= :limit
            """)
    int incrementBoxesSoldAtomically(
            @Param("setId") Long setId,
            @Param("saleDate") LocalDate saleDate,
            @Param("qty") int qty,
            @Param("limit") int limit
    );
}
