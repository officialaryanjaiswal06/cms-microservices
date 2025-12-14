package com.cms.notification_service.consumer;

import com.cms.notification_service.dto.OtpMessageDto;
import com.cms.notification_service.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RabbitConsumer {
    private final OtpService otpService;

    // Listener logic
    @RabbitListener(queues = "otp_queue")
    public void receiveMessage(OtpMessageDto msg) {

        // Trigger the service logic
        otpService.generateAndQueueOtp(msg.getEmail(),
                msg.getSubject(),
                msg.getMessageBody(),
                msg.getType()
        );
    }

}
