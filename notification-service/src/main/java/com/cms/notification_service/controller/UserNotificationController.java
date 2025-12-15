package com.cms.notification_service.controller;

import com.cms.notification_service.model.EmailRequest;
import com.cms.notification_service.model.NotificationCategory;
import com.cms.notification_service.repository.EmailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class UserNotificationController {
    private final EmailRepository emailRepository;

    @GetMapping("/mine")
    public ResponseEntity<List<EmailRequest>> getMyNotifications(
            @RequestParam String email // Pass the email to look up
    ) {

        // 1. Fetch data logic
        // findByEmail + Category NOT 'OTP' + Order Newest First
        List<EmailRequest> myNotifications = emailRepository
                .findByEmailAndCategoryNotOrderByCreatedAtDesc(
                        email,
                        NotificationCategory.OTP // <--- This filters out the secret codes
                );

        return ResponseEntity.ok(myNotifications);
    }
}
