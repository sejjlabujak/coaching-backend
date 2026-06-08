package com.coaching_app.controllers;

import com.coaching_app.dto.InjuryDTO;
import com.coaching_app.services.InjuryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class InjuryController {

    private final InjuryService injuryService;

    // GET /api/players/{id}/injuries — all injuries for a player
    @GetMapping("/api/players/{id}/injuries")
    public ResponseEntity<List<InjuryDTO>> getInjuries(@PathVariable Long id) {
        return ResponseEntity.ok(injuryService.getInjuriesForPlayer(id));
    }

    // GET /api/players/{id}/injuries/active — only active injuries for a player
    @GetMapping("/api/players/{id}/injuries/active")
    public ResponseEntity<List<InjuryDTO>> getActiveInjuries(@PathVariable Long id) {
        return ResponseEntity.ok(injuryService.getActiveInjuriesForPlayer(id));
    }

    // GET /api/injuries/active — all active injuries across all players (dashboard)
    @GetMapping("/api/injuries/active")
    public ResponseEntity<List<InjuryDTO>> getAllActiveInjuries() {
        return ResponseEntity.ok(injuryService.getAllActiveInjuries());
    }

    // POST /api/players/{id}/injuries — log a new injury
    @PostMapping("/api/players/{id}/injuries")
    public ResponseEntity<InjuryDTO> createInjury(
            @PathVariable Long id,
            @RequestBody InjuryDTO dto) {
        return ResponseEntity.ok(injuryService.createInjury(id, dto));
    }

    // PUT /api/injuries/{id} — update description or active status
    @PutMapping("/api/injuries/{id}")
    public ResponseEntity<InjuryDTO> updateInjury(
            @PathVariable Long id,
            @RequestBody InjuryDTO dto) {
        return ResponseEntity.ok(injuryService.updateInjury(id, dto));
    }

    // PATCH /api/injuries/{id}/close — mark injury as resolved (sets endDate automatically)
    @PatchMapping("/api/injuries/{id}/close")
    public ResponseEntity<InjuryDTO> closeInjury(@PathVariable Long id) {
        return ResponseEntity.ok(injuryService.closeInjury(id));
    }

    // DELETE /api/injuries/{id}
    @DeleteMapping("/api/injuries/{id}")
    public ResponseEntity<Map<String, String>> deleteInjury(@PathVariable Long id) {
        injuryService.deleteInjury(id);
        return ResponseEntity.ok(Map.of("status", "Deleted"));
    }
}