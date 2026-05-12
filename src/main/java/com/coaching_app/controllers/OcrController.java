package com.coaching_app.controllers;

import com.coaching_app.dto.OcrConfirmDTO;
import com.coaching_app.dto.OcrUploadResponseDTO;
import com.coaching_app.services.DrillService;
import com.coaching_app.services.OcrJobStore;
import com.coaching_app.services.OcrService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/drills")
@CrossOrigin(origins = "http://localhost:4200")
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
            throw new IllegalArgumentException(
                    "Invalid file type. Please upload a PDF or image file.");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Could not read file", e);
        }

        String jobId = java.util.UUID.randomUUID().toString();

        CompletableFuture.runAsync(() -> {
            ocrService.processBytes(bytes, contentType, jobId);
        });

        return ResponseEntity.accepted()
                .body(Map.of("jobId", jobId, "status", "processing"));
    }

    // GET /api/drills/ocr-status/{jobId}
    @GetMapping("/ocr-status/{jobId}")
    public ResponseEntity<?> getOcrStatus(@PathVariable String jobId) {
        OcrUploadResponseDTO result = OcrJobStore.get(jobId);
        if (result == null) {
            return ResponseEntity.accepted().body(Map.of("status", "processing"));
        }
        return ResponseEntity.ok(result);
    }

    // GET /api/drills/ocr-drills/{jobId} — get extracted drills list for review
    @GetMapping("/ocr-drills/{jobId}")
    public ResponseEntity<?> getExtractedDrills(@PathVariable String jobId) {
        List<OcrConfirmDTO> drills = OcrJobStore.getDrills(jobId);
        if (drills == null) {
            return ResponseEntity.accepted().body(Map.of("status", "processing"));
        }
        return ResponseEntity.ok(drills);
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