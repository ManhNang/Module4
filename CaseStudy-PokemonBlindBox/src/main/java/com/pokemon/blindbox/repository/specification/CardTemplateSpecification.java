package com.pokemon.blindbox.repository.specification;

import com.pokemon.blindbox.dto.request.CardTemplateFilterRequest;
import com.pokemon.blindbox.entity.CardSet;
import com.pokemon.blindbox.entity.CardSpecies;
import com.pokemon.blindbox.entity.CardTemplate;
import com.pokemon.blindbox.entity.Rarity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * CardTemplateSpecification xây dựng bộ lọc động cho Pokedex / Digital Binder.
 * Khi gọi qua CardTemplateRepository.findAll(spec, pageable), phương thức đã được gắn sẵn
 * @EntityGraph(attributePaths = {"cardSet", "rarity", "cardSpecies"}) theo .agent/rules.md
 * để lấy trọn vẹn dữ liệu chỉ trong 1 câu lệnh SQL duy nhất (chống N+1 Query).
 */
public final class CardTemplateSpecification {

    private CardTemplateSpecification() {
    }

    public static Specification<CardTemplate> withFilter(CardTemplateFilterRequest filter) {
        return (root, query, cb) -> {
            if (filter == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            // 1. Lọc theo Tên thẻ (name - tìm kiếm gần đúng không phân biệt hoa thường)
            if (hasText(filter.name())) {
                String pattern = "%" + filter.name().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), pattern));
            }

            // 2. Lọc theo Số thứ tự thẻ chính xác (cardNumber kiểu Integer: 52, 98, 141...)
            if (filter.cardNumber() != null) {
                predicates.add(cb.equal(root.get("cardNumber"), filter.cardNumber()));
            }

            // 3. Lọc theo Độ Hiếm (rarityId hoặc tên như "Không phổ biến", "Hyper Rare")
            if (filter.rarityId() != null || hasText(filter.rarityName())) {
                Join<CardTemplate, Rarity> rarityJoin = root.join("rarity", JoinType.INNER);
                if (filter.rarityId() != null) {
                    predicates.add(cb.equal(rarityJoin.get("id"), filter.rarityId()));
                }
                if (hasText(filter.rarityName())) {
                    predicates.add(cb.equal(
                            cb.lower(rarityJoin.get("name")),
                            filter.rarityName().trim().toLowerCase(Locale.ROOT)
                    ));
                }
            }

            // 4. Lọc theo Bộ sưu tập / Đặt (setId, setCode "DLR" hoặc setName "Delta Reign")
            boolean needSetJoin = filter.setId() != null
                    || hasText(filter.setCode())
                    || hasText(filter.setName())
                    || Boolean.TRUE.equals(filter.secretRareOnly());

            if (needSetJoin) {
                Join<CardTemplate, CardSet> setJoin = root.join("cardSet", JoinType.INNER);
                if (filter.setId() != null) {
                    predicates.add(cb.equal(setJoin.get("id"), filter.setId()));
                }
                if (hasText(filter.setCode())) {
                    predicates.add(cb.equal(
                            cb.lower(setJoin.get("code")),
                            filter.setCode().trim().toLowerCase(Locale.ROOT)
                    ));
                }
                if (hasText(filter.setName())) {
                    predicates.add(cb.equal(
                            cb.lower(setJoin.get("name")),
                            filter.setName().trim().toLowerCase(Locale.ROOT)
                    ));
                }
                if (Boolean.TRUE.equals(filter.secretRareOnly())) {
                    predicates.add(cb.greaterThan(root.get("cardNumber"), setJoin.get("baseSetTotal")));
                }
            }

            // 5. Lọc theo Loài (speciesId hoặc speciesName như "Hầm hào huyền thoại", "Steelix", "Rayquaza")
            if (filter.speciesId() != null || hasText(filter.speciesName())) {
                Join<CardTemplate, CardSpecies> speciesJoin = root.join("cardSpecies", JoinType.INNER);
                if (filter.speciesId() != null) {
                    predicates.add(cb.equal(speciesJoin.get("id"), filter.speciesId()));
                }
                if (hasText(filter.speciesName())) {
                    predicates.add(cb.equal(
                            cb.lower(speciesJoin.get("name")),
                            filter.speciesName().trim().toLowerCase(Locale.ROOT)
                    ));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
