package com.coaching_app.controllers;

import com.coaching_app.dto.RecommendationDTO;
import com.coaching_app.services.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/recommendations")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public DeferredResult<ResponseEntity<List<RecommendationDTO>>> getRecommendations() {
        DeferredResult<ResponseEntity<List<RecommendationDTO>>> result =
                new DeferredResult<>(120_000L);

        result.onTimeout(() -> result.setErrorResult(
                ResponseEntity.status(503).body("Request timed out. Please try again later.")
        ));

        CompletableFuture.supplyAsync(() -> {
            try {
                return recommendationService.getRecommendations();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).thenAccept(recommendations ->
                result.setResult(ResponseEntity.ok(recommendations))
        ).exceptionally(ex -> {
            result.setErrorResult(ResponseEntity.internalServerError().build());
            return null;
        });

        return result;
    }

    @PostMapping("/invalidate-cache")
    public ResponseEntity<Void> invalidateCache() {
        recommendationService.invalidateCache();
        return ResponseEntity.ok().build();
    }
}