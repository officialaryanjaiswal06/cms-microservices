package com.cms.notification_service.dto;

import com.cms.notification_service.model.OtpType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpMessageDto {
    private String email;
    private String subject;
    private String messageBody;
    private OtpType type;
}
