package com.cms.notification_service.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("auditorAware")
public class ApplicationAuditAware implements AuditorAware<String> {
    @Override
    public Optional<String> getCurrentAuditor() {
        // 1. Get the Authentication object from Spring Security Context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 2. Check if it is null (Async/RabbitMQ) or Anonymous (Public user)
        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            return Optional.of("SYSTEM"); // This will be saved to DB for Background Tasks
        }

        // 3. Return the Logged In Username (e.g. "superadmin")
        return Optional.ofNullable(authentication.getName());
    }
}
