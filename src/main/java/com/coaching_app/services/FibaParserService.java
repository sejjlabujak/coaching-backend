package com.coaching_app.services;

import com.coaching_app.models.Game;
import com.coaching_app.models.IndividualPerformance;
import com.coaching_app.models.TeamPerformance;
import com.coaching_app.repositories.GameRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FibaParserService {

    private final GameRepository gameRepository;
    private final FibaWidgetScraperService scraperService;
    private final PlayerSyncService playerSyncService;

    // Team we are tracking — matches by name substring
    private static final String OUR_TEAM = "Play Off";

    private static final List<Integer> GAME_IDS = List.of(
            2801746, 2806047, 2808450, 2811975, 2814884,
            2815188, 2817375, 2818322, 2818407, 2818581,
            2818583, 2820915, 2827172, 2830005, 2830652,
            2832249, 2832818, 2833879, 2834205, 2834206
    );

    // ── Import all known games ────────────────────────────────────────────────

    public List<Game> importAllGames() {
        List<Game> saved = new ArrayList<>();
        for (int fibaId : GAME_IDS) {
            try {
                Game game = importGame(fibaId);
                if (game != null) saved.add(game);
            } catch (Exception e) {
                log.warn("Failed to import game {}: {}", fibaId, e.getMessage());
            }
        }
        log.info("Imported {} games", saved.size());
        return saved;
    }

    // ── Import single game ────────────────────────────────────────────────────

    public Game importGame(int fibaId) {
        // Skip if already imported
        if (gameRepository.findByFibaGameId(fibaId).isPresent()) {
            log.info("Game {} already exists, skipping", fibaId);
            return gameRepository.findByFibaGameId(fibaId).get();
        }

        JsonNode root = scraperService.fetchGame(fibaId);
        if (root == null) {
            log.warn("Could not fetch game {}", fibaId);
            return null;
        }

        JsonNode teams = root.path("tm");
        JsonNode team1 = teams.path("1");
        JsonNode team2 = teams.path("2");

        String team1Name = team1.path("name").asText();
        String team2Name = team2.path("name").asText();

        // Determine which team is ours
        boolean ourTeamIsHome = team1Name.contains(OUR_TEAM);
        boolean ourTeamIsAway = team2Name.contains(OUR_TEAM);

        if (!ourTeamIsHome && !ourTeamIsAway) {
            log.info("Game {} does not involve {}, skipping", fibaId, OUR_TEAM);
            return null;
        }

        JsonNode ourTeamNode = ourTeamIsHome ? team1 : team2;
        JsonNode opponentNode = ourTeamIsHome ? team2 : team1;

        int ourScore = ourTeamNode.path("score").asInt();
        int opponentScore = opponentNode.path("score").asInt();
        String result = ourScore > opponentScore ? "WIN" : "LOSS";

        // Build Game
        Game game = new Game();
        game.setFibaGameId(fibaId);
        game.setHomeTeam(team1Name);
        game.setAwayTeam(team2Name);
        game.setHomeScore(team1.path("score").asInt());
        game.setAwayScore(team2.path("score").asInt());
        game.setResult(result);
        game.setDate(LocalDate.now()); // FIBA JSON doesn't include date in this format
        game.setCompetition("KSBIH Playoff");

        // Build TeamPerformance from our team's totals
        TeamPerformance tp = parseTeamPerformance(ourTeamNode, game);
        game.setTeamPerformance(tp);

        // Build IndividualStats from our team's players
        List<IndividualPerformance> stats = parsePlayers(ourTeamNode, game);
        game.setIndividualStats(stats);

        Game saved = gameRepository.save(game);
        log.info("Saved game {} — {} {} vs {} {}", fibaId, team1Name, ourScore, opponentScore, result);

        List<PlayerSyncService.FibaPlayerRef> fibaPlayers = collectPlayerRefs(ourTeamNode);
        if (!fibaPlayers.isEmpty()) {
            playerSyncService.syncPlayersFromGame(fibaPlayers);
        }
        return saved;
    }

    private List<PlayerSyncService.FibaPlayerRef> collectPlayerRefs(JsonNode team) {
        List<PlayerSyncService.FibaPlayerRef> refs = new ArrayList<>();
        JsonNode players = team.path("pl");

        players.fields().forEachRemaining(entry -> {
            JsonNode p = entry.getValue();
            String minutes = p.path("sMinutes").asText("0:00");
            if (minutes.equals("0:00")) return; // skip DNPs

            String firstName   = p.path("firstName").asText();
            String familyName  = p.path("familyName").asText();
            String shirtNumber = p.path("shirtNumber").asText();

            if (!firstName.isBlank() || !familyName.isBlank()) {
                refs.add(new PlayerSyncService.FibaPlayerRef(firstName, familyName, shirtNumber));
            }
        });

        return refs;
    }

    // ── Parse team performance ────────────────────────────────────────────────

    private TeamPerformance parseTeamPerformance(JsonNode team, Game game) {
        TeamPerformance tp = new TeamPerformance();
        tp.setGame(game);

        tp.setFieldGoalsMade(team.path("tot_sFieldGoalsMade").asInt());
        tp.setFieldGoalsAttempted(team.path("tot_sFieldGoalsAttempted").asInt());
        tp.setFieldGoalsPercentage(team.path("tot_sFieldGoalsPercentage").asInt());
        tp.setThreePointersMade(team.path("tot_sThreePointersMade").asInt());
        tp.setThreePointersAttempted(team.path("tot_sThreePointersAttempted").asInt());
        tp.setThreePointersPercentage(team.path("tot_sThreePointersPercentage").asInt());
        tp.setFreeThrowsMade(team.path("tot_sFreeThrowsMade").asInt());
        tp.setFreeThrowsAttempted(team.path("tot_sFreeThrowsAttempted").asInt());
        tp.setFreeThrowsPercentage(team.path("tot_sFreeThrowsPercentage").asInt());
        tp.setReboundsDefensive(team.path("tot_sReboundsDefensive").asInt());
        tp.setReboundsOffensive(team.path("tot_sReboundsOffensive").asInt());
        tp.setReboundsTotal(team.path("tot_sReboundsTotal").asInt());
        tp.setAssists(team.path("tot_sAssists").asInt());
        tp.setTurnovers(team.path("tot_sTurnovers").asInt());
        tp.setSteals(team.path("tot_sSteals").asInt());
        tp.setBlocks(team.path("tot_sBlocks").asInt());
        tp.setPoints(team.path("tot_sPoints").asInt());
        tp.setPointsFastBreak(team.path("tot_sPointsFastBreak").asInt());
        tp.setPointsFromTurnovers(team.path("tot_sPointsFromTurnovers").asInt());
        tp.setPointsInThePaint(team.path("tot_sPointsInThePaint").asInt());
        tp.setPointsSecondChance(team.path("tot_sPointsSecondChance").asInt());
        tp.setBenchPoints(team.path("tot_sBenchPoints").asInt());
        tp.setFoulsPersonal(team.path("tot_sFoulsPersonal").asInt());
        tp.setBiggestLead(team.path("tot_sBiggestLead").asInt());
        tp.setBiggestScoringRun(team.path("tot_sBiggestScoringRun").asInt());
        tp.setLeadChanges(team.path("tot_sLeadChanges").asInt());

        return tp;
    }

    // ── Parse individual players ──────────────────────────────────────────────

    private List<IndividualPerformance> parsePlayers(JsonNode team, Game game) {
        List<IndividualPerformance> stats = new ArrayList<>();
        JsonNode players = team.path("pl");

        players.fields().forEachRemaining(entry -> {
            JsonNode p = entry.getValue();
            String minutes = p.path("sMinutes").asText("0:00");

            // Skip players who didn't play
            if (minutes.equals("0:00")) return;

            IndividualPerformance stat = new IndividualPerformance();
            stat.setGame(game);
            stat.setFibaPlayerId(Integer.parseInt(entry.getKey()));
            stat.setFirstName(p.path("firstName").asText());
            stat.setFamilyName(p.path("familyName").asText());
            stat.setShirtNumber(p.path("shirtNumber").asText());
            stat.setMinutesPlayed(minutes);
            stat.setPoints(p.path("sPoints").asInt());
            stat.setReboundsTotal(p.path("sReboundsTotal").asInt());
            stat.setReboundsDefensive(p.path("sReboundsDefensive").asInt());
            stat.setReboundsOffensive(p.path("sReboundsOffensive").asInt());
            stat.setAssists(p.path("sAssists").asInt());
            stat.setSteals(p.path("sSteals").asInt());
            stat.setBlocks(p.path("sBlocks").asInt());
            stat.setTurnovers(p.path("sTurnovers").asInt());
            stat.setFoulsPersonal(p.path("sFoulsPersonal").asInt());
            stat.setFieldGoalsMade(p.path("sFieldGoalsMade").asInt());
            stat.setFieldGoalsAttempted(p.path("sFieldGoalsAttempted").asInt());
            stat.setFieldGoalsPercentage(p.path("sFieldGoalsPercentage").asInt());
            stat.setThreePointersMade(p.path("sThreePointersMade").asInt());
            stat.setThreePointersAttempted(p.path("sThreePointersAttempted").asInt());
            stat.setThreePointersPercentage(p.path("sThreePointersPercentage").asInt());
            stat.setFreeThrowsMade(p.path("sFreeThrowsMade").asInt());
            stat.setFreeThrowsAttempted(p.path("sFreeThrowsAttempted").asInt());
            stat.setFreeThrowsPercentage(p.path("sFreeThrowsPercentage").asInt());
            stat.setPlusMinusPoints(p.path("sPlusMinusPoints").asInt());
            stat.setStarter(p.path("starter").asInt() == 1);

            stats.add(stat);
        });

        return stats;
    }
}