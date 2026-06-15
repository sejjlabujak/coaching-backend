package com.coaching_app.controllers;

import com.coaching_app.dto.PlayerStatsDTO;
import com.coaching_app.models.User;
import com.coaching_app.repositories.UserRepository;
import com.coaching_app.services.PlayerStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class PlayerStatsController {

    private final PlayerStatsService playerStatsService;
    private final UserRepository userRepository;

    @GetMapping("/{id}/stats")
    @Transactional(readOnly = true)
    public ResponseEntity<List<PlayerStatsDTO>> getPlayerStats(
            @PathVariable Long id,
            @RequestParam(required = false) String opponent) {
        User principal = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        String teamName = user.getTeam() != null ? user.getTeam().getTeamName() : null;
        return ResponseEntity.ok(playerStatsService.getPlayerStats(id, opponent, teamName));
    }
}
