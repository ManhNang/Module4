package com.pokemon.blindbox.service;

import com.pokemon.blindbox.config.RedisCacheConfig;
import com.pokemon.blindbox.dto.response.CardSetResponseDTO;
import com.pokemon.blindbox.dto.response.CardSpeciesResponseDTO;
import com.pokemon.blindbox.dto.response.CardTemplateResponseDTO;
import com.pokemon.blindbox.dto.response.RarityResponseDTO;
import com.pokemon.blindbox.entity.CardSet;
import com.pokemon.blindbox.exception.ResourceNotFoundException;
import com.pokemon.blindbox.repository.CardSetRepository;
import com.pokemon.blindbox.repository.CardSpeciesRepository;
import com.pokemon.blindbox.repository.CardTemplateRepository;
import com.pokemon.blindbox.repository.RarityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service chuyên trách truy vấn danh mục có gắn @Cacheable (Redis).
 * Tách riêng thành Bean độc lập để các Service khác (GachaEngineService, StoreQueryService)
 * gọi qua Spring Proxy, tuyệt đối tránh lỗi Self-invocation theo .agent/rules.md.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CatalogCacheService {

    private final CardSetRepository cardSetRepository;
    private final RarityRepository rarityRepository;
    private final CardSpeciesRepository cardSpeciesRepository;
    private final CardTemplateRepository cardTemplateRepository;

    @Cacheable(value = RedisCacheConfig.CACHE_CARD_SETS, key = "'all'")
    public List<CardSetResponseDTO> getAllCardSets() {
        log.info("[CACHE MISS] Querying DB for all CardSets");
        return cardSetRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(CardSetResponseDTO::fromEntity)
                .toList();
    }

    /**
     * Lấy thông tin Bộ sưu tập theo setId (TTL: 24h).
     */
    @Cacheable(value = RedisCacheConfig.CACHE_CARD_SETS, key = "#setId")
    public CardSetResponseDTO getCardSetById(Long setId) {
        log.info("[CACHE MISS] Querying DB for CardSet setId={}", setId);
        CardSet cardSet = cardSetRepository.findById(setId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Bộ thẻ với id = " + setId));
        return CardSetResponseDTO.fromEntity(cardSet);
    }

    @Cacheable(value = RedisCacheConfig.CACHE_RARITIES, key = "'all_by_id'")
    public List<RarityResponseDTO> getAllRaritiesOrderedById() {
        log.info("[CACHE MISS] Querying DB for all Rarities ordered by id");
        return rarityRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(RarityResponseDTO::fromEntity)
                .toList();
    }

    /**
     * Lấy danh sách Độ hiếm kèm trọng số dropWeight phục vụ thuật toán quay Gacha (TTL: 24h).
     */
    @Cacheable(value = RedisCacheConfig.CACHE_RARITIES, key = "'all_weights'")
    public List<RarityResponseDTO> getAllRaritiesWithWeights() {
        log.info("[CACHE MISS] Querying DB for all Rarities and dropWeights");
        return rarityRepository.findAllByOrderByDropWeightDesc()
                .stream()
                .map(RarityResponseDTO::fromEntity)
                .toList();
    }

    @Cacheable(value = RedisCacheConfig.CACHE_CARD_SPECIES, key = "'all'")
    public List<CardSpeciesResponseDTO> getAllCardSpecies() {
        log.info("[CACHE MISS] Querying DB for all CardSpecies ordered by name");
        return cardSpeciesRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .map(CardSpeciesResponseDTO::fromEntity)
                .toList();
    }

    @Cacheable(value = RedisCacheConfig.CACHE_CARD_TEMPLATES, key = "'set_' + #setId")
    public List<CardTemplateResponseDTO> getCardTemplatesBySetId(Long setId) {
        log.info("[CACHE MISS] Querying DB for CardTemplates setId={}", setId);
        return cardTemplateRepository.findByCardSetIdOrderByCardNumberAsc(setId)
                .stream()
                .map(CardTemplateResponseDTO::fromEntity)
                .toList();
    }

    /**
     * Lấy danh sách khuôn mẫu thẻ đang hoạt động (isActive = true) theo Bộ và Độ hiếm cho Gacha Engine (TTL: 12h).
     */
    @Cacheable(value = RedisCacheConfig.CACHE_CARD_TEMPLATES, key = "#setId + '_' + #rarityId")
    public List<CardTemplateResponseDTO> getCardTemplatesBySetAndRarity(Long setId, Long rarityId) {
        log.info("[CACHE MISS] Querying DB for active CardTemplates setId={}, rarityId={}", setId, rarityId);
        return cardTemplateRepository.findByCardSetIdAndRarityIdAndIsActiveTrueOrderByCardNumberAsc(setId, rarityId)
                .stream()
                .map(CardTemplateResponseDTO::fromEntity)
                .toList();
    }
}
