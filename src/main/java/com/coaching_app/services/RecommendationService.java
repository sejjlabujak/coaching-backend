package com.coaching_app.services;

import com.coaching_app.dto.RecommendationDTO;
import com.coaching_app.dto.RecommendedDrillDTO;
import com.coaching_app.models.Drill;
import com.coaching_app.models.Game;
import com.coaching_app.models.TeamPerformance;
import com.coaching_app.models.User;
import com.coaching_app.repositories.DrillRepository;
import com.coaching_app.repositories.GameRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final GameRepository gameRepository;
    private final DrillRepository drillRepository;
    private final ObjectMapper objectMapper;
    private final GeminiClient geminiClient;

    // Global fallback cache (keyed to null)
    private List<RecommendationDTO> cachedRecommendations = null;
    private LocalDateTime cacheTime = null;

    // Per-team cache
    private record CachedEntry(List<RecommendationDTO> data, LocalDateTime time) {}
    private final Map<Long, CachedEntry> teamCache = new HashMap<>();

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

    public List<RecommendationDTO> getRecommendationsForCoach(User coach) throws Exception {
        Long teamId = coach.getTeam() != null ? coach.getTeam().getId() : null;
        if (teamId == null) throw new RuntimeException("Coach has no team assigned");

        CachedEntry entry = teamCache.get(teamId);
        if (entry != null && entry.time().plusHours(CACHE_HOURS).isAfter(LocalDateTime.now())) {
            log.info("Returning cached recommendations for team {}", teamId);
            return entry.data();
        }

        List<Game> last4Games = gameRepository.findTop4ByTeamIdOrderByDateDesc(teamId);
        if (last4Games.isEmpty()) {
            throw new RuntimeException("No games found for team " + teamId);
        }

        List<Drill> drills = drillRepository.findByDeletedFalse();
        String prompt = PROMPT_TEMPLATE
                .replace("{GAME_DATA}", formatGameData(last4Games))
                .replace("{DRILL_DATA}", formatDrillData(drills));

        String responseContent = geminiClient.generate(prompt, 3);
        List<RecommendationDTO> result = parseRecommendations(responseContent);
        teamCache.put(teamId, new CachedEntry(result, LocalDateTime.now()));
        return result;
    }

    public void invalidateCache() {
        cachedRecommendations = null;
        cacheTime = null;
        teamCache.clear();
    }

    // ── Format last 4 games as readable text
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

    // ── Format drills as readable text
    private String formatDrillData(List<Drill> drills) {
        StringBuilder sb = new StringBuilder();

        for (Drill drill : drills) {
            sb.append("ID: ").append(drill.getId())
                    .append(" | Title: ").append(drill.getTitle())
                    .append(" | Focus: ").append(drill.getFocus() != null ? drill.getFocus().name() : "N/A")
                    .append(" | Intensity: ").append(drill.getIntensity() != null ? drill.getIntensity().name() : "N/A")
                    .append(" | Level: ").append(drill.getLevel() != null ? drill.getLevel() : "N/A");

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

    // ── Parse JSON response into DTOs
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