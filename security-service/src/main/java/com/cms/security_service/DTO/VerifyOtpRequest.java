package com.cms.security_service.DTO;

import lombok.Data;

@Data
public class VerifyOtpRequest {
    private String email;
    private String otp;
}
