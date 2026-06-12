package com.coaching_app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponseDTO {
    private String token;
    private String role;
    private boolean mustChangePassword;
    private String teamName;
}
