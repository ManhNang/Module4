package com.pokemon.blindbox.repository;

import com.pokemon.blindbox.entity.Auction;
import com.pokemon.blindbox.entity.enums.AuctionStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {

    @EntityGraph(attributePaths = {
            "userCard",
            "userCard.cardTemplate",
            "userCard.cardTemplate.cardSet",
            "userCard.cardTemplate.rarity",
            "userCard.cardTemplate.cardSpecies"
    })
    Optional<Auction> findWithDetailsById(Long id);

    /**
     * Khóa dòng Auction (PESSIMISTIC_WRITE) ngay đầu placeBid để đồng nhất thứ tự khóa:
     * Auction -> UserCard -> Wallet(minId) -> Wallet(maxId), triệt tiêu Deadlock với Settlement (GAP-02).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Auction a WHERE a.id = :id")
    Optional<Auction> findByIdForUpdate(@Param("id") Long id);

    /**
     * Khóa dòng Auction với SKIP LOCKED bên trong từng transaction con REQUIRES_NEW khi tất toán (GAP-01, GAP-02).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("""
            SELECT a FROM Auction a
            JOIN FETCH a.userCard uc
            WHERE a.id = :id AND a.status = com.pokemon.blindbox.entity.enums.AuctionStatus.ACTIVE
            """)
    Optional<Auction> findActiveByIdForUpdateSkipLocked(@Param("id") Long id);

    /**
     * Quét danh sách ID các phiên đấu giá đã hết hạn (read-only batch scan, khóa độc lập từng phiên ở transaction con).
     */
    @Query("""
            SELECT a.id FROM Auction a
            WHERE a.status = :status AND a.endTime <= :now
            ORDER BY a.endTime ASC
            """)
    List<Long> findExpiredActiveAuctionIds(
            @Param("status") AuctionStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    /**
     * Tra cứu chéo các phiên Đấu Giá đang diễn ra theo Bộ sưu tập (tận dụng B-Tree Index utf8mb4_unicode_ci, bỏ LOWER(), P2-06).
     */
    @EntityGraph(attributePaths = {
            "userCard",
            "userCard.cardTemplate",
            "userCard.cardTemplate.cardSet",
            "userCard.cardTemplate.rarity",
            "userCard.cardTemplate.cardSpecies"
    })
    @Query("""
            SELECT a FROM Auction a
            JOIN a.userCard uc
            JOIN uc.cardTemplate ct
            JOIN ct.cardSet cs
            WHERE a.status = :status
              AND (:setId IS NULL OR cs.id = :setId)
              AND (:setCode IS NULL OR cs.code = :setCode)
              AND (:setName IS NULL OR cs.name = :setName)
            ORDER BY a.endTime ASC, ct.cardNumber ASC
            """)
    List<Auction> findActiveByCardSet(
            @Param("status") AuctionStatus status,
            @Param("setId") Long setId,
            @Param("setCode") String setCode,
            @Param("setName") String setName
    );

    /**
     * Tra cứu chéo các phiên Đấu Giá đang diễn ra theo Loài (bỏ LOWER() để giữ B-Tree Index, P2-06).
     */
    @EntityGraph(attributePaths = {
            "userCard",
            "userCard.cardTemplate",
            "userCard.cardTemplate.cardSet",
            "userCard.cardTemplate.rarity",
            "userCard.cardTemplate.cardSpecies"
    })
    @Query("""
            SELECT a FROM Auction a
            JOIN a.userCard uc
            JOIN uc.cardTemplate ct
            JOIN ct.cardSpecies sp
            WHERE a.status = :status
              AND (:speciesId IS NULL OR sp.id = :speciesId)
              AND (:speciesName IS NULL OR sp.name = :speciesName)
            ORDER BY a.endTime ASC, ct.cardNumber ASC
            """)
    List<Auction> findActiveBySpecies(
            @Param("status") AuctionStatus status,
            @Param("speciesId") Long speciesId,
            @Param("speciesName") String speciesName
    );
}
