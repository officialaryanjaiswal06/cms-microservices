package com.cms.notification_service.model;

public enum NotificationCategory {
    OTP,            // Verification codes (Hidden from UI)
    SYSTEM_ALERT,   // Admin Broadcasts (Visible)
    ACCOUNT,        // General Account info (Visible)
    PROMOTION       // Marketing (Visible)
}
