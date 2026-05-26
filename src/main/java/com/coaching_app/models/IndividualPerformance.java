package com.coaching_app.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "individual_stats")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndividualPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonBackReference

    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = true)
    private Player player;

    // Player info from FIBA JSON (no FK to Player yet)
    private String firstName;
    private String familyName;
    private String shirtNumber;
    private Integer fibaPlayerId;

    // Stats
    private String minutesPlayed;
    private Integer points;
    private Integer reboundsTotal;
    private Integer reboundsDefensive;
    private Integer reboundsOffensive;
    private Integer assists;
    private Integer steals;
    private Integer blocks;
    private Integer turnovers;
    private Integer foulsPersonal;
    private Integer fieldGoalsMade;
    private Integer fieldGoalsAttempted;
    private Integer fieldGoalsPercentage;
    private Integer threePointersMade;
    private Integer threePointersAttempted;
    private Integer threePointersPercentage;
    private Integer freeThrowsMade;
    private Integer freeThrowsAttempted;
    private Integer freeThrowsPercentage;
    private Integer plusMinusPoints;
    private Boolean starter;
}