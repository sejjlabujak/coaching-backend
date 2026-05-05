package com.coaching_app.services;

import com.coaching_app.dto.OcrUploadResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

// services/OcrService.java
@Service
public class OcrService {

    public OcrUploadResponseDTO processOcr(MultipartFile file) {
        // For now: return dummy parsed data
        // Later: integrate with Tesseract4J or call external OCR
        return new OcrUploadResponseDTO(
            "Extracted Drill Title",
            "Full extracted text from PDF...",
            0.94,
            "Shooting",
            "Intermediate"
        );
    }
}