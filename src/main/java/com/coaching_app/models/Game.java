package com.coaching_app.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "games")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer fibaGameId;
    private String homeTeam;
    private String awayTeam;
    private Integer homeScore;
    private Integer awayScore;
    private String result;
    private LocalDate date;
    private String competition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id")
    @ToString.Exclude
    private Team homeTeamRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id")
    @ToString.Exclude
    private Team awayTeamRef;

    @ToString.Exclude
    @OneToOne(mappedBy = "game", cascade = CascadeType.ALL)
    private TeamPerformance teamPerformance;

    @JsonManagedReference
    @JsonIgnore
    @ToString.Exclude
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL)
    private List<IndividualPerformance> individualStats = new ArrayList<>();
}