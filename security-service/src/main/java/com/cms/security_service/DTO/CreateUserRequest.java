package com.cms.security_service.DTO;

import jakarta.persistence.Entity;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Data
@Getter
@Setter
@ToString
@NoArgsConstructor // ✅ Critical for JSON
@AllArgsConstructor
public class CreateUserRequest {
    private String username;
    private String password;
    private String email;
    private Set<String> roles = new HashSet<>(); // optional; default to ROLE_USER

}