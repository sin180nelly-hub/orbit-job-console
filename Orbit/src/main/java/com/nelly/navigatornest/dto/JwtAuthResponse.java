package com.nelly.navigatornest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtAuthResponse {
    private String token;
    private String type = "Bearer";

    public JwtAuthResponse(String token) {
        this.token = token;
    }
}