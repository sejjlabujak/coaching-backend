package com.coaching_app.controllers;

import com.coaching_app.models.User;
import com.coaching_app.repositories.UserRepository;
import com.coaching_app.security.JwtUtil;
import com.coaching_app.services.PlayerSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PlayerSyncService playerSyncService;

    @Transactional(readOnly = true)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }

        String token = jwtUtil.generateToken(user);
        String teamName = (user.getTeam() != null) ? user.getTeam().getTeamName() : null;

        java.util.Map<String, Object> resp = new java.util.LinkedHashMap<>();
        resp.put("token", token);
        resp.put("mustChangePassword", user.getMustChangePassword());
        resp.put("role", user.getRole().name());
        resp.put("teamName", teamName);
        return ResponseEntity.ok(resp);
    }

    @Transactional
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !passwordEncoder.matches(oldPassword, user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);

//        playerSyncService.syncRosterForCoach(user);

        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
}
