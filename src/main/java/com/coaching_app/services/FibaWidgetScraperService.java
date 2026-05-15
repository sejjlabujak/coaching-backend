package com.coaching_app.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class FibaWidgetScraperService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executor = Executors.newFixedThreadPool(20);

    // Cache found game IDs so we don't rescan
    private final List<Integer> cachedGameIds = new CopyOnWriteArrayList<>();
    private boolean scanned = false;

    // ── Fetch single game JSON

    public JsonNode fetchGame(int gameId) {
        String url = "https://fibalivestats.dcd.shared.geniussports.com/data/" + gameId + "/data.json";
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, buildEntity(), String.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return objectMapper.readTree(response.getBody());
            }
        } catch (Exception e) {
            log.debug("Game {} not found: {}", gameId, e.getMessage());
        }
        return null;
    }

    /**
     Extract the numeric data id from a UI page by looking for embedded
     /data/{id}/data.json links.
     */
    public Integer fetchDataIdFromUi(String uiUrl) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    uiUrl, HttpMethod.GET, buildEntity(), String.class);
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                return null;
            }

            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("/data/(\\d+)/data\\.json")
                    .matcher(response.getBody());

            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        } catch (Exception e) {
            log.debug("Failed to fetch UI page {}: {}", uiUrl, e.getMessage());
        }
        return null;
    }

    /**
     Convenience method for testing one UI link.
     It extracts the data id from the UI page and checks whether the JSON contains
     the keyword "playoff".
     */
    @SuppressWarnings("unused")
    public boolean hasPlayoffKeyword(String uiUrl) {
        Integer dataId = fetchDataIdFromUi(uiUrl);
        if (dataId == null) {
            return false;
        }

        JsonNode game = fetchGame(dataId);
        if (game == null) {
            return false;
        }

        String lowerJson = game.toString().toLowerCase();
        return lowerJson.contains("playoff");
    }


    /**
     Scan a numeric ID range and return all IDs whose game JSON contains at
     least one of the provided keywords
     */
    public List<Integer> findGameIdsByKeywordsInRange(int startId, int endId, Collection<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> found = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger processed = new AtomicInteger(0);
        int total = endId - startId + 1;
        List<Future<?>> futures = new ArrayList<>();

        for (int id = startId; id <= endId; id++) {
            final int gameId = id;
            futures.add(executor.submit(() -> {
                try {
                    JsonNode node = fetchGame(gameId);
                    if (node != null && containsAnyKeyword(node, keywords)) {
                        log.info("Found matching game: {}", gameId);
                        found.add(gameId);
                    }
                } catch (Exception e) {
                    log.debug("Error on {}: {}", gameId, e.getMessage());
                } finally {
                    int done = processed.incrementAndGet();
                    if (done % 100 == 0) {
                        log.info("Scanned {}/{} IDs, found {} games so far", done, total, found.size());
                    }
                }
            }));
        }

        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (Exception ignored) {
            }
        }

        found.sort(Integer::compareTo);
        return new ArrayList<>(found);
    }

    private boolean containsAnyKeyword(JsonNode node, Collection<String> keywords) {
        String lowerJson = node.toString().toLowerCase();
        String compactJson = lowerJson.replaceAll("\\s+", "");

        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()) continue;
            String lowerKeyword = keyword.toLowerCase();
            if (lowerJson.contains(lowerKeyword) || compactJson.contains(lowerKeyword.replaceAll("\\s+", ""))) {
                return true;
            }
        }
        return false;
    }

    // ── Scan range in parallel

    public List<Integer> findBiHGames(int startId, int endId) {
        if (scanned && !cachedGameIds.isEmpty()) {
            log.info("Returning {} cached game IDs", cachedGameIds.size());
            return new ArrayList<>(cachedGameIds);
        }

        List<Integer> found = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger processed = new AtomicInteger(0);
        int total = endId - startId + 1;

        List<Future<?>> futures = new ArrayList<>();

        for (int id = startId; id <= endId; id++) {
            final int gameId = id;
            futures.add(executor.submit(() -> {
                try {
                    JsonNode node = fetchGame(gameId);
                    if (node != null && isBiHGame(node)) {
                        log.info("Found BiH game: {}", gameId);
                        found.add(gameId);
                    }
                } catch (Exception e) {
                    log.debug("Error on {}: {}", gameId, e.getMessage());
                } finally {
                    int done = processed.incrementAndGet();
                    if (done % 100 == 0) {
                        log.info("Scanned {}/{} IDs, found {} games so far",
                                done, total, found.size());
                    }
                }
            }));
        }

        // Wait for all to complete
        for (Future<?> f : futures) {
            try { f.get(); } catch (Exception ignored) {}
        }

        found.sort(Integer::compareTo);
        cachedGameIds.clear();
        cachedGameIds.addAll(found);
        scanned = true;

        log.info("Scan complete. Found {} BiH games", found.size());
        return new ArrayList<>(found);
    }

    // ── Check if game belongs to BiH league

    private boolean isBiHGame(JsonNode node) {
        // Check competition name — adjust these strings after you see real data
        String[] biHKeywords = {
                "bih", "bosna", "bosnia", "hercegovina",
                "premijer liga", "kup bih", "liga bih"
        };
        String lowerJson = node.toString().toLowerCase();
        for (String keyword : biHKeywords) {
            if (lowerJson.contains(keyword)) return true;
        }

        // Check common competition fields safely
        JsonNode competition = node.path("competition");
        if (!competition.isMissingNode()) {
            // competition might be an object or a string
            String compName = competition.path("name").asText(competition.path("title").asText(competition.asText(""))).toLowerCase();
            for (String keyword : biHKeywords) {
                if (compName.contains(keyword)) return true;
            }
        }

        // Also check tournament/league fields sometimes placed elsewhere
        String[] altFields = {"league", "tournament", "competitionName", "series"};
        for (String f : altFields) {
            JsonNode n = node.path(f);
            if (!n.isMissingNode()) {
                String text = n.asText("").toLowerCase();
                for (String keyword : biHKeywords) {
                    if (text.contains(keyword)) return true;
                }
            }
        }

        // Finally, check teams' country or name fields
        JsonNode home = node.path("home");
        JsonNode away = node.path("away");
        List<JsonNode> teams = Arrays.asList(home, away);
        for (JsonNode team : teams) {
            if (team.isMissingNode()) continue;
            String name = team.path("name").asText("").toLowerCase();
            String country = team.path("country").asText("").toLowerCase();
            for (String keyword : biHKeywords) {
                if (name.contains(keyword) || country.contains(keyword)) return true;
            }
        }

        return false;
    }

    // ── Get last N games

    @SuppressWarnings("unused")
    public List<JsonNode> getLastNGames(int n) {
        List<Integer> ids = cachedGameIds.isEmpty()
                ? findBiHGames(2847000, 2849000)
                : new ArrayList<>(cachedGameIds);

        // Take last N IDs (most recent)
        List<Integer> lastN = ids.size() > n
                ? ids.subList(ids.size() - n, ids.size())
                : ids;

        List<JsonNode> games = new ArrayList<>();
        for (int id : lastN) {
            JsonNode game = fetchGame(id);
            if (game != null) games.add(game);
        }
        return games;
    }

    // ── Build HTTP headers

    private HttpEntity<String> buildEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36");
        headers.set("Referer", "https://fibalivestats.dcd.shared.geniussports.com/");
        headers.set("Origin", "https://fibalivestats.dcd.shared.geniussports.com");
        headers.set("Accept", "application/json,text/plain,*/*");
        return new HttpEntity<>(headers);
    }
}