package com.coaching_app.controllers;

import com.coaching_app.dto.OcrConfirmDTO;
import com.coaching_app.dto.OcrUploadResponseDTO;
import com.coaching_app.services.DrillService;
import com.coaching_app.services.OcrService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

// controllers/OcrController.java
@RestController
@RequestMapping("/api/drills")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class OcrController {

    private final OcrService ocrService;
    private final DrillService drillService;

    // POST /api/drills/ocr-upload
    @PostMapping("/ocr-upload")
    public ResponseEntity<OcrUploadResponseDTO> uploadOcr(
            @RequestParam("drill_document") MultipartFile file) {
        return ResponseEntity.ok(ocrService.processOcr(file));
    }

    // POST /api/drills/ocr-confirm
    @PostMapping("/ocr-confirm")
    public ResponseEntity<Map<String, Object>> confirmOcr(
            @RequestBody OcrConfirmDTO dto) {
        Long id = drillService.saveDrillFromOcr(dto);
        return ResponseEntity.ok(Map.of("id", id, "status", "Saved"));
    }
}