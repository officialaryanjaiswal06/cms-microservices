package com.cms.security_service.services;

import com.cms.security_service.DTO.OtpMessageDto;
import com.cms.security_service.client.NotificationClient;
import com.cms.security_service.model.OtpType;
import com.cms.security_service.model.Role;
import com.cms.security_service.model.Users;
import com.cms.security_service.repository.RoleRepo;
import com.cms.security_service.repository.UsersRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {
    @Autowired
    private UsersRepo usersRepo;

    @Autowired
    private RoleRepo roleRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private NotificationClient notificationClient;

    @Transactional
    public String registerUser(Users userIn) {
        // Validation Checks
        if (usersRepo.existsByUsername(userIn.getUsername())) {
            throw new RuntimeException("Error: Username is already taken!");
        }
        if (usersRepo.existsByEmail(userIn.getEmail())) {
            throw new RuntimeException("Error: Email is already registered!");
        }

        // Create new User entity
        Users newUser = new Users();
        newUser.setUsername(userIn.getUsername());
        newUser.setEmail(userIn.getEmail());
        newUser.setPassword(passwordEncoder.encode(userIn.getPassword()));

        // Critical: User is DISABLED until OTP verify
        newUser.setEnabled(false);

        // Default Role: USER
        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepo.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Error: Role 'USER' is not found."));
        roles.add(userRole);
        newUser.setRoles(roles);

        // Save to Postgres
        usersRepo.save(newUser);

        // Fire Async Event to RabbitMQ (Notification Service listens to this)
        // Ensure "otp_exchange" and "otp_routing_key" match your Notification Service Config
//        OtpMessageDto msg = new OtpMessageDto(newUser.getEmail());
        OtpMessageDto msg = OtpMessageDto.builder()
                .email(newUser.getEmail())
                .subject("Welcome to CMS - Verify Account")
                .messageBody("Your Registration Code is: ")
                .type(OtpType.REGISTRATION) // <--- CRITICAL: Set context
                .build();
        rabbitTemplate.convertAndSend("otp_exchange", "otp_routing_key", msg);

        return "Registration Successful! Check your email for OTP.";
    }

    // 2. VERIFICATION LOGIC
    public String verifyAccount(String email, String otp) {
        // Call Notification Service via Feign (Synchronous)
        boolean isValid = notificationClient.validateOtp(email, otp,OtpType.REGISTRATION);

        if (isValid) {
            // Find User and Enable account
            Users user = usersRepo.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found via email"));

            user.setEnabled(true);
            usersRepo.save(user);
            return "Account Verified Successfully! You can now login.";
        } else {
            throw new RuntimeException("Invalid or Expired OTP.");
        }
    }

    public String initiateForgotPassword(String email) {

        // Security check
        if (!usersRepo.existsByEmail(email)) {
            // Best practice: Don't tell hackers the email doesn't exist.
            // Just return success or generic error. For now, let's error.
            throw new RuntimeException("Error: Email address not found.");
        }

        // Fire Async Event to RabbitMQ
        OtpMessageDto msg = OtpMessageDto.builder()
                .email(email)
                .subject("Reset Your Password - CMS")
                .messageBody("Use this code to reset your password: ")
                .type(OtpType.FORGOT_PASSWORD) // <--- CRITICAL: Distinct Type
                .build();

        rabbitTemplate.convertAndSend("otp_exchange", "otp_routing_key", msg);

        return "OTP sent to your email for password reset.";
    }

    @Transactional
    public String completePasswordReset(String email, String otp, String newPassword) {

        // Check Validity with Notification Service
        // Ensure we check FORGOT_PASSWORD type (so registration OTPs don't work here)
        boolean isValid = notificationClient.validateOtp(email, otp, OtpType.FORGOT_PASSWORD);

        if (isValid) {
            Users user = usersRepo.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Update Password
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setEnabled(true); // Re-enable account if it was locked/disabled
            usersRepo.save(user);

            return "Password successfully reset! You can now login.";
        } else {
            throw new RuntimeException("Invalid OTP for Password Reset.");
        }
    }
}
