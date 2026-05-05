package com.coaching_app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// dto/OcrUploadResponseDTO.java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OcrUploadResponseDTO {
    private String extractedTitle;
    private String extractedText;
    private Double confidence;
    private String category;
    private String complexity;
}