package com.coaching_app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InjuryDTO {
    private Long id;
    private String description;
    private String startDate;   // "2026-05-01"
    private String endDate;     // null if still active
    private Boolean isActive;
    private Long playerId;
    private String playerName;  // firstName + lastName for convenience
}