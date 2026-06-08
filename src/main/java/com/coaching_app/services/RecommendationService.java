package com.coaching_app.services;

import com.coaching_app.dto.RecommendationDTO;
import com.coaching_app.dto.RecommendedDrillDTO;
import com.coaching_app.models.Drill;
import com.coaching_app.models.Game;
import com.coaching_app.models.TeamPerformance;
import com.coaching_app.repositories.DrillRepository;
import com.coaching_app.repositories.GameRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final GameRepository gameRepository;
    private final DrillRepository drillRepository;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    private List<RecommendationDTO> cachedRecommendations = null;
    private LocalDateTime cacheTime = null;
    private static final int CACHE_HOURS = 6;

    private static final String PROMPT_TEMPLATE = """
            You are an expert basketball coaching assistant analyzing team performance data from the last 4 games.

            Based on the following team statistics from the last 4 games, identify the 3 weakest areas and recommend specific drills to address them.

            LAST 4 GAMES TEAM PERFORMANCE:
            {GAME_DATA}

            AVAILABLE DRILLS IN LIBRARY:
            {DRILL_DATA}

            Analysis rules:
            - Field goal percentage below 45% = shooting problem
            - Three point percentage below 35% = perimeter shooting problem
            - Free throw percentage below 70% = free throw problem
            - Turnovers above 15 per game = ball handling/passing problem
            - Rebounds total below 30 per game = rebounding problem
            - Assists below 10 per game = offensive flow problem
            - Points in the paint below 20 per game = interior offense problem
            - Steals below 5 per game = defensive pressure problem
            - Fast break points below 10 per game = transition/conditioning problem

            Return ONLY a valid JSON array, no explanation, no markdown, no backticks:
            [
              {
                "weakArea": "Shooting",
                "averageStat": "38% field goal percentage",
                "analysis": "Team is struggling with field goal efficiency over last 4 games",
                "recommendedDrills": [
                  {
                    "drillId": 1,
                    "drillTitle": "Three-Point Shooting",
                    "reason": "Improves perimeter shooting consistency and form"
                  }
                ]
              }
            ]
            """;

    public List<RecommendationDTO> getRecommendations() throws Exception {
        if (isCacheValid()) {
            log.info("Returning cached recommendations");
            return cachedRecommendations;
        }
        // 1. Fetch last 4 games with TeamPerformance
        List<Game> last4Games = gameRepository.findTop4ByOrderByDateDesc();
        if (last4Games.isEmpty()) {
            throw new RuntimeException("No games found in the database.");
        }

        // 2. Fetch all non-deleted drills
        List<Drill> drills = drillRepository.findByDeletedFalse();

        // 3. Format data for the prompt
        String gameData = formatGameData(last4Games);
        String drillData = formatDrillData(drills);

        // 4. Build the prompt
        String prompt = PROMPT_TEMPLATE
                .replace("{GAME_DATA}", gameData)
                .replace("{DRILL_DATA}", drillData);

        // 5. Call Gemini API with retry on 429
        String responseContent = callGeminiWithRetry(prompt, 3);
        List<RecommendationDTO> result = parseRecommendations(responseContent);
        cachedRecommendations = result;
        cacheTime = LocalDateTime.now();
        return result;
    }

    public void invalidateCache(){
        cachedRecommendations = null;
        cacheTime = null;
    }

    public boolean isCacheValid() {
        return cachedRecommendations !=null
                && cacheTime != null
                && cacheTime.plusHours(CACHE_HOURS).isAfter(LocalDateTime.now());
    }
    // ── Format last 4 games as readable text ──────────────────────────────────
    private String formatGameData(List<Game> games) {
        StringBuilder sb = new StringBuilder();
        int gameNum = 1;
        for (Game game : games) {
            TeamPerformance tp = game.getTeamPerformance();
            sb.append("Game ").append(gameNum++).append(": ")
                    .append(game.getHomeTeam()).append(" vs ").append(game.getAwayTeam())
                    .append(" | Result: ").append(game.getResult())
                    .append(" (").append(game.getHomeScore()).append("-").append(game.getAwayScore()).append(")")
                    .append(" | Date: ").append(game.getDate()).append("\n");
            if (tp != null) {
                sb.append("  Shooting: FG ").append(tp.getFieldGoalsMade()).append("/")
                        .append(tp.getFieldGoalsAttempted()).append(" (").append(tp.getFieldGoalsPercentage()).append("%)")
                        .append(", 3PT ").append(tp.getThreePointersMade()).append("/")
                        .append(tp.getThreePointersAttempted()).append(" (").append(tp.getThreePointersPercentage()).append("%)")
                        .append(", FT ").append(tp.getFreeThrowsMade()).append("/")
                        .append(tp.getFreeThrowsAttempted()).append(" (").append(tp.getFreeThrowsPercentage()).append("%)\n");
                sb.append("  Rebounds: ").append(tp.getReboundsTotal())
                        .append(" (Off: ").append(tp.getReboundsOffensive())
                        .append(", Def: ").append(tp.getReboundsDefensive()).append(")\n");
                sb.append("  Playmaking: Assists ").append(tp.getAssists())
                        .append(", Turnovers ").append(tp.getTurnovers())
                        .append(", Steals ").append(tp.getSteals())
                        .append(", Blocks ").append(tp.getBlocks()).append("\n");
                sb.append("  Scoring: Points ").append(tp.getPoints())
                        .append(", FastBreak ").append(tp.getPointsFastBreak())
                        .append(", Paint ").append(tp.getPointsInThePaint())
                        .append(", FromTurnovers ").append(tp.getPointsFromTurnovers())
                        .append(", SecondChance ").append(tp.getPointsSecondChance())
                        .append(", Bench ").append(tp.getBenchPoints()).append("\n");
                sb.append("  Fouls: ").append(tp.getFoulsPersonal())
                        .append(", BiggestLead: ").append(tp.getBiggestLead()).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // ── Format drills as readable text ────────────────────────────────────────

    private String formatDrillData(List<Drill> drills) {
        StringBuilder sb = new StringBuilder();

        for (Drill drill : drills) {
            sb.append("ID: ").append(drill.getId())
                    .append(" | Title: ").append(drill.getTitle())
                    .append(" | Focus: ").append(drill.getFocus() != null ? drill.getFocus().name() : "N/A")
                    .append(" | Intensity: ").append(drill.getIntensity() != null ? drill.getIntensity().name() : "N/A")
                    .append(" | Level: ").append(drill.getLevel() != null ? drill.getLevel() : "N/A")
                    .append(" | AgeGroup: ").append(drill.getAgeGroup() != null ? drill.getAgeGroup().name() : "N/A");

            if (drill.getDescription() != null && !drill.getDescription().isBlank()) {
                String shortDesc = drill.getDescription().length() > 120
                        ? drill.getDescription().substring(0, 120) + "..."
                        : drill.getDescription();
                sb.append(" | Desc: ").append(shortDesc);
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    // ── Call Gemini API with retry on 429 ─────────────────────────────────────

    private String callGeminiWithRetry(String prompt, int maxRetries) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        String requestBody = objectMapper.writeValueAsString(new java.util.HashMap<>() {{
            put("contents", List.of(new java.util.HashMap<>() {{
                put("parts", List.of(new java.util.HashMap<>() {{
                    put("text", prompt);
                }}));
            }}));
        }});

        int attempt = 0;
        int waitSeconds = 60;

        while (attempt <= maxRetries) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_URL + geminiApiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                return root.path("candidates").get(0)
                        .path("content")
                        .path("parts").get(0)
                        .path("text")
                        .asText();
            }

            if (response.statusCode() == 429) {
                // Try to extract retryDelay from response
                try {
                    JsonNode errorRoot = objectMapper.readTree(response.body());
                    JsonNode details = errorRoot.path("error").path("details");
                    for (JsonNode detail : details) {
                        if (detail.has("retryDelay")) {
                            String retryDelay = detail.path("retryDelay").asText();
                            waitSeconds = Integer.parseInt(retryDelay.replace("s", "")) + 10;
                        }
                    }
                } catch (Exception ignored) {}

                log.warn("Gemini rate limited (429). Waiting {}s before retry {}/{}",
                        waitSeconds, attempt + 1, maxRetries);
                Thread.sleep(waitSeconds * 1000L);
                attempt++;
                continue;
            }

            throw new RuntimeException("Gemini API error: HTTP " + response.statusCode()
                    + " — " + response.body());
        }

        throw new RuntimeException("Gemini API failed after " + maxRetries + " retries due to rate limiting.");
    }

    // ── Parse JSON response into DTOs ─────────────────────────────────────────

    private List<RecommendationDTO> parseRecommendations(String rawContent) throws Exception {
        // Strip markdown fences if present
        String cleaned = rawContent.replaceAll("```json|```", "").trim();

        JsonNode array = objectMapper.readTree(cleaned);
        List<RecommendationDTO> result = new ArrayList<>();

        if (!array.isArray()) {
            throw new RuntimeException("Expected JSON array from Gemini, got: " + cleaned);
        }

        for (JsonNode node : array) {
            RecommendationDTO dto = new RecommendationDTO();
            dto.setWeakArea(node.path("weakArea").asText());
            dto.setAverageStat(node.path("averageStat").asText());
            dto.setAnalysis(node.path("analysis").asText());

            List<RecommendedDrillDTO> drillDTOs = new ArrayList<>();
            JsonNode drillsNode = node.path("recommendedDrills");

            if (drillsNode.isArray()) {
                for (JsonNode drillNode : drillsNode) {
                    RecommendedDrillDTO drillDTO = new RecommendedDrillDTO();
                    drillDTO.setDrillId(drillNode.path("drillId").asLong());
                    drillDTO.setDrillTitle(drillNode.path("drillTitle").asText());
                    drillDTO.setReason(drillNode.path("reason").asText());
                    drillDTOs.add(drillDTO);
                }
            }

            dto.setRecommendedDrills(drillDTOs);
            result.add(dto);
        }

        return result;
    }
}