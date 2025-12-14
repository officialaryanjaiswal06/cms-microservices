package com.cms.notification_service.controller;


import com.cms.notification_service.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;

    @PostMapping("/validate")
    public boolean validateOtp(@RequestParam String email, @RequestParam String otp) {
        return otpService.validate(email, otp);
    }
}
