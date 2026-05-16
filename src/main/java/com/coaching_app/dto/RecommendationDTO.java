package com.coaching_app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecommendationDTO {
    private String weakArea;
    private String averageStat;
    private String analysis;
    private List<RecommendedDrillDTO> recommendedDrills;
}