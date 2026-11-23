package com.pokemon.blindbox.repository;

import com.pokemon.blindbox.entity.MarketListing;
import com.pokemon.blindbox.entity.enums.ListingStatus;
import org.springframework.data.domain.Pageable;
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
            "userCard.cardTemplate.cardSpecies",
            "sealedItem",
            "sealedItem.cardSet"
    })
    Optional<MarketListing> findWithDetailsById(Long id);

    /**
     * Tra cứu chéo trên Chợ theo Bộ sưu tập (hỗ trợ cả Thẻ bài, Pack lẻ & Box nguyên seal).
     */
    @EntityGraph(attributePaths = {
            "userCard",
            "userCard.cardTemplate",
            "userCard.cardTemplate.cardSet",
            "userCard.cardTemplate.rarity",
            "userCard.cardTemplate.cardSpecies",
            "sealedItem",
            "sealedItem.cardSet"
    })
    @Query("""
            SELECT ml FROM MarketListing ml
            LEFT JOIN ml.userCard uc
            LEFT JOIN uc.cardTemplate ct
            LEFT JOIN ct.cardSet cs
            LEFT JOIN ml.sealedItem si
            LEFT JOIN si.cardSet scs
            WHERE ml.status = :status
              AND (:setId IS NULL OR cs.id = :setId OR scs.id = :setId)
              AND (:setCode IS NULL OR cs.code = :setCode OR scs.code = :setCode)
              AND (:setName IS NULL OR cs.name = :setName OR scs.name = :setName)
            ORDER BY ml.createdAt DESC, ml.price ASC
            """)
    List<MarketListing> findActiveByCardSet(
            @Param("status") ListingStatus status,
            @Param("setId") Long setId,
            @Param("setCode") String setCode,
            @Param("setName") String setName
    );

    @EntityGraph(attributePaths = {
            "userCard",
            "userCard.cardTemplate",
            "userCard.cardTemplate.cardSet",
            "userCard.cardTemplate.rarity",
            "userCard.cardTemplate.cardSpecies",
            "sealedItem",
            "sealedItem.cardSet"
    })
    @Query("""
            SELECT ml FROM MarketListing ml
            LEFT JOIN ml.userCard uc
            LEFT JOIN uc.cardTemplate ct
            LEFT JOIN ct.cardSet cs
            LEFT JOIN ml.sealedItem si
            LEFT JOIN si.cardSet scs
            WHERE ml.status = :status
              AND (:setId IS NULL OR cs.id = :setId OR scs.id = :setId)
              AND (:setCode IS NULL OR cs.code = :setCode OR scs.code = :setCode)
              AND (:setName IS NULL OR cs.name = :setName OR scs.name = :setName)
            ORDER BY ml.createdAt DESC, ml.price ASC
            """)
    List<MarketListing> findActiveByCardSet(
            @Param("status") ListingStatus status,
            @Param("setId") Long setId,
            @Param("setCode") String setCode,
            @Param("setName") String setName,
            Pageable pageable
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
            @Param("speciesName") String speciesName,
            Pageable pageable
    );
}
