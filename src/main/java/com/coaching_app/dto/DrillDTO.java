package com.coaching_app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// dto/DrillDTO.java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DrillDTO {
    private Long id;
    private String title;
    private String description;
    private String focus;
    private String intensity;
    private String ageGroup;
    private String tag;
    private Integer duration;
    private String level;
    private String equipment;
}