package com.coaching_app.controllers;

import com.coaching_app.dto.ReuseSessionDTO;
import com.coaching_app.dto.SessionDTO;
import com.coaching_app.dto.SessionDetailDTO;
import com.coaching_app.dto.UpdateSessionDTO;
import com.coaching_app.models.User;
import com.coaching_app.services.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @GetMapping
    public ResponseEntity<List<SessionDTO>> getSessions(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(sessionService.getSessions(month, year, user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionDetailDTO> getSessionById(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.getSessionById(id));
    }

    @PostMapping("/{id}/reuse")
    public ResponseEntity<Map<String, Object>> reuseSession(
            @PathVariable Long id,
            @RequestBody(required = false) ReuseSessionDTO dto,
            @AuthenticationPrincipal User user) {
        String newDate = (dto != null && dto.getNewDate() != null) ? dto.getNewDate() : null;
        Long newSessionId = sessionService.reuseSession(id, newDate, user.getId());
        return ResponseEntity.ok(Map.of("newSessionId", newSessionId, "status", "Copied"));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createSession(
            @RequestBody SessionDetailDTO dto,
            @AuthenticationPrincipal User user) {
        try {
            Long newId = sessionService.createSession(dto, user.getId());
            return ResponseEntity.ok(Map.of("id", newId, "status", "Saved"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateSession(
            @PathVariable Long id,
            @RequestBody UpdateSessionDTO dto,
            @AuthenticationPrincipal User user) {
        try {
            Long updatedId = sessionService.updateSession(id, dto, user.getId());
            return ResponseEntity.ok(Map.of("id", updatedId, "status", "Updated"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/note")
    public ResponseEntity<Map<String, Object>> updateNote(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            sessionService.updateNote(id, body.get("note"));
            return ResponseEntity.ok(Map.of("id", id, "status", "Note updated"));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteSession(@PathVariable Long id) {
        try {
            sessionService.deleteSession(id);
            return ResponseEntity.ok(Map.of("status", "Deleted"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
}
