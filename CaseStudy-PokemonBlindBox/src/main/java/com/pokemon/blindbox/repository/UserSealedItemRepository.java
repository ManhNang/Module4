package com.pokemon.blindbox.repository;

import com.pokemon.blindbox.entity.UserSealedItem;
import com.pokemon.blindbox.entity.enums.SealedItemType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSealedItemRepository extends JpaRepository<UserSealedItem, Long> {

    interface SealedVaultAggProjection {
        Long getSetId();
        String getSetName();
        String getSetCode();
        String getPackImageUrl();
        String getBoxImageUrl();
        Integer getSetCardsPerPack();
        Integer getSetPacksPerBox();
        BigDecimal getPackPrice();
        SealedItemType getItemType();
        Boolean getIsLocked();
        Long getTotalCount();
        Long getFirstItemId();
    }

    @Query("""
            SELECT cs.id AS setId,
                    cs.name AS setName,
                    cs.code AS setCode,
                    cs.packImageUrl AS packImageUrl,
                    cs.boxImageUrl AS boxImageUrl,
                    cs.cardsPerPack AS setCardsPerPack,
                    cs.packsPerBox AS setPacksPerBox,
                    cs.packPrice AS packPrice,
                    u.itemType AS itemType,
                    u.isLocked AS isLocked,
                    COUNT(u.id) AS totalCount,
                    MIN(u.id) AS firstItemId
            FROM UserSealedItem u
            JOIN u.cardSet cs
            WHERE u.userId = :userId
              AND u.isConsumed = FALSE
            GROUP BY cs.id, cs.name, cs.code, cs.packImageUrl, cs.boxImageUrl, cs.cardsPerPack, cs.packsPerBox, cs.packPrice, u.itemType, u.isLocked
            ORDER BY cs.id ASC
            """)
    List<SealedVaultAggProjection> aggregateVaultByUserId(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"cardSet"})
    List<UserSealedItem> findByUserIdAndIsConsumedFalseOrderByAcquiredAtDesc(Long userId);

    @EntityGraph(attributePaths = {"cardSet"})
    List<UserSealedItem> findByUserIdAndIsConsumedFalseAndIsLockedFalseOrderByAcquiredAtDesc(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserSealedItem u JOIN FETCH u.cardSet WHERE u.id = :id")
    Optional<UserSealedItem> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT u FROM UserSealedItem u
            JOIN FETCH u.cardSet
            WHERE u.userId = :userId
              AND u.cardSet.id = :cardSetId
              AND u.itemType = :itemType
              AND u.isConsumed = FALSE
              AND u.isLocked = FALSE
            ORDER BY u.id ASC
            """)
    List<UserSealedItem> findAvailableForUpdate(
            @Param("userId") Long userId,
            @Param("cardSetId") Long cardSetId,
            @Param("itemType") SealedItemType itemType,
            Pageable pageable
    );
}
