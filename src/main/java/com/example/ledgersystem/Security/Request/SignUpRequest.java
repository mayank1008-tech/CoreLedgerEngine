package com.example.ledgersystem.Security.Request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class SignUpRequest {
    @Email
    @NotBlank
    @Size(min = 3, max = 30)
    private String email;

    @NotBlank
    @Size(min = 3, max = 30)
    private String password;

    @NotBlank
    @Size(min = 3, max = 30)
    private String username;

    private Set<String> role;

}
