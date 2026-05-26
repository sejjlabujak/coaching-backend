package com.coaching_app.services;

import com.coaching_app.dto.PlayerStatsDTO;
import com.coaching_app.models.Game;
import com.coaching_app.models.IndividualPerformance;
import com.coaching_app.models.Player;
import com.coaching_app.repositories.PlayerStatsRepository;
import com.coaching_app.repositories.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerStatsService {

    private static final String OUR_TEAM = "Play Off";

    private final PlayerRepository playerRepository;
    private final PlayerStatsRepository playerStatsRepository;
    public List<PlayerStatsDTO> getPlayerStats(Long playerId, String opponent) {
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

        return allStats.stream()
                .filter(stat -> stat.getGame() != null)
                .filter(stat -> matchesOpponent(stat.getGame(), opponent))
                .map(this::toDto)
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean matchesOpponent(Game game, String opponent) {
        if (opponent == null || opponent.isBlank()) return true;

        String opponentLower = opponent.toLowerCase();
        String home = game.getHomeTeam() != null ? game.getHomeTeam().toLowerCase() : "";
        String away = game.getAwayTeam() != null ? game.getAwayTeam().toLowerCase() : "";

        boolean homeIsOurs = home.contains(OUR_TEAM.toLowerCase());
        String opponentTeam = homeIsOurs ? away : home;

        return opponentTeam.contains(opponentLower);
    }

    private PlayerStatsDTO toDto(IndividualPerformance stat) {
        Game game = stat.getGame();

        String home = game.getHomeTeam();
        String away = game.getAwayTeam();
        boolean homeIsOurs = home != null && home.contains(OUR_TEAM);
        String opponentName = homeIsOurs ? away : home;

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