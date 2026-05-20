package com.coaching_app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerStatsDTO {

    // ── Game context ──────────────────────────────────────────────────────────
    private Long gameId;
    private String date;
    private String homeTeam;
    private String awayTeam;
    private String result;
    private String opponent;

    // ── Player identity ───────────────────────────────────────────────────────
    private String firstName;
    private String familyName;
    private String shirtNumber;
    private Boolean starter;
    private String minutesPlayed;

    // ── Scoring ───────────────────────────────────────────────────────────────
    private Integer points;
    private Integer fieldGoalsMade;
    private Integer fieldGoalsAttempted;
    private Integer fieldGoalsPercentage;
    private Integer threePointersMade;
    private Integer threePointersAttempted;
    private Integer threePointersPercentage;
    private Integer freeThrowsMade;
    private Integer freeThrowsAttempted;
    private Integer freeThrowsPercentage;

    // ── Rebounding ────────────────────────────────────────────────────────────
    private Integer reboundsTotal;
    private Integer reboundsOffensive;
    private Integer reboundsDefensive;

    // ── Playmaking ────────────────────────────────────────────────────────────
    private Integer assists;
    private Integer turnovers;
    private Integer steals;
    private Integer blocks;
    private Integer foulsPersonal;
    private Integer plusMinusPoints;
}