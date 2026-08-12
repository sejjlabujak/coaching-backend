package com.coaching_app.controllers;

import com.coaching_app.dto.PlayerDTO;
import com.coaching_app.dto.PlayerRequestDTO;
import com.coaching_app.dto.PlayerResponseDTO;
import com.coaching_app.models.User;
import com.coaching_app.services.PlayerService;
import com.coaching_app.services.PlayerSyncService;
import com.coaching_app.services.PlayersScraperService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;
    private final PlayersScraperService scraperService;
    private final PlayerSyncService playerSyncService;

    @GetMapping
    public ResponseEntity<List<PlayerResponseDTO>> getAllPlayers(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(playerService.getPlayersForUser(user.getId()));
    }

//    @GetMapping("/roster-preview")
//    public ResponseEntity<List<PlayerDTO>> previewRoster() {
//        return ResponseEntity.ok(scraperService.scrapeRoster());
//    }

//    @PostMapping("/sync-roster")
//    public ResponseEntity<Map<String, Object>> syncRoster(
//            @AuthenticationPrincipal User user) {
//        playerSyncService.syncRosterForCoach(user);
//        return ResponseEntity.ok(Map.of("status", "Done"));
//    }

    @PostMapping
    public ResponseEntity<PlayerResponseDTO> createPlayer(
            @RequestBody PlayerRequestDTO dto,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(playerService.createPlayer(dto, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlayerResponseDTO> updatePlayer(
            @PathVariable Long id,
            @RequestBody PlayerRequestDTO dto,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(playerService.updatePlayer(id, user.getId(), dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deletePlayer(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        playerService.deletePlayer(id, user.getId());
        return ResponseEntity.ok(Map.of("status", "Deleted"));
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<PlayerResponseDTO> uploadPlayerImage(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile file,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(playerService.uploadImage(id, user.getId(), file));
    }
}
