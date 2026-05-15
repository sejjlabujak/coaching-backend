package com.coaching_app.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "team_performance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "game")
public class TeamPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @JsonBackReference

    @OneToOne
    @JoinColumn(name = "game_id")
    private Game game;

    // Shooting
    private Integer fieldGoalsMade;
    private Integer fieldGoalsAttempted;
    private Integer fieldGoalsPercentage;
    private Integer threePointersMade;
    private Integer threePointersAttempted;
    private Integer threePointersPercentage;
    private Integer freeThrowsMade;
    private Integer freeThrowsAttempted;
    private Integer freeThrowsPercentage;

    // Rebounding
    private Integer reboundsDefensive;
    private Integer reboundsOffensive;
    private Integer reboundsTotal;

    // Playmaking
    private Integer assists;
    private Integer turnovers;
    private Integer steals;
    private Integer blocks;

    // Scoring
    private Integer points;
    private Integer pointsFastBreak;
    private Integer pointsFromTurnovers;
    private Integer pointsInThePaint;
    private Integer pointsSecondChance;
    private Integer benchPoints;

    // Game flow
    private Integer foulsPersonal;
    private Integer biggestLead;
    private Integer biggestScoringRun;
    private Integer leadChanges;
}