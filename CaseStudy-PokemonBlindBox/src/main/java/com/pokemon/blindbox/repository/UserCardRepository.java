package com.pokemon.blindbox.repository;

import com.pokemon.blindbox.entity.UserCard;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserCardRepository extends JpaRepository<UserCard, Long> {

    @EntityGraph(attributePaths = {"cardTemplate", "cardTemplate.cardSet", "cardTemplate.rarity", "cardTemplate.cardSpecies"})
    Optional<UserCard> findWithTemplateDetailsById(Long id);

    /**
     * Pessimistic Write Lock (SELECT ... FOR UPDATE) chống Double-selling khi tạo Listing/Auction đồng thời (P0-02).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT uc FROM UserCard uc
            JOIN FETCH uc.cardTemplate ct
            JOIN FETCH ct.cardSet
            JOIN FETCH ct.rarity
            JOIN FETCH ct.cardSpecies
            WHERE uc.id = :id
            """)
    Optional<UserCard> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"cardTemplate", "cardTemplate.cardSet", "cardTemplate.rarity", "cardTemplate.cardSpecies"})
    List<UserCard> findByBoosterPackIdOrderByIdAsc(Long boosterPackId);

    @EntityGraph(attributePaths = {"cardTemplate", "cardTemplate.cardSet", "cardTemplate.rarity", "cardTemplate.cardSpecies"})
    List<UserCard> findByUserIdOrderByAcquiredAtDesc(Long userId);

    @EntityGraph(attributePaths = {"cardTemplate", "cardTemplate.cardSet", "cardTemplate.rarity", "cardTemplate.cardSpecies"})
    Page<UserCard> findByUserIdOrderByAcquiredAtDesc(Long userId, Pageable pageable);

    /**
     * Truy vấn chuyên biệt cho trang chi tiết thẻ bài (không load toàn bộ kho thẻ của user) (P1-05).
     */
    List<UserCard> findByUserIdAndCardTemplateIdOrderByIdAsc(Long userId, Long cardTemplateId);

    boolean existsByCardTemplateId(Long cardTemplateId);

    /**
     * Tối ưu SQL (Aggregation Projection): Đếm số lượng từng khuôn mẫu thẻ mà người chơi sở hữu
     * trong một Bộ thẻ cụ thể chỉ bằng 1 câu lệnh GROUP BY duy nhất.
     */
    @Query("""
            SELECT uc.cardTemplate.id AS cardTemplateId, COUNT(uc.id) AS ownedQuantity
            FROM UserCard uc
            WHERE uc.userId = :userId AND uc.cardTemplate.cardSet.id = :setId
            GROUP BY uc.cardTemplate.id
            """)
    List<OwnedCardCountProjection> countOwnedCardsByUserAndSet(
            @Param("userId") Long userId,
            @Param("setId") Long setId
    );

    interface OwnedCardCountProjection {
        Long getCardTemplateId();
        Long getOwnedQuantity();
    }

    /**
     * Đếm tổng số thẻ sở hữu theo từng userId theo lô (Batch) cho Admin User Management — không gây N+1 Query.
     */
    @Query("""
            SELECT uc.userId AS userId, COUNT(uc.id) AS totalCards
            FROM UserCard uc
            WHERE uc.userId IN :userIds
            GROUP BY uc.userId
            """)
    List<UserCardCountProjection> countTotalCardsByUserIds(@Param("userIds") Collection<Long> userIds);

    interface UserCardCountProjection {
        Long getUserId();
        Long getTotalCards();
    }
}
