package com.pokemon.blindbox.repository;

import com.pokemon.blindbox.entity.BoosterBox;
import com.pokemon.blindbox.entity.enums.BoxStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoosterBoxRepository extends JpaRepository<BoosterBox, Long> {

    boolean existsByBoxCode(String boxCode);

    boolean existsByCardSetId(Long setId);

    @EntityGraph(attributePaths = {"cardSet"})
    List<BoosterBox> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = {"cardSet"})
    Page<BoosterBox> findAllByOrderByIdAsc(Pageable pageable);

    @EntityGraph(attributePaths = {"cardSet"})
    List<BoosterBox> findByCardSetIdOrderByIdDesc(Long setId);

    @EntityGraph(attributePaths = {"cardSet"})
    List<BoosterBox> findByCardSetIdAndStatusOrderByIdAsc(Long setId, BoxStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BoosterBox b WHERE b.id = :id")
    Optional<BoosterBox> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b FROM BoosterBox b
            WHERE b.cardSet.id = :setId
              AND b.status = com.pokemon.blindbox.entity.enums.BoxStatus.AVAILABLE
              AND b.remainingPacks = b.totalPacks
            ORDER BY b.id DESC
            """)
    List<BoosterBox> findSealedAvailableBoxesBySetIdForUpdate(@Param("setId") Long setId, Pageable pageable);

    /**
     * Atomic SQL Update giảm remaining_packs và khóa can_buy_whole_box mà không gây Hot-Row @Version
     * contention khi nhiều người chơi đồng thời mua các gói lẻ khác nhau trong cùng 1 hộp (P1-02).
     */
    @Modifying(clearAutomatically = false, flushAutomatically = true)
    @Query("""
            UPDATE BoosterBox b
            SET b.remainingPacks = b.remainingPacks - 1,
                b.canBuyWholeBox = false,
                b.status = CASE WHEN b.remainingPacks - 1 <= 0
                                THEN com.pokemon.blindbox.entity.enums.BoxStatus.SOLD_OUT
                                ELSE com.pokemon.blindbox.entity.enums.BoxStatus.PARTIALLY_SOLD END,
                b.version = b.version + 1
            WHERE b.id = :boxId AND b.remainingPacks > 0
            """)
    int decrementRemainingPackAtomically(@Param("boxId") Long boxId);
}
