package com.coaching_app.controllers;

import com.coaching_app.dto.OcrConfirmDTO;
import com.coaching_app.models.User;
import com.coaching_app.services.DrillService;
import com.coaching_app.services.OcrService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/drills")
@RequiredArgsConstructor
public class OcrController {

    private final OcrService ocrService;
    private final DrillService drillService;

    @PostMapping("/ocr-upload")
    public ResponseEntity<Map<String, Object>> uploadOcr(
            @RequestParam("drill_document") MultipartFile file) {

        String contentType = file.getContentType();
        boolean isPdf   = "application/pdf".equals(contentType);
        boolean isImage = contentType != null && contentType.startsWith("image/");

        if (!isPdf && !isImage) {
            throw new IllegalArgumentException("Invalid file type. Please upload a PDF or image file.");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Could not read file", e);
        }

        List<OcrConfirmDTO> drills = ocrService.processBytesAndReturn(bytes, contentType);

        String jobId = java.util.UUID.randomUUID().toString();
        return ResponseEntity.ok(Map.of(
                "jobId", jobId,
                "drills", drills,
                "status", "done"
        ));
    }

    @PostMapping("/ocr-confirm-all")
    public ResponseEntity<Map<String, Object>> confirmAllDrills(
            @RequestBody List<OcrConfirmDTO> drills,
            @AuthenticationPrincipal User user) {
        int saved = drillService.saveAllDrillsFromOcr(drills, user);
        return ResponseEntity.ok(Map.of("saved", saved, "status", "Saved"));
    }

    @PostMapping("/ocr-confirm")
    public ResponseEntity<Map<String, Object>> confirmOcr(
            @RequestBody OcrConfirmDTO dto,
            @AuthenticationPrincipal User user) {
        Long id = drillService.saveDrillFromOcr(dto, user);
        return ResponseEntity.ok(Map.of("id", id, "status", "Saved"));
    }
}
