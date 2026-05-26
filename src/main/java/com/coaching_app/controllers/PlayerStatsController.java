package com.coaching_app.controllers;

import com.coaching_app.dto.PlayerStatsDTO;
import com.coaching_app.services.PlayerStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class PlayerStatsController {

    private final PlayerStatsService playerStatsService;

    /**
     * GET /api/players/{id}/stats
     * GET /api/players/{id}/stats?opponent=Celik
     *
     * Returns all game stats for a player.
     * Optionally filter by opponent team name substring (case-insensitive).
     *
     * Examples:
     *   /api/players/3/stats                   → all games for player 3
     *   /api/players/3/stats?opponent=Celik    → only games vs Celik
     *   /api/players/3/stats?opponent=Jedinstvo → only games vs Jedinstvo
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<List<PlayerStatsDTO>> getPlayerStats(
            @PathVariable Long id,
            @RequestParam(required = false) String opponent) {
        return ResponseEntity.ok(playerStatsService.getPlayerStats(id, opponent));
    }
}