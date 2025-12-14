package com.cms.notification_service.model;
import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "otp_tb")
public class Otp {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String email;
    private String otpCode;
    private LocalDateTime expiryTime;
}
