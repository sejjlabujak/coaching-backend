package com.coaching_app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// dto/SessionDetailDTO.java  — used for /api/session/{id} GET
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SessionDetailDTO {
    private Long id;
    private String title;
    private String date;
    private Integer duration;
    private String intensity;
    private String focus;
    private String ageGroup;
    private List<TrainingDrillDTO> drills;
}