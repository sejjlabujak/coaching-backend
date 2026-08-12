package com.coaching_app.dto;

import lombok.Data;

@Data
public class PlayerRequestDTO {
    private String firstName;
    private String lastName;
    private String position;
    private Integer jerseyNumber;
    private Integer heightCm;
    private Integer weightKg;
    private String birthDate;
    private String birthCity;
    private String nationality;
    private String ageGroup;
}
