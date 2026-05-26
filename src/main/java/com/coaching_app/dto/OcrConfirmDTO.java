package com.coaching_app.dto;

import lombok.Data;

@Data
public class OcrConfirmDTO {
    private String title;
    private String description;
    private String focus;
    private String intensity;
    private String ageGroup;
    private String level;
    private String equipment;
    private Integer duration;
}