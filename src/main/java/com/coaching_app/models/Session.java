package com.coaching_app.models;

import com.coaching_app.enums.AgeGroup;
import com.coaching_app.enums.IntensityLevel;
import com.coaching_app.enums.TrainingColor;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// models/Session.java
@Entity
@Table(name = "sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDate date;

    private String time;
    private Integer duration;

    @Enumerated(EnumType.STRING)
    private IntensityLevel intensity;

    @Enumerated(EnumType.STRING)
    private TrainingColor color;

    @Enumerated(EnumType.STRING)
    private AgeGroup ageGroup;

    private String focus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<TrainingDrill> drills = new ArrayList<>();
}