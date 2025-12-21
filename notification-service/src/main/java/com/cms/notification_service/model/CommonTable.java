package com.cms.notification_service.model;

import jakarta.persistence.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * Common Audit Entity.
 * Contains shared metadata columns that every table must have.
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder // Needed for inheritance
@EntityListeners(AuditingEntityListener.class)
public abstract class CommonTable {

    // Note: We do NOT define @Id here. Subclasses define their own IDs.

    @Column(name = "entry_name")
    private String entryName;

    @Column(name = "entry_type")
    private String entryType;

    @Column(name = "module_name")
    private String moduleName;

    @Column(name = "last_action")
    private String lastAction;

    @CreatedBy
    @Column(name = "created_by_username" , updatable = false)
    private String createdByUsername;

    @CreatedDate
    @Column(name = "entry_date_time", updatable = false)
    private LocalDateTime entryDateTime;

    @Column(name = "from_url")
    private String fromUrl;

    /**
     * Abstract methods enforce developers to classify data
     */
    protected abstract String provideEntryName();
    protected abstract String provideEntryType();
    protected abstract String provideModuleName();

    @PrePersist
    public void onPrePersist() {

        this.lastAction = "CREATE";


        this.entryName = provideEntryName();
        this.entryType = provideEntryType();
        this.moduleName = provideModuleName();

        if (this.fromUrl == null) {
            this.fromUrl = resolveSourceUrl();
        }
    }

    @PreUpdate
    public void onPreUpdate() {
        this.lastAction = "UPDATE";


        // Refresh auditing info on update
        this.entryName = provideEntryName();
        // entryType and moduleName usually static per class, but ensuring they are set:
        if(this.entryType == null) this.entryType = provideEntryType();
        if(this.moduleName == null) this.moduleName = provideModuleName();
    }

    @PreRemove
    public void onPreRemove() {
        this.lastAction = "DELETE";
    }

    // Helper: Safely get username from Security Context or default to SYSTEM (for RabbitMQ)
//    private String resolveUsername() {
//        try {
//            if (SecurityContextHolder.getContext().getAuthentication() != null) {
//                return SecurityContextHolder.getContext().getAuthentication().getName();
//            }
//        } catch (Exception ignored) { }
//        return "SYSTEM"; // Likely a background scheduler or RabbitMQ event
//    }

    // Helper: Safely get Request URL or default
    private String resolveSourceUrl() {
        try {
            var attribs = RequestContextHolder.getRequestAttributes();
            if (attribs instanceof ServletRequestAttributes) {
                HttpServletRequest req = ((ServletRequestAttributes) attribs).getRequest();
                return req.getMethod() + " " + req.getRequestURI();
            }
        } catch (Exception ignored) { }
        return "ASYNC_MQ_EVENT"; // Fallback
    }
}