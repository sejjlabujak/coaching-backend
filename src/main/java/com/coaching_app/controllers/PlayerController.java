package com.coaching_app.controllers;

import com.coaching_app.models.Player;
import com.coaching_app.repositories.PlayerRepository;
import com.coaching_app.services.PlayerSyncService;
import com.coaching_app.services.PlayersScraperService;
import com.coaching_app.dto.PlayerDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints for player management and Eurobasket sync.
 *
 * POST /api/players/sync-roster
 *   Scrapes the current Eurobasket roster and tries to match every player
 *   against IndividualPerformance records already in the DB, then upserts
 *   Player entities.  Useful for an initial import or after a transfer window.
 *
 * GET /api/players/roster-preview
 *   Returns the raw scraped roster (without touching the DB) so you can
 *   preview what Eurobasket currently shows.
 *
 * GET /api/players
 *   Returns all Player entities saved in the database.
 */
@RestController
@RequestMapping("/api/players")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerRepository playerRepository;
    private final PlayersScraperService scraperService;
    private final PlayerSyncService playerSyncService;

    // GET /api/players
    @GetMapping
    public ResponseEntity<List<Player>> getAllPlayers() {
        return ResponseEntity.ok(playerRepository.findAll());
    }

    /**
     * GET /api/players/roster-preview
     *
     * Scrapes Eurobasket roster and returns the raw DTO list.
     * Does NOT write anything to the database.
     * Useful for debugging or verifying the scraper output.
     */
    @GetMapping("/roster-preview")
    public ResponseEntity<List<PlayerDTO>> previewRoster() {
        List<PlayerDTO> roster = scraperService.scrapeRoster();
        return ResponseEntity.ok(roster);
    }

    /**
     * POST /api/players/sync-roster
     *
     * Full sync: scrapes the roster, then for each player on the roster
     * builds a FibaPlayerRef from name tokens and calls the sync service.
     * Call this manually after a transfer window or to do an initial import.
     */
    @PostMapping("/sync-roster")
    public ResponseEntity<Map<String, Object>> syncRoster() {
        List<PlayerDTO> roster = scraperService.scrapeRoster();

        // Convert roster DTOs to FibaPlayerRef so PlayerSyncService can process them.
        // For a roster-driven sync the "FIBA game stats" come from the Eurobasket names,
        // so we split fullName into firstName / familyName.
        List<PlayerSyncService.FibaPlayerRef> refs = roster.stream()
                .filter(dto -> dto.getFullName() != null && dto.getFullName().contains(" "))
                .map(dto -> {
                    String[] parts = dto.getFullName().trim().split("\\s+", 2);
                    return new PlayerSyncService.FibaPlayerRef(parts[0], parts[1], null);
                })
                .toList();

        playerSyncService.syncPlayersFromGame(refs);

        return ResponseEntity.ok(Map.of(
                "rosterSize", roster.size(),
                "synced", refs.size(),
                "status", "Done"
        ));
    }
}