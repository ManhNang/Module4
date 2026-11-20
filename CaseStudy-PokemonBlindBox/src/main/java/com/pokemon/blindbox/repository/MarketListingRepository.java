package com.pokemon.blindbox.repository;

import com.pokemon.blindbox.entity.MarketListing;
import com.pokemon.blindbox.entity.enums.ListingStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarketListingRepository extends JpaRepository<MarketListing, Long> {

    @EntityGraph(attributePaths = {
            "userCard",
            "userCard.cardTemplate",
            "userCard.cardTemplate.cardSet",
            "userCard.cardTemplate.rarity",
            "userCard.cardTemplate.cardSpecies"
    })
    Optional<MarketListing> findWithDetailsById(Long id);

    /**
     * Tra cứu chéo trên Sàn P2P theo Bộ sưu tập (setId, setCode hoặc setName như "Delta Reign").
     * Sử dụng @EntityGraph để lấy trọn vẹn trong 1 câu lệnh SQL duy nhất (chống N+1 Query).
     */
    @EntityGraph(attributePaths = {
            "userCard",
            "userCard.cardTemplate",
            "userCard.cardTemplate.cardSet",
            "userCard.cardTemplate.rarity",
            "userCard.cardTemplate.cardSpecies"
    })
    @Query("""
            SELECT ml FROM MarketListing ml
            JOIN ml.userCard uc
            JOIN uc.cardTemplate ct
            JOIN ct.cardSet cs
            WHERE ml.status = :status
              AND (:setId IS NULL OR cs.id = :setId)
              AND (:setCode IS NULL OR cs.code = :setCode)
              AND (:setName IS NULL OR cs.name = :setName)
            ORDER BY ct.cardNumber ASC, ml.price ASC
            """)
    List<MarketListing> findActiveByCardSet(
            @Param("status") ListingStatus status,
            @Param("setId") Long setId,
            @Param("setCode") String setCode,
            @Param("setName") String setName
    );

    /**
     * Tra cứu chéo trên Sàn P2P theo Loài (bỏ LOWER() để tận dụng B-Tree Index utf8mb4_unicode_ci, P2-06).
     */
    @EntityGraph(attributePaths = {
            "userCard",
            "userCard.cardTemplate",
            "userCard.cardTemplate.cardSet",
            "userCard.cardTemplate.rarity",
            "userCard.cardTemplate.cardSpecies"
    })
    @Query("""
            SELECT ml FROM MarketListing ml
            JOIN ml.userCard uc
            JOIN uc.cardTemplate ct
            JOIN ct.cardSpecies sp
            WHERE ml.status = :status
              AND (:speciesId IS NULL OR sp.id = :speciesId)
              AND (:speciesName IS NULL OR sp.name = :speciesName)
            ORDER BY ct.cardNumber ASC, ml.price ASC
            """)
    List<MarketListing> findActiveBySpecies(
            @Param("status") ListingStatus status,
            @Param("speciesId") Long speciesId,
            @Param("speciesName") String speciesName
    );
}
