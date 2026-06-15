package com.coaching_app.controllers;

import com.coaching_app.dto.RecentTeamGameDTO;
import com.coaching_app.models.Game;
import com.coaching_app.models.TeamPerformance;
import com.coaching_app.models.User;
import com.coaching_app.repositories.GameRepository;
import com.coaching_app.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/team-stats")
@RequiredArgsConstructor
public class TeamStatsController {

    private final UserRepository userRepository;
    private final GameRepository gameRepository;

    @GetMapping("/recent")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getRecent(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof User principal)) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        User user = userRepository.findById(principal.getId()).orElse(null);
        if (user == null || user.getTeam() == null) {
            return ResponseEntity.ok(List.of());
        }

        Long teamId = user.getTeam().getId();
        List<Game> games = gameRepository.findTop3ByHomeTeamRef_IdOrderByDateDesc(teamId);

        List<RecentTeamGameDTO> result = new ArrayList<>();
        for (Game game : games) {
            TeamPerformance tp = game.getTeamPerformance();
            if (tp == null) continue;

            Integer ourScore = game.getHomeScore();
            Integer oppScore = game.getAwayScore();
            String resultStr = (ourScore != null && oppScore != null && ourScore > oppScore) ? "W" : "L";

            result.add(new RecentTeamGameDTO(
                game.getId(),
                game.getDate() != null ? game.getDate().toString() : null,
                game.getAwayTeam(),
                ourScore,
                oppScore,
                resultStr,
                tp.getFieldGoalsPercentage(),
                tp.getThreePointersPercentage(),
                tp.getFreeThrowsPercentage(),
                tp.getTurnovers(),
                tp.getAssists(),
                tp.getReboundsTotal()
            ));
        }

        return ResponseEntity.ok(result);
    }
}
