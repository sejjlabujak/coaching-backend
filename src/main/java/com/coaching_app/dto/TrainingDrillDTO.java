package com.coaching_app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.type.descriptor.jdbc.TinyIntAsSmallIntJdbcType;

// dto/TrainingDrillDTO.java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrainingDrillDTO {
    private Long id;
    private Long drillId;
    private String title;
    private Integer duration;
    private Integer orderIndex;
}