package com.coaching_app.services;

import com.coaching_app.dto.BihGameRefDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GameScanService {

    private final FibaWidgetScraperService scraperService;
    private final ObjectMapper objectMapper;

    private static final String JSON_PATH = "src/main/resources/bih_women_games.json";

    private static final List<String> DEFAULT_KEYWORDS = List.of(
            "Play Off", "Playoff",
            "Jumper", "OKK Zenica", "Celik",
            "Igman", "Jedinstvo", "Konjic",
            "Lavovi", "Leotar",
            "Mladi Krajisnik", "Orlovi",
            "RMU", "Banovici"
    );

    public Map<String, Object> scan(int startId, int endId) throws Exception {
        List<Integer> foundIds = scraperService.findGameIdsByKeywordsInRange(startId, endId, DEFAULT_KEYWORDS);

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
            String home = root != null ? root.path("tm").path("1").path("name").asText("") : "";
            String away = root != null ? root.path("tm").path("2").path("name").asText("") : "";
            existing.add(new BihGameRefDTO(id, home, away));
            newlyAdded++;
        }

        existing.sort(Comparator.comparingInt(BihGameRefDTO::getFibaGameId));
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, existing);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startId", startId);
        result.put("endId", endId);
        result.put("foundInRange", foundIds.size());
        result.put("newlyAdded", newlyAdded);
        result.put("backfilled", backfilled);
        result.put("totalInFile", existing.size());
        result.put("ids", foundIds);
        return result;
    }
}
