package com.coaching_app.services;

import com.coaching_app.dto.BihGameRefDTO;
import com.coaching_app.models.Game;
import com.coaching_app.models.IndividualPerformance;
import com.coaching_app.models.Team;
import com.coaching_app.models.TeamPerformance;
import com.coaching_app.repositories.GameRepository;
import com.coaching_app.repositories.TeamRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FibaParserService {

    private final GameRepository gameRepository;
    private final FibaWidgetScraperService scraperService;
    private final PlayerSyncService playerSyncService;
    private final TeamRepository teamRepository;
    private final ObjectMapper objectMapper;

    private List<BihGameRefDTO> cachedGameRefs = null;

    // ── Load game list from JSON
    private List<BihGameRefDTO> loadGameIdsFromJson() {
        if (cachedGameRefs != null) return cachedGameRefs;
        try {
            ClassPathResource resource = new ClassPathResource("bih_women_games.json");
            cachedGameRefs = objectMapper.readValue(
                    resource.getInputStream(),
                    new TypeReference<List<BihGameRefDTO>>() {}
            );
            log.info("Loaded {} game refs from bih_women_games.json", cachedGameRefs.size());
            return cachedGameRefs;
        } catch (IOException e) {
            log.error("Failed to load bih_women_games.json: {}", e.getMessage());
            return List.of();
        }
    }

    // ── Import all known games
    public List<Game> importAllGames() {
        List<Game> saved = new ArrayList<>();
        for (BihGameRefDTO ref : loadGameIdsFromJson()) {
            try {
                Game game = importGame(ref.getFibaGameId());
                if (game != null) saved.add(game);
            } catch (Exception e) {
                log.warn("Failed to import game {}: {}", ref.getFibaGameId(), e.getMessage());
            }
        }
        log.info("Imported {} games", saved.size());
        return saved;
    }

    // ── Import single game
    public Game importGame(int fibaId) {
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

        int homeScore = team1.path("score").asInt();
        int awayScore = team2.path("score").asInt();
        String result = homeScore >= awayScore ? "WIN" : "LOSS";

        Game game = new Game();
        game.setFibaGameId(fibaId);
        game.setHomeTeam(team1Name);
        game.setAwayTeam(team2Name);
        game.setHomeScore(homeScore);
        game.setAwayScore(awayScore);
        game.setResult(result);
        game.setDate(LocalDate.now());
        game.setCompetition("BIH Women's Premier League");

        TeamPerformance tp = parseTeamPerformance(team1, game);
        game.setTeamPerformance(tp);

        List<IndividualPerformance> stats = parsePlayers(team1, game);
        game.setIndividualStats(stats);

        Game saved = gameRepository.save(game);
        log.info("Saved game {} — {} {} : {} {}", fibaId, team1Name, homeScore, awayScore, result);

        linkTeamRefs(saved, team1Name, team2Name);

//        List<PlayerSyncService.FibaPlayerRef> fibaPlayers = collectPlayerRefs(team1);
//        if (!fibaPlayers.isEmpty()) {
//            playerSyncService.syncPlayersFromGame(fibaPlayers);
//        }
        return saved;
    }

    // ── Relink all existing games to Team FKs (backfill)
    public int[] relinkAllGames() {
        List<Game> all = gameRepository.findAll();
        int relinked = 0;
        int unmatched = 0;

        for (Game game : all) {
            Team home = resolveTeam(game.getHomeTeam());
            Team away = resolveTeam(game.getAwayTeam());

            if (home == null) unmatched++;
            if (away == null) unmatched++;

            game.setHomeTeamRef(home);
            game.setAwayTeamRef(away);
            gameRepository.save(game);
            relinked++;
        }

        log.info("Relinked {} games, {} unmatched team names", relinked, unmatched);
        return new int[]{relinked, unmatched};
    }

    // ── Match FIBA name to Team row
    private void linkTeamRefs(Game game, String homeTeamName, String awayTeamName) {
        game.setHomeTeamRef(resolveTeam(homeTeamName));
        game.setAwayTeamRef(resolveTeam(awayTeamName));
        gameRepository.save(game);
    }

    private Team resolveTeam(String fibaName) {
        if (fibaName == null || fibaName.isBlank()) return null;

        Optional<Team> exact = teamRepository.findByTeamName(fibaName);
        if (exact.isPresent()) return exact.get();

        String fibaLower = fibaName.toLowerCase();
        for (Team t : teamRepository.findAll()) {
            String dbLower = t.getTeamName().toLowerCase();
            if (dbLower.contains(fibaLower) || fibaLower.contains(dbLower)) {
                return t;
            }
        }

        log.warn("No Team row found for FIBA name '{}' — FK will be null", fibaName);
        return null;
    }

    // ── Collect player refs
    private List<PlayerSyncService.FibaPlayerRef> collectPlayerRefs(JsonNode team) {
        List<PlayerSyncService.FibaPlayerRef> refs = new ArrayList<>();
        JsonNode players = team.path("pl");

        players.fields().forEachRemaining(entry -> {
            JsonNode p = entry.getValue();
            String minutes = p.path("sMinutes").asText("0:00");
            if (minutes.equals("0:00")) return;

            String firstName   = p.path("firstName").asText();
            String familyName  = p.path("familyName").asText();
            String shirtNumber = p.path("shirtNumber").asText();

            if (!firstName.isBlank() || !familyName.isBlank()) {
                refs.add(new PlayerSyncService.FibaPlayerRef(firstName, familyName, shirtNumber));
            }
        });

        return refs;
    }

    // ── Parse team performance
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

    // ── Parse individual players
    private List<IndividualPerformance> parsePlayers(JsonNode team, Game game) {
        List<IndividualPerformance> stats = new ArrayList<>();
        JsonNode players = team.path("pl");

        players.fields().forEachRemaining(entry -> {
            JsonNode p = entry.getValue();
            String minutes = p.path("sMinutes").asText("0:00");
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
