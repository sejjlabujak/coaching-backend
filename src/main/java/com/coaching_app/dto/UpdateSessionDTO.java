package com.coaching_app.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSessionDTO {
    private String title;
    private String date;
    private String time;
    private Integer duration;
    private String intensity;
    private String focus;
    private String ageGroup;
    private List<TrainingDrillDTO> drills;
    private String note;
}
