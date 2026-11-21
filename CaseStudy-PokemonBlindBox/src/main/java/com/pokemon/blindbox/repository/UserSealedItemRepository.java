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

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSealedItemRepository extends JpaRepository<UserSealedItem, Long> {

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
