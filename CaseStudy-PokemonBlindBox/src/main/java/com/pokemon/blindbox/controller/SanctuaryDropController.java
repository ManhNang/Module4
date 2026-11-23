package com.pokemon.blindbox.controller;

import com.pokemon.blindbox.security.AuthenticatedUserProvider;
import com.pokemon.blindbox.service.SanctuaryDropService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sanctuary")
@RequiredArgsConstructor
@Tag(name = "7B. Sanctuary Random Drops", description = "API Sự kiện ngẫu nhiên trên Đảo Pokémon & Đổi Mảnh Pack")
public class SanctuaryDropController {

    private final SanctuaryDropService sanctuaryDropService;
    private final AuthenticatedUserProvider authUserProvider;

    @GetMapping("/drops/check")
    public ResponseEntity<Map<String, Object>> checkOrSpawnDrop() {
        return ResponseEntity.ok(sanctuaryDropService.checkOrSpawnDrop(authUserProvider.getRequiredUserId()));
    }

    @PostMapping("/drops/{id}/claim")
    public ResponseEntity<Map<String, Object>> claimDrop(@PathVariable Long id) {
        return ResponseEntity.ok(sanctuaryDropService.claimDrop(authUserProvider.getRequiredUserId(), id));
    }

    @PostMapping("/shards/exchange")
    public ResponseEntity<Map<String, Object>> exchangeShards() {
        return ResponseEntity.ok(sanctuaryDropService.exchangeShardsForPack(authUserProvider.getRequiredUserId()));
    }
}
