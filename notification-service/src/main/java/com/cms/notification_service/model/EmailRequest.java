package com.cms.notification_service.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "email_tb")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder // Needed to extend CommonTable
@EqualsAndHashCode(callSuper = true)
public class EmailRequest extends CommonTable{
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String messageBody;

    private String status;  // "PENDING", "SENT", "FAILED"
    private int retryTimes; // e.g. 0, 1, 2...

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationCategory category;


//    @PrePersist
//    public void init() {
//        this.createdAt = LocalDateTime.now();
//        if(this.status == null) this.status = "PENDING";
//
//        if (this.category == null) this.category = NotificationCategory.ACCOUNT;
//    }

    @Override
    protected String provideEntryName() {
        return "To: " + this.email;
    }

    @Override
    protected String provideEntryType() {
        return "NOTIFICATION_" + (this.category != null ? this.category.name() : "GENERAL");
    }

    @Override
    protected String provideModuleName() {
        return "NOTIFICATION_SERVICE";
    }

    @Override
    public void onPrePersist() {
        // --- YOUR LOGIC (Formerly in init()) ---
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "PENDING";
        if (this.category == null) this.category = NotificationCategory.ACCOUNT;

        // --- PARENT LOGIC (Audit Columns) ---
        // We run this AFTER setting local defaults, so provideEntryType() sees the category!
        super.onPrePersist();
    }

}
