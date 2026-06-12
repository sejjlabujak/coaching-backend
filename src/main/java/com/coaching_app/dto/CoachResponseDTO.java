package com.coaching_app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CoachResponseDTO {
    private Long id;
    private String username;
    private String email;
    private String teamName;
    private boolean mustChangePassword;
}
