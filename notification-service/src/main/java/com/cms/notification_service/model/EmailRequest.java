package com.cms.notification_service.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "email_tb")
public class EmailRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String messageBody;

    private String status;  // "PENDING", "SENT", "FAILED"
    private int retryTimes; // e.g. 0, 1, 2...

    private LocalDateTime createdAt;

    @PrePersist
    public void init() {
        this.createdAt = LocalDateTime.now();
        if(this.status == null) this.status = "PENDING";
    }
}
