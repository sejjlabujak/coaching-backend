package com.coaching_app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// dto/SessionDTO.java  — used for calendar list (/api/sessions GET)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SessionDTO {
    private Long id;
    private String title;
    private String date;   // "2026-04-05"
    private String time;
    private Integer duration;
    private String intensity;
    private Boolean readOnly;
}