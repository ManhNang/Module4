package com.pokemon.blindbox.controller;

import com.pokemon.blindbox.dto.request.PlaceSanctuaryCardRequest;
import com.pokemon.blindbox.dto.request.StartExpeditionRequest;
import com.pokemon.blindbox.security.AuthenticatedUserProvider;
import com.pokemon.blindbox.service.SanctuaryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/sanctuary")
@RequiredArgsConstructor
@Tag(name = "7. Poke-Sanctuary", description = "API Đảo Nuôi Dưỡng 2.5D, Thám Hiểm & Nhiệm Vụ Ngày")
public class SanctuaryController {

    private final SanctuaryService sanctuaryService;
    private final AuthenticatedUserProvider authUserProvider;

    @GetMapping("/state")
    public ResponseEntity<Map<String, Object>> getState() {
        return ResponseEntity.ok(sanctuaryService.getSanctuaryState(authUserProvider.getRequiredUserId()));
    }

    @PostMapping("/slots")
    public ResponseEntity<Map<String, Object>> placeCard(@Valid @RequestBody PlaceSanctuaryCardRequest request) {
        return ResponseEntity.ok(sanctuaryService.placeCardOnIsland(authUserProvider.getRequiredUserId(), request.userCardId()));
    }

    @DeleteMapping("/slots/{slotId}")
    public ResponseEntity<Map<String, Object>> removeCard(@PathVariable Long slotId) {
        return ResponseEntity.ok(sanctuaryService.removeCardFromIsland(authUserProvider.getRequiredUserId(), slotId));
    }

    @PostMapping("/slots/{slotId}/harvest")
    public ResponseEntity<Map<String, Object>> harvestSlot(@PathVariable Long slotId) {
        return ResponseEntity.ok(sanctuaryService.harvestSlot(authUserProvider.getRequiredUserId(), slotId));
    }

    @PostMapping("/expeditions")
    public ResponseEntity<Map<String, Object>> startExpedition(@Valid @RequestBody StartExpeditionRequest request) {
        return ResponseEntity.ok(sanctuaryService.startExpedition(authUserProvider.getRequiredUserId(), request));
    }

    @PostMapping("/expeditions/{expeditionId}/claim")
    public ResponseEntity<Map<String, Object>> claimExpedition(@PathVariable Long expeditionId) {
        return ResponseEntity.ok(sanctuaryService.claimExpedition(authUserProvider.getRequiredUserId(), expeditionId));
    }

    @PostMapping("/quests/{questId}/claim")
    public ResponseEntity<Map<String, Object>> claimQuest(@PathVariable Long questId) {
        return ResponseEntity.ok(sanctuaryService.claimDailyQuest(authUserProvider.getRequiredUserId(), questId));
    }
}
