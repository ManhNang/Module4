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

    /**
     * Khóa bi quan toàn bộ các thẻ chưa khóa thuộc danh sách cardTemplateId của người dùng để bán nhanh hàng loạt an toàn.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT uc FROM UserCard uc
            JOIN FETCH uc.cardTemplate ct
            JOIN FETCH ct.rarity
            WHERE uc.userId = :userId
              AND uc.isLocked = false
              AND ct.id IN :templateIds
            ORDER BY uc.id ASC
            """)
    List<UserCard> findUnlockedByUserIdAndTemplateIdsForUpdate(
            @Param("userId") Long userId,
            @Param("templateIds") Collection<Long> templateIds
    );

    @EntityGraph(attributePaths = {"cardTemplate", "cardTemplate.cardSet", "cardTemplate.rarity", "cardTemplate.cardSpecies"})
    List<UserCard> findByBoosterPackIdOrderByIdAsc(Long boosterPackId);

    @EntityGraph(attributePaths = {"cardTemplate", "cardTemplate.cardSet", "cardTemplate.rarity", "cardTemplate.cardSpecies"})
    List<UserCard> findByUserIdOrderByAcquiredAtDesc(Long userId);

    @EntityGraph(attributePaths = {"cardTemplate", "cardTemplate.cardSet", "cardTemplate.rarity", "cardTemplate.cardSpecies"})
    Page<UserCard> findByUserIdOrderByAcquiredAtDesc(Long userId, Pageable pageable);

    /**
     * Truy vấn kho thẻ của người chơi đã sắp xếp sẵn từ DB theo đúng thứ tự mặc định của trang Kho đồ:
     * Giá bán nhanh (buybackPrice) Cao -> Thấp, Mã thẻ (#001 -> #103), ưu tiên bản sao chưa khóa (isLocked ASC) lên đầu.
     */
    @EntityGraph(attributePaths = {"cardTemplate", "cardTemplate.cardSet", "cardTemplate.rarity", "cardTemplate.cardSpecies"})
    @Query("""
            SELECT uc FROM UserCard uc
            JOIN uc.cardTemplate ct
            JOIN ct.rarity r
            WHERE uc.userId = :userId
            ORDER BY r.buybackPrice DESC, ct.cardNumber ASC, uc.isLocked ASC, uc.id ASC
            """)
    Page<UserCard> findByUserIdOrderByDefaultInventorySort(@Param("userId") Long userId, Pageable pageable);

    /**
     * Truy vấn TOÀN BỘ thẻ rảnh (isLocked = false) cho Đảo Pokémon & Trạm Thám Hiểm,
     * dùng LEFT JOIN để không bao giờ làm rớt thẻ và sắp xếp trực tiếp theo tốc độ sinh Xu/phút giảm dần
     * (không cắt LIMIT 150 trước khi sort).
     */
    @EntityGraph(attributePaths = {"cardTemplate", "cardTemplate.cardSet", "cardTemplate.rarity", "cardTemplate.cardSpecies"})
    @Query("""
            SELECT uc FROM UserCard uc
            JOIN uc.cardTemplate ct
            LEFT JOIN ct.rarity r
            WHERE uc.userId = :userId
              AND uc.isLocked = false
            ORDER BY COALESCE(r.sanctuaryCoinsPerMin, 2) DESC, COALESCE(r.dropWeight, 1000000) ASC, ct.cardNumber ASC, uc.id ASC
            """)
    List<UserCard> findUnlockedForSanctuary(@Param("userId") Long userId);

    /**
     * Truy vấn chuyên biệt cho trang chi tiết thẻ bài (không load toàn bộ kho thẻ của user) (P1-05).
     */
    List<UserCard> findByUserIdAndCardTemplateIdOrderByIdAsc(Long userId, Long cardTemplateId);

    long countByUserId(Long userId);

    List<UserCard> findByUserIdAndCardTemplateIdInOrderByIdAsc(Long userId, Collection<Long> cardTemplateIds);

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
