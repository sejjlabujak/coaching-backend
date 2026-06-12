package com.coaching_app.controllers;

import com.coaching_app.dto.TeamOptionDTO;
import com.coaching_app.enums.UserRole;
import com.coaching_app.models.Team;
import com.coaching_app.models.User;
import com.coaching_app.repositories.TeamRepository;
import com.coaching_app.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    // ── Teams ─────────────────────────────────────────────────────────────────

    @GetMapping("/teams")
    public ResponseEntity<List<TeamOptionDTO>> listTeams() {
        List<TeamOptionDTO> teams = teamRepository.findAll().stream()
                .map(t -> new TeamOptionDTO(t.getTeamName(), t.getLogoUrl()))
                .toList();
        return ResponseEntity.ok(teams);
    }

    // ── Coaches ───────────────────────────────────────────────────────────────

    @PostMapping("/coaches")
    public ResponseEntity<?> createCoach(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String email    = body.get("email");
        String teamName = body.get("teamName");

        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
        }
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
        }

        Team team = teamRepository.findByTeamName(teamName)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Unknown team: " + teamName));

        User coach = new User();
        coach.setUsername(username);
        coach.setPassword(passwordEncoder.encode(password));
        coach.setEmail(email);
        coach.setRole(UserRole.COACH);
        coach.setTeam(team);
        coach.setMustChangePassword(true);
        coach.setActive(true);

        User saved = userRepository.save(coach);
        return ResponseEntity.ok(Map.of(
                "id",       saved.getId(),
                "username", saved.getUsername(),
                "teamName", saved.getTeam().getTeamName()
        ));
    }

    @Transactional(readOnly = true)
    @GetMapping("/coaches")
    public ResponseEntity<List<Map<String, Object>>> listCoaches() {
        List<Map<String, Object>> coaches = userRepository.findByRole(UserRole.COACH).stream()
                .map(u -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id",       u.getId());
                    m.put("username", u.getUsername());
                    m.put("email",    u.getEmail());
                    m.put("teamName", u.getTeam() != null ? u.getTeam().getTeamName() : "");
                    m.put("active",   u.getActive());
                    return m;
                })
                .toList();
        return ResponseEntity.ok(coaches);
    }

    @Transactional
    @PatchMapping("/coaches/{id}")
    public ResponseEntity<?> updateCoach(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User coach = userRepository.findById(id)
                .filter(u -> u.getRole() == UserRole.COACH)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coach not found"));

        String username = body.get("username");
        String email    = body.get("email");
        String teamName = body.get("teamName");

        if (username != null && !username.isBlank() && !username.equals(coach.getUsername())) {
            if (userRepository.existsByUsername(username)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username already taken"));
            }
            coach.setUsername(username.trim());
        }
        if (email != null && !email.isBlank() && !email.equals(coach.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email already taken"));
            }
            coach.setEmail(email.trim());
        }
        if (teamName != null && !teamName.isBlank()) {
            Team team = teamRepository.findByTeamName(teamName)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown team: " + teamName));
            coach.setTeam(team);
        }

        userRepository.save(coach);
        return ResponseEntity.ok(Map.of("id", coach.getId(), "username", coach.getUsername()));
    }

    @PatchMapping("/coaches/{id}/deactivate")
    public ResponseEntity<?> deactivateCoach(@PathVariable Long id) {
        return userRepository.findById(id)
                .filter(u -> u.getRole() == UserRole.COACH)
                .map(u -> {
                    u.setActive(false);
                    userRepository.save(u);
                    return ResponseEntity.ok(Map.of("id", u.getId(), "active", false));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/coaches/{id}/activate")
    public ResponseEntity<?> activateCoach(@PathVariable Long id) {
        return userRepository.findById(id)
                .filter(u -> u.getRole() == UserRole.COACH)
                .map(u -> {
                    u.setActive(true);
                    userRepository.save(u);
                    return ResponseEntity.ok(Map.of("id", u.getId(), "active", true));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
