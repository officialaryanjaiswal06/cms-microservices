package com.cms.notification_service.service;


import com.cms.notification_service.model.EmailRequest;
import com.cms.notification_service.model.Otp;
import com.cms.notification_service.model.OtpType;
import com.cms.notification_service.repository.EmailRepository;
import com.cms.notification_service.repository.OtpRepository;
//import jakarta.transaction.Transactional;
import org.springframework.transaction.annotation.Transactional;
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
        log.info("Processing Request for: {} | Type: {}", email, type);
        if (type == null) {
            log.error("Warning: OtpType is NULL. Defaulting to REGISTRATION to prevent crash.");
            type = OtpType.REGISTRATION;
        }

        // 1. Generate Logic
        String otpCode = String.valueOf(new Random().nextInt(900000) + 100000);

        // 2. Save for Validation (Table 1)
//        otpRepository.findByEmail(email).ifPresent(otpRepository::delete);
        otpRepository.deleteByEmailAndType(email, type);
        Otp otp = new Otp();
        otp.setEmail(email);
        otp.setOtpCode(otpCode);
        otp.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        otp.setType(type);


        otpRepository.saveAndFlush(otp);
//        log.info("OTP Saved to DB successfully.");
        log.info("OTP Code persisted to 'otp_tb' for verification.");

        // 3. Queue for Scheduler (Table 2) - This implements your "History" note
        EmailRequest req = new EmailRequest();
        req.setEmail(email);
        req.setSubject(subject);
//        req.setMessageBody(body + otpCode);
        req.setMessageBody(body + "{{OTP}}");
        req.setStatus("PENDING");
        req.setRetryTimes(0);

        emailRepository.save(req);
    }

//    public boolean validate(String email, String inputCode, OtpType type) {
//        // ... (standard logic validation logic)
//        return otpRepository.findByEmailAndType(email,type)
//                .filter(o -> o.getOtpCode().equals(inputCode))
//                .filter(o -> o.getExpiryTime().isAfter(LocalDateTime.now()))
//                .map(o -> {
//                    otpRepository.delete(o);
//                    return true;
//                }).orElse(false);
//    }

    public boolean validate(String email, String inputCode, OtpType type) {
        return otpRepository.findByEmailAndType(email, type)
                .map(o -> {
                    // Check expiry first
                    if (o.getExpiryTime().isBefore(LocalDateTime.now())) {
                        otpRepository.delete(o);
                        return false;
                    }
                    // Check match
                    if (o.getOtpCode().equals(inputCode)) {
//                        otpRepository.delete(o); // ✅ Clean up used OTP
                        return true;
                    }
                    return false;
                }).orElse(false);
    }
}
