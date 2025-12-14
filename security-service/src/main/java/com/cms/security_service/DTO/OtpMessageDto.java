package com.cms.security_service.DTO;


import com.cms.security_service.model.OtpType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OtpMessageDto {
    private String email;
    private String subject;
    private String messageBody;
    private OtpType type;
}
