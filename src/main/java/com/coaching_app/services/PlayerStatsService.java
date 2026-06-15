package com.coaching_app.services;

import com.coaching_app.dto.PlayerStatsDTO;
import com.coaching_app.models.Game;
import com.coaching_app.models.IndividualPerformance;
import com.coaching_app.models.Player;
import com.coaching_app.repositories.PlayerStatsRepository;
import com.coaching_app.repositories.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlayerStatsService {

    private final PlayerRepository playerRepository;
    private final PlayerStatsRepository playerStatsRepository;

    /** Backward-compatible overload — no team context. */
    @Transactional(readOnly = true)
    public List<PlayerStatsDTO> getPlayerStats(Long playerId, String opponent) {
        return getPlayerStats(playerId, opponent, (String) null);
    }

    @Transactional(readOnly = true)
    public List<PlayerStatsDTO> getPlayerStats(Long playerId, String opponent, String ourTeamName) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found: " + playerId));

        List<IndividualPerformance> allStats = playerStatsRepository
                .findByPlayer_PlayerID(playerId);

        if (allStats.isEmpty()) {
            allStats = playerStatsRepository
                    .findByFirstNameIgnoreCaseAndFamilyNameIgnoreCase(
                            player.getFirstName(), player.getLastName()
                    );
        }

        List<IndividualPerformance> withGame = allStats.stream()
                .filter(stat -> stat.getGame() != null)
                .toList();

        // Infer the player's own team from game data — it's the team that appears most across all games
        String playerTeamName = inferPlayerTeam(withGame);

        return withGame.stream()
                .filter(stat -> matchesOpponent(stat.getGame(), opponent, playerTeamName))
                .map(stat -> toDto(stat, playerTeamName))
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Counts how many games each team name appears in (home or away).
     * The player's own team is the one that appears in the most games.
     */
    private String inferPlayerTeam(List<IndividualPerformance> stats) {
        Map<String, Long> counts = stats.stream()
                .flatMap(s -> {
                    List<String> teams = new java.util.ArrayList<>();
                    if (s.getGame().getHomeTeam() != null) teams.add(s.getGame().getHomeTeam());
                    if (s.getGame().getAwayTeam() != null) teams.add(s.getGame().getAwayTeam());
                    return teams.stream();
                })
                .collect(java.util.stream.Collectors.groupingBy(t -> t, java.util.stream.Collectors.counting()));

        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private boolean matchesOpponent(Game game, String opponent, String ourTeamName) {
        if (opponent == null || opponent.isBlank()) return true;

        String opponentLower = opponent.toLowerCase();
        String home = game.getHomeTeam() != null ? game.getHomeTeam().toLowerCase() : "";
        String away = game.getAwayTeam() != null ? game.getAwayTeam().toLowerCase() : "";

        String opponentTeam;
        if (ourTeamName != null) {
            boolean homeIsOurs = home.contains(ourTeamName.toLowerCase());
            opponentTeam = homeIsOurs ? away : home;
        } else {
            // fallback: check which team is not matched by either side — can't know without context
            // return true so no filtering is applied when team is unknown
            return home.contains(opponentLower) || away.contains(opponentLower);
        }

        return opponentTeam.contains(opponentLower);
    }

    private PlayerStatsDTO toDto(IndividualPerformance stat, String ourTeamName) {
        Game game = stat.getGame();

        String home = game.getHomeTeam();
        String away = game.getAwayTeam();
        String opponentName;
        if (ourTeamName != null) {
            boolean homeIsOurs = home != null && home.toLowerCase().contains(ourTeamName.toLowerCase());
            opponentName = homeIsOurs ? away : home;
        } else {
            opponentName = away; // best-effort fallback
        }

        PlayerStatsDTO dto = new PlayerStatsDTO();
        dto.setGameId(game.getId());
        dto.setDate(game.getDate() != null ? game.getDate().toString() : null);
        dto.setHomeTeam(home);
        dto.setAwayTeam(away);
        dto.setResult(game.getResult());
        dto.setOpponent(opponentName);
        dto.setFirstName(stat.getFirstName());
        dto.setFamilyName(stat.getFamilyName());
        dto.setShirtNumber(stat.getShirtNumber());
        dto.setStarter(stat.getStarter());
        dto.setMinutesPlayed(stat.getMinutesPlayed());
        dto.setPoints(stat.getPoints());
        dto.setFieldGoalsMade(stat.getFieldGoalsMade());
        dto.setFieldGoalsAttempted(stat.getFieldGoalsAttempted());
        dto.setFieldGoalsPercentage(stat.getFieldGoalsPercentage());
        dto.setThreePointersMade(stat.getThreePointersMade());
        dto.setThreePointersAttempted(stat.getThreePointersAttempted());
        dto.setThreePointersPercentage(stat.getThreePointersPercentage());
        dto.setFreeThrowsMade(stat.getFreeThrowsMade());
        dto.setFreeThrowsAttempted(stat.getFreeThrowsAttempted());
        dto.setFreeThrowsPercentage(stat.getFreeThrowsPercentage());
        dto.setReboundsTotal(stat.getReboundsTotal());
        dto.setReboundsOffensive(stat.getReboundsOffensive());
        dto.setReboundsDefensive(stat.getReboundsDefensive());
        dto.setAssists(stat.getAssists());
        dto.setTurnovers(stat.getTurnovers());
        dto.setSteals(stat.getSteals());
        dto.setBlocks(stat.getBlocks());
        dto.setFoulsPersonal(stat.getFoulsPersonal());
        dto.setPlusMinusPoints(stat.getPlusMinusPoints());

        return dto;
    }
}
