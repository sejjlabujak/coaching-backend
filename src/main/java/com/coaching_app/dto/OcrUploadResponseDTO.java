//package com.coaching_app.dto;
//
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//@Data
//@AllArgsConstructor
//@NoArgsConstructor
//public class OcrUploadResponseDTO {
//    private String extractedTitle;
//    private String extractedText;
//    private Double confidence;
//    private String category;
//    private String complexity;
//}

package com.coaching_app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcrUploadResponseDTO {
    private String extractedTitle;
    private String extractedText;   // description
    private double confidence;
    private String category;        // TrainingFocus
    private String level;
    private String intensity;
    private String duration;
    private String equipment;
}