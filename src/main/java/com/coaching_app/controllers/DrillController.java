package com.coaching_app.controllers;

import com.coaching_app.dto.DrillDTO;
import com.coaching_app.models.User;
import com.coaching_app.services.DrillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// controllers/DrillController.java
@RestController
@RequestMapping("/api/drills")
@RequiredArgsConstructor
public class DrillController {

    private final DrillService drillService;

    private User currentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // GET /api/drills?focus=defense&search=layup&focus=high&ageGroup=U16
    @GetMapping
    public ResponseEntity<List<DrillDTO>> getDrills(
            @RequestParam(required = false) String focus,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(drillService.getDrills(focus, search, currentUser()));
    }

    // GET /api/drill/{id}
    @GetMapping("/{id}")
    public ResponseEntity<DrillDTO> getDrillById(@PathVariable Long id) {
        return ResponseEntity.ok(drillService.getDrillById(id));
    }

    // POST /api/drills
    @PostMapping
    public ResponseEntity<Map<String, Object>> createDrill(@RequestBody DrillDTO dto) {
        Long newId = drillService.createDrill(dto, currentUser());
        return ResponseEntity.ok(Map.of("id", newId, "status", "Saved"));
    }

    // PUT /api/drill/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateDrill(
            @PathVariable Long id,
            @RequestBody DrillDTO dto) {
        drillService.updateDrill(id, dto);
        return ResponseEntity.ok(Map.of("id", id, "status", "Updated"));
    }

    // DELETE /api/drill/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteDrill(@PathVariable Long id) {
        drillService.deleteDrill(id);
        return ResponseEntity.ok(Map.of(
            "status", "Success",
            "message", "Drill moved to trash"
        ));
    }
}