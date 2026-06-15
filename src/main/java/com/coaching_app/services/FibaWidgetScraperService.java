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

    private boolean containsAnyKeyword(JsonNode node, Collection<String> keywords) {
        // Only look at the two team names, not the entire JSON

        //tm.1.name and tm.2.name
        String team1 = node.path("tm").path("1").path("name").asText("").toLowerCase();
        String team2 = node.path("tm").path("2").path("name").asText("").toLowerCase();

        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()) continue;
            String k = keyword.toLowerCase();
            if (team1.contains(k) || team2.contains(k)) {
                return true;
            }
        }
        return false;
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