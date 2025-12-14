package com.cms.security_service.DTO;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String email;
    private String otp;
    private String newPassword;
}
