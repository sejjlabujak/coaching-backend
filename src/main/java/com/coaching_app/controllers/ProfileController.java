package com.coaching_app.controllers;

import com.coaching_app.dto.ProfileDTO;
import com.coaching_app.dto.UpdateProfileDTO;
import com.coaching_app.models.User;
import com.coaching_app.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ProfileDTO> getProfile(@AuthenticationPrincipal User user) {
        User loaded = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        String teamName = loaded.getTeam() != null ? loaded.getTeam().getTeamName() : null;
        return ResponseEntity.ok(new ProfileDTO(
                loaded.getUsername(),
                loaded.getEmail(),
                loaded.getRole().name(),
                teamName
        ));
    }

    @PutMapping
    @Transactional
    public ResponseEntity<Map<String, String>> updateProfile(
            @RequestBody UpdateProfileDTO dto,
            @AuthenticationPrincipal User user) {
        User loaded = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            loaded.setEmail(dto.getEmail());
        }
        return ResponseEntity.ok(Map.of("status", "Updated"));
    }
}
