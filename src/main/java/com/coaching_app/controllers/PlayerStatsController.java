package com.coaching_app.controllers;

import com.coaching_app.dto.PlayerStatsDTO;
import com.coaching_app.models.User;
import com.coaching_app.services.PlayerStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class PlayerStatsController {

    private final PlayerStatsService playerStatsService;

    @GetMapping("/{id}/stats")
    @Transactional(readOnly = true)
    public ResponseEntity<List<PlayerStatsDTO>> getPlayerStats(
            @PathVariable Long id,
            @RequestParam(required = false) String opponent,
            @AuthenticationPrincipal User user) {
        // Initialize lazy-loaded Team proxy within transaction
        String teamName = null;
        if (user != null && user.getTeam() != null) {
            teamName = user.getTeam().getTeamName();
        }
        return ResponseEntity.ok(playerStatsService.getPlayerStats(id, opponent, teamName));
    }
}
