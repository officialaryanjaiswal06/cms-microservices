package com.cms.notification_service.service;

import com.cms.notification_service.model.EmailRequest;
import com.cms.notification_service.repository.EmailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailScheduler {

    private final EmailRepository emailRepository;
    private final JavaMailSender mailSender;

    // Checks DB every 30 Seconds for pending emails
    @Scheduled(fixedRate = 30000)
    public void processPendingEmails() {

        List<EmailRequest> pendingList =
                emailRepository.findByStatusNotAndRetryTimesLessThan("SENT", 3);

        for (EmailRequest req : pendingList) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo(req.getEmail());
                msg.setSubject(req.getSubject());
                msg.setText(req.getMessageBody());

                mailSender.send(msg);

                // Success
                req.setStatus("SENT");
                emailRepository.save(req);
                log.info("SENT Email to {}", req.getEmail());

            } catch (Exception e) {
                // Failure Logic from Note: Increment Retry
                int attempts = req.getRetryTimes() + 1;
                req.setRetryTimes(attempts);
                log.error("Failed sending email (Try {}): {}", attempts, e.getMessage());

                if (attempts >= 3) {
                    req.setStatus("FAILED"); // Max retries reached
                }

                emailRepository.save(req);
            }
        }
    }
}
