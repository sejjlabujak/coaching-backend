package com.coaching_app.controllers;

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
        try {
            return ResponseEntity.ok(sessionService.getSessionById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // POST /api/session/{id}/reuse
    @PostMapping("/{id}/reuse")
    public ResponseEntity<Map<String, Object>> reuseSession(
            @PathVariable Long id,
            @RequestBody(required = false) ReuseSessionDTO dto) {
        String newDate = (dto != null && dto.getNewDate() != null) ? dto.getNewDate() : null;
        Long newSessionId = sessionService.reuseSession(id, newDate);
        return ResponseEntity.ok(Map.of("newSessionId", newSessionId, "status", "Copied"));
    }

    // POST /api/sessions
    @PostMapping
    public ResponseEntity<Map<String, Object>> createSession(
            @RequestBody SessionDetailDTO dto) {
        Long newId = sessionService.createSession(dto);
        return ResponseEntity.ok(Map.of("id", newId, "status", "Saved"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteSession(@PathVariable Long id) {
        sessionService.deleteSession(id);
        return ResponseEntity.ok(Map.of("status", "Deleted"));
    }
}