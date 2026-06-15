package com.coaching_app.controllers;

import com.coaching_app.dto.BihGameRefDTO;
import com.coaching_app.services.FibaWidgetScraperService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.*;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class TestController {

    private final FibaWidgetScraperService scraperService;
    private final ObjectMapper objectMapper;

    private static final String JSON_PATH = "src/main/resources/bih_women_games.json";

    @GetMapping("/game/{id}")
    public ResponseEntity<?> getGame(@PathVariable int id) {
        JsonNode game = scraperService.fetchGame(id);
        if (game == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(game.toString());
    }

    @GetMapping("/scan")
    public ResponseEntity<?> scan(
            @RequestParam(required = false, defaultValue = "2818000") int startId,
            @RequestParam(required = false, defaultValue = "2820000") int endId) throws Exception {

        List<String> keywords = List.of(
                "Play Off", "Playoff",
                "Jumper", "OKK Zenica", "Celik",
                "Igman", "Jedinstvo", "Konjic",
                "Lavovi", "Leotar",
                "Mladi Krajisnik", "Orlovi",
                "RMU", "Banovici"
        );

        List<Integer> foundIds = scraperService.findGameIdsByKeywordsInRange(startId, endId, keywords);

        File jsonFile = new File(JSON_PATH);
        List<BihGameRefDTO> existing = new ArrayList<>();
        if (jsonFile.exists() && jsonFile.length() > 2) {
            existing = objectMapper.readValue(jsonFile, new TypeReference<List<BihGameRefDTO>>() {});
        }

        Set<Integer> existingIds = new HashSet<>();
        for (BihGameRefDTO ref : existing) existingIds.add(ref.getFibaGameId());

        int backfilled = 0;
        for (BihGameRefDTO ref : existing) {
            if (ref.getHomeTeam() != null && !ref.getHomeTeam().isEmpty()) continue;
            JsonNode root = scraperService.fetchGame(ref.getFibaGameId());
            if (root != null) {
                ref.setHomeTeam(root.path("tm").path("1").path("name").asText(""));
                ref.setAwayTeam(root.path("tm").path("2").path("name").asText(""));
                backfilled++;
            }
        }

        int newlyAdded = 0;
        for (Integer id : foundIds) {
            if (existingIds.contains(id)) continue;
            JsonNode root = scraperService.fetchGame(id);
            String home = "";
            String away = "";
            if (root != null) {
                home = root.path("tm").path("1").path("name").asText("");
                away = root.path("tm").path("2").path("name").asText("");
            }
            existing.add(new BihGameRefDTO(id, home, away));
            newlyAdded++;
        }

        existing.sort(Comparator.comparingInt(BihGameRefDTO::getFibaGameId));
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, existing);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("startId", startId);
        response.put("endId", endId);
        response.put("foundInRange", foundIds.size());
        response.put("newlyAdded", newlyAdded);
        response.put("backfilled", backfilled);
        response.put("totalInFile", existing.size());
        response.put("ids", foundIds);
        return ResponseEntity.ok(response);
    }
}
