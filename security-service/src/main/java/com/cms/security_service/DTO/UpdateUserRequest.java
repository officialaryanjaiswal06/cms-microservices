package com.cms.security_service.DTO;

public class UpdateUserRequest {
    private String email;
    private String password; // optional; if null/blank, don't change

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
