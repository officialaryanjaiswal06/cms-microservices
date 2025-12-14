package com.cms.security_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "notification-service")
public interface NotificationClient {

    @PostMapping("/otp/validate")
    boolean validateOtp(@RequestParam("email") String email,
                        @RequestParam("otp") String otp);
}