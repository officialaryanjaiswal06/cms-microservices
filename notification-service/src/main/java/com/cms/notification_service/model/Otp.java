package com.cms.notification_service.model;
import lombok.AllArgsConstructor;
import lombok.Data;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "otp_tb")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Otp extends CommonTable{
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String email;
    private String otpCode;
    private LocalDateTime expiryTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpType type;

    @Override
    protected String provideEntryName() {
        return this.email;
    }

    @Override
    protected String provideEntryType() {
        return "OTP_RECORD_" + this.type;
    }

    @Override
    protected String provideModuleName() {
        return "NOTIFICATION_SERVICE";
    }
}
