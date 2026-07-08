package com.coaching_app.controllers;

import com.coaching_app.dto.DrillDTO;
import com.coaching_app.models.User;
import com.coaching_app.services.DrillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/drills")
@RequiredArgsConstructor
public class DrillController {

    private final DrillService drillService;

    @GetMapping
    public ResponseEntity<List<DrillDTO>> getDrills(
            @RequestParam(required = false) String focus,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(drillService.getDrills(focus, search, user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DrillDTO> getDrillById(@PathVariable Long id) {
        return ResponseEntity.ok(drillService.getDrillById(id));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createDrill(
            @RequestBody DrillDTO dto,
            @AuthenticationPrincipal User user) {
        try {
            Long newId = drillService.createDrill(dto, user);
            return ResponseEntity.ok(Map.of("id", newId, "status", "Saved"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateDrill(
            @PathVariable Long id,
            @RequestBody DrillDTO dto) {
        drillService.updateDrill(id, dto);
        return ResponseEntity.ok(Map.of("id", id, "status", "Updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteDrill(@PathVariable Long id) {
        drillService.deleteDrill(id);
        return ResponseEntity.ok(Map.of(
            "status", "Success",
            "message", "Drill moved to trash"
        ));
    }
}
