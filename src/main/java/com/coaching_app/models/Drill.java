package com.coaching_app.models;

import com.coaching_app.enums.AgeGroup;
import com.coaching_app.enums.IntensityLevel;
import com.coaching_app.enums.TrainingFocus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// models/Drill.java
@Entity
@Table(name = "drills")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Drill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private TrainingFocus focus;

    @Enumerated(EnumType.STRING)
    private IntensityLevel intensity;

    @Enumerated(EnumType.ORDINAL)
    private AgeGroup ageGroup;

    private String tag;
    private Integer duration;
    private String level; // Beginner, Intermediate, Advanced
    private String equipment; // comma-separated

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean deleted = false;
}