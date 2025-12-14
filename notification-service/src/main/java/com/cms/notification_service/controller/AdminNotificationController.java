package com.cms.notification_service.controller;


import com.cms.notification_service.client.SecurityClient;
import com.cms.notification_service.model.EmailRequest;
import com.cms.notification_service.repository.EmailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/push")
@RequiredArgsConstructor
@Slf4j
public class AdminNotificationController {
    private final EmailRepository emailRepository;

    private final SecurityClient securityClient;


    // endpoint to manually push notification
    @PostMapping("/manual")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN') or hasAuthority('NOTIFICATION_CREATE')")
    public ResponseEntity<String> manualPushNotification(@RequestBody EmailRequest request) {

        log.info("Received Admin Push Request for recipient: {}", request.getEmail());

        // 1. Validate Input (Basic)
        if (request.getEmail() == null || request.getMessageBody() == null) {
            return ResponseEntity.badRequest().body("Error: Email and Message Body are required.");
        }

        // 2. Setup Defaults (Ensures the Scheduler picks it up)
        request.setStatus("PENDING");
        request.setRetryTimes(0);
        request.setCreatedAt(LocalDateTime.now()); // Ensure timestamp is current

        // 3. Save to Database
        emailRepository.save(request);

        log.info("Notification successfully queued with ID: {}. Scheduler will process it shortly.", request.getId());

        return ResponseEntity.ok("Notification Queued Successfully. Status: PENDING");
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN') or hasAuthority('NOTIFICATION_READ')")
    public ResponseEntity<List<EmailRequest>> getAllNotificationHistory() {

        List<EmailRequest> history = emailRepository.findAll();
        return ResponseEntity.ok(history);
    }
    // delete notification by id
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN') or hasAuthority('NOTIFICATION_DELETE')")
    public ResponseEntity<String> deleteNotification(@PathVariable Long id) {

        if (emailRepository.existsById(id)) {
            emailRepository.deleteById(id);
            log.info("Deleted notification log with ID: {}", id);
            return ResponseEntity.ok("Notification Log Deleted");
        }

        return ResponseEntity.status(404).body("Notification ID not found");
    }

    @PostMapping("/broadcast/users-only")
//    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN') or hasAuthority('NOTIFICATION_CREATE')")
    public String broadcastToStrictUsers(@RequestBody EmailRequest requestTemplate) {

        log.info("Fetching strict USERS list from Security Service...");

        // 1. Fetch Emails: Role = "USER", Strict = TRUE
        List<String> recipients = securityClient.getEmailsByRole("USER", true);

        log.info("Found {} strict users. Queueing notifications...", recipients.size());

        if (recipients.isEmpty()) {
            return "No users found with ONLY 'USER' role.";
        }

        // 2. Loop and Save to DB (Status: PENDING)
        int count = 0;
        for (String email : recipients) {
            EmailRequest req = new EmailRequest();
            req.setEmail(email);
            req.setSubject(requestTemplate.getSubject());
            req.setMessageBody(requestTemplate.getMessageBody());
            req.setStatus("PENDING");
            req.setRetryTimes(0);

            emailRepository.save(req);
            count++;
        }

        return "Broadcast successfully queued for " + count + " specific users.";
    }

}
