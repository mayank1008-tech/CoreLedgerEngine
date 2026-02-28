package com.example.ledgersystem.Security.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoResponse {
    private UUID id;
    private String username;
    private String jwtToken;


    public UserInfoResponse(UUID userId, String username) {
        this.username = username;
        this.id = userId;
    }
}
