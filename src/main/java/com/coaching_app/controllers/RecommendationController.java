package com.coaching_app.controllers;

import com.coaching_app.dto.RecommendationDTO;
import com.coaching_app.models.User;
import com.coaching_app.repositories.UserRepository;
import com.coaching_app.services.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getRecommendations(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        User fresh = userRepository.findById(user.getId()).orElse(null);
        if (fresh == null || fresh.getTeam() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "No team assigned"));
        }

        try {
            List<RecommendationDTO> recommendations = recommendationService.getRecommendationsForCoach(fresh);
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/invalidate-cache")
    public ResponseEntity<Void> invalidateCache() {
        recommendationService.invalidateCache();
        return ResponseEntity.ok().build();
    }
}
