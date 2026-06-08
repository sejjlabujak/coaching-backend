package com.coaching_app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerResponseDTO {
    private Long playerID;
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
    private List<ImageDTO> images;
    private List<InjuryResponseDTO> injuries;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ImageDTO {
        private Long imageID;
        private String url;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InjuryResponseDTO {
        private Long id;
        private String description;
        private String startDate;
        private String endDate;
        private boolean active;
    }
}