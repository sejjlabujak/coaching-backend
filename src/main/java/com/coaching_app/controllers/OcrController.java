package com.coaching_app.controllers;

import com.coaching_app.dto.OcrConfirmDTO;
import com.coaching_app.dto.OcrUploadResponseDTO;
import com.coaching_app.services.DrillService;
import com.coaching_app.services.OcrService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    // POST /api/drills/ocr-upload
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

        // Process synchronously — just wait for the result
        List<OcrConfirmDTO> drills = ocrService.processBytesAndReturn(bytes, contentType);

        String jobId = java.util.UUID.randomUUID().toString();
        return ResponseEntity.ok(Map.of(
                "jobId", jobId,
                "drills", drills,
                "status", "done"
        ));
    }

    // POST /api/drills/ocr-confirm-all
    @PostMapping("/ocr-confirm-all")
    public ResponseEntity<Map<String, Object>> confirmAllDrills(
            @RequestBody List<OcrConfirmDTO> drills) {
        int saved = drillService.saveAllDrillsFromOcr(drills);
        return ResponseEntity.ok(Map.of("saved", saved, "status", "Saved"));
    }

    // POST /api/drills/ocr-confirm
    @PostMapping("/ocr-confirm")
    public ResponseEntity<Map<String, Object>> confirmOcr(
            @RequestBody OcrConfirmDTO dto) {
        Long id = drillService.saveDrillFromOcr(dto);
        return ResponseEntity.ok(Map.of("id", id, "status", "Saved"));
    }
}