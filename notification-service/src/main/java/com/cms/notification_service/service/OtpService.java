package com.cms.notification_service.service;


import com.cms.notification_service.model.EmailRequest;
import com.cms.notification_service.model.Otp;
import com.cms.notification_service.model.OtpType;
import com.cms.notification_service.repository.EmailRepository;
import com.cms.notification_service.repository.OtpRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.security.auth.Subject;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpRepository otpRepository;
    private final EmailRepository emailRepository;

    @Transactional
    public void generateAndQueueOtp(String email, String subject, String body, OtpType type) {
        log.info("Processing Request for: " + email);

        // 1. Generate Logic
        String otpCode = String.valueOf(new Random().nextInt(900000) + 100000);

        // 2. Save for Validation (Table 1)
        otpRepository.findByEmail(email).ifPresent(otpRepository::delete);
        Otp otp = new Otp();
        otp.setEmail(email);
        otp.setOtpCode(otpCode);
        otp.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        otp.setType(type);
        otpRepository.save(otp);

        // 3. Queue for Scheduler (Table 2) - This implements your "History" note
        EmailRequest req = new EmailRequest();
        req.setEmail(email);
        req.setSubject(subject);
        req.setMessageBody(body + otpCode);
        req.setStatus("PENDING");
        req.setRetryTimes(0);

        emailRepository.save(req);
    }

    public boolean validate(String email, String inputCode, OtpType type) {
        // ... (standard logic validation logic)
        return otpRepository.findByEmailAndType(email,type)
                .filter(o -> o.getOtpCode().equals(inputCode))
                .filter(o -> o.getExpiryTime().isAfter(LocalDateTime.now()))
                .map(o -> {
                    otpRepository.delete(o);
                    return true;
                }).orElse(false);
    }
}
