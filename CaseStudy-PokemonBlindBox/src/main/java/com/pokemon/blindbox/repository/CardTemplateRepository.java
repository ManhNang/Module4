package com.pokemon.blindbox.repository;

import com.pokemon.blindbox.entity.CardTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardTemplateRepository extends JpaRepository<CardTemplate, Long>, JpaSpecificationExecutor<CardTemplate> {

    @EntityGraph(attributePaths = {"cardSet", "rarity", "cardSpecies"})
    Optional<CardTemplate> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"cardSet", "rarity", "cardSpecies"})
    Optional<CardTemplate> findByCardSetIdAndCardNumber(Long setId, Integer cardNumber);

    /**
     * Kiểm tra trùng Số thứ tự thẻ trong cùng một Bộ sưu tập trước khi lưu (trả về 409 CONFLICT).
     */
    boolean existsByCardSetIdAndCardNumber(Long setId, Integer cardNumber);

    /**
     * Kiểm tra trùng Số thứ tự thẻ khi cập nhật (loại trừ chính thẻ đang sửa).
     */
    boolean existsByCardSetIdAndCardNumberAndIdNot(Long setId, Integer cardNumber, Long id);

    boolean existsByCardSetId(Long setId);

    boolean existsByRarityId(Long rarityId);

    boolean existsByCardSpeciesId(Long speciesId);

    /**
     * Chống lỗi N+1 Query khi lọc Pokedex bằng JpaSpecificationExecutor:
     * JOIN FETCH trọn vẹn cardSet, rarity, cardSpecies trong 1 câu lệnh SQL duy nhất.
     */
    @Override
    @EntityGraph(attributePaths = {"cardSet", "rarity", "cardSpecies"})
    Page<CardTemplate> findAll(Specification<CardTemplate> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"cardSet", "rarity", "cardSpecies"})
    List<CardTemplate> findByCardSetIdAndRarityIdOrderByCardNumberAsc(Long setId, Long rarityId);

    @EntityGraph(attributePaths = {"cardSet", "rarity", "cardSpecies"})
    List<CardTemplate> findByCardSetIdAndRarityIdAndIsActiveTrueOrderByCardNumberAsc(Long setId, Long rarityId);

    /**
     * Lấy toàn bộ khuôn mẫu thẻ của 1 Bộ sưu tập sắp xếp chuẩn ORDER BY cardNumber ASC
     * trong 1 câu lệnh SQL duy nhất (chống N+1 Query) phục vụ Digital Binder.
     */
    @EntityGraph(attributePaths = {"cardSet", "rarity", "cardSpecies"})
    List<CardTemplate> findByCardSetIdOrderByCardNumberAsc(Long setId);
}
