package com.coaching_app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecentTeamGameDTO {
    private Long gameId;
    private String date;
    private String opponent;
    private Integer ourScore;
    private Integer opponentScore;
    private String result;
    private Integer fieldGoalsPercentage;
    private Integer threePointersPercentage;
    private Integer freeThrowsPercentage;
    private Integer turnovers;
    private Integer assists;
    private Integer reboundsTotal;
}
