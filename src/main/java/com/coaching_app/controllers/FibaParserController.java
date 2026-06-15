package com.coaching_app.controllers;

import com.coaching_app.models.Game;
import com.coaching_app.repositories.GameRepository;
import com.coaching_app.services.FibaParserService;
import com.coaching_app.services.FibaWidgetScraperService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fiba")
@RequiredArgsConstructor
public class FibaParserController {

    private final FibaWidgetScraperService scraperService;
    private final FibaParserService parserService;
    private final GameRepository gameRepository;

    // Test fetching raw JSON for a game
    @GetMapping("/game/{id}")
    public ResponseEntity<String> getGame(@PathVariable int id) {
        var game = scraperService.fetchGame(id);
        if (game == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(game.toString());
    }

    // Import all games into DB
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importAll() {
        List<Game> games = parserService.importAllGames();
        return ResponseEntity.ok(Map.of(
                "imported", games.size(),
                "status", "Done"
        ));
    }

    // Import single game
    @PostMapping("/import/{id}")
    public ResponseEntity<Map<String, Object>> importGame(@PathVariable int id) {
        Game game = parserService.importGame(id);
        if (game == null) return ResponseEntity.badRequest()
                .body(Map.of("status", "Failed or not our team"));
        return ResponseEntity.ok(Map.of("id", game.getId(), "status", "Saved"));
    }

    // Get all saved games
    @GetMapping("/games")
    public ResponseEntity<List<Game>> getAllGames() {
        return ResponseEntity.ok(gameRepository.findAll());
    }

    // Get last 4 games
    @GetMapping("/games/last4")
    public ResponseEntity<List<Game>> getLast4() {
        return ResponseEntity.ok(gameRepository.findTop4ByOrderByDateDesc());
    }

    // Backfill Team FKs on all existing games
    @PostMapping("/relink-teams")
    public ResponseEntity<Map<String, Object>> relinkTeams() {
        int[] result = parserService.relinkAllGames();
        return ResponseEntity.ok(Map.of(
                "relinked", result[0],
                "unmatched", result[1]
        ));
    }
}