package com.cms.security_service.services;

import com.cms.security_service.DTO.OtpMessageDto;
import com.cms.security_service.client.NotificationClient;
import com.cms.security_service.model.Role;
import com.cms.security_service.model.Users;
import com.cms.security_service.repository.RoleRepo;
import com.cms.security_service.repository.UsersRepo;
import jakarta.transaction.Transactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
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
        OtpMessageDto msg = new OtpMessageDto(newUser.getEmail());
        rabbitTemplate.convertAndSend("otp_exchange", "otp_routing_key", msg);

        return "Registration Successful! Check your email for OTP.";
    }

    // 2. VERIFICATION LOGIC
    public String verifyAccount(String email, String otp) {
        // Call Notification Service via Feign (Synchronous)
        boolean isValid = notificationClient.validateOtp(email, otp);

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
}
