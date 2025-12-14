package com.cms.notification_service.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "SECURITY-SERVICE")
public interface SecurityClient {
    @GetMapping("/users/emails")
    List<String> getEmailsByRole(@RequestParam("roleName") String roleName);

    @GetMapping("/users/emails")
    List<String> getEmailsByRole(
            @RequestParam("roleName") String roleName,
            @RequestParam("strict") boolean strict // <--- Added parameter
    );
}
