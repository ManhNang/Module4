package com.pokemon.blindbox.controller;

import com.pokemon.blindbox.dto.request.CardTemplateFilterRequest;
import com.pokemon.blindbox.dto.response.CardTemplateResponseDTO;
import com.pokemon.blindbox.dto.response.MasterSetProgressDTO;
import com.pokemon.blindbox.security.AuthenticatedUserProvider;
import com.pokemon.blindbox.service.DigitalBinderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/binder")
@RequiredArgsConstructor
@Tag(name = "3. Inventory", description = "API Kho đồ, Bộ lọc tra cứu thẻ bài động (JPA Specification + @EntityGraph) & Tiến độ Master Set 103 ô")
public class DigitalBinderController {

    private final DigitalBinderService digitalBinderService;
    private final AuthenticatedUserProvider authUserProvider;

    @GetMapping("/cards")
    public ResponseEntity<Page<CardTemplateResponseDTO>> searchCards(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer cardNumber,
            @RequestParam(required = false) Long rarityId,
            @RequestParam(required = false) String rarityName,
            @RequestParam(required = false) Long setId,
            @RequestParam(required = false) String setCode,
            @RequestParam(required = false) String setName,
            @RequestParam(required = false) Long speciesId,
            @RequestParam(required = false) String speciesName,
            @RequestParam(required = false) Boolean secretRareOnly,
            @PageableDefault(size = 50, sort = "cardNumber", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        CardTemplateFilterRequest filter = CardTemplateFilterRequest.builder()
                .name(name)
                .cardNumber(cardNumber)
                .rarityId(rarityId)
                .rarityName(rarityName)
                .setId(setId)
                .setCode(setCode)
                .setName(setName)
                .speciesId(speciesId)
                .speciesName(speciesName)
                .secretRareOnly(secretRareOnly)
                .build();

        return ResponseEntity.ok(digitalBinderService.searchCardTemplates(filter, pageable));
    }

    @GetMapping("/sets/{setId}/progress")
    public ResponseEntity<MasterSetProgressDTO> getMasterSetProgress(@PathVariable Long setId) {
        Long currentUserId = authUserProvider.getRequiredUserId();
        return ResponseEntity.ok(digitalBinderService.getMasterSetProgress(currentUserId, setId));
    }
}
