package com.coaching_app.controllers;

import com.coaching_app.dto.DrillDTO;
import com.coaching_app.dto.ReuseSessionDTO;
import com.coaching_app.dto.SessionDTO;
import com.coaching_app.dto.SessionDetailDTO;
import com.coaching_app.services.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// controllers/SessionController.java
@RestController
@RequestMapping("/api/sessions")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    // GET /api/sessions?month=4
    @GetMapping
    public ResponseEntity<List<SessionDTO>> getSessions(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(sessionService.getSessions(month, year));
    }

    // GET /api/session/{id}
    @GetMapping("/{id}")
    public ResponseEntity<SessionDetailDTO> getSessionById(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.getSessionById(id));
    }

    // POST /api/session/{id}/reuse
    @PostMapping("/{id}/reuse")
    public ResponseEntity<Map<String, Object>> reuseSession(@PathVariable Long id) {
        Long newSessionId = sessionService.reuseSession(id);
        return ResponseEntity.ok(Map.of("newSessionId", newSessionId, "status", "Copied"));
    }

    // POST /api/sessions  — save new session from builder
    @PostMapping
    public ResponseEntity<Map<String, Object>> createSession(
            @RequestBody SessionDetailDTO dto) {
        Long newId = sessionService.createSession(dto);
        return ResponseEntity.ok(Map.of("id", newId, "status", "Saved"));
    }

    // GET /api/recommendation
//    @GetMapping("/recommendation")
//    public ResponseEntity<List<DrillDTO>> getRecommendations() {
//        return ResponseEntity.ok(sessionService.getRecommendations());
//    }
}