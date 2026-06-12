package com.coaching_app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BihGameRefDTO {
    private Integer fibaGameId;
    private String homeTeam;
    private String awayTeam;
}