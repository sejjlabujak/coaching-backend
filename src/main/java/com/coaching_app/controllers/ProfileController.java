package com.coaching_app.controllers;

import com.coaching_app.dto.ProfileDTO;
import com.coaching_app.dto.UpdateProfileDTO;
import com.coaching_app.models.User;
import com.coaching_app.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;

    private Long currentUserId() {
        User principal = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal.getId();
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ProfileDTO> getProfile() {
        User user = userRepository.findById(currentUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        String teamName = user.getTeam() != null ? user.getTeam().getTeamName() : null;
        return ResponseEntity.ok(new ProfileDTO(
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                teamName
        ));
    }

    @PutMapping
    @Transactional
    public ResponseEntity<Map<String, String>> updateProfile(@RequestBody UpdateProfileDTO dto) {
        User user = userRepository.findById(currentUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            user.setEmail(dto.getEmail());
        }
        return ResponseEntity.ok(Map.of("status", "Updated"));
    }
}
