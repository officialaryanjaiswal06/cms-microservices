package com.cms.notification_service.repository;

import com.cms.notification_service.model.EmailRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailRepository extends JpaRepository<EmailRequest, Long> {
    // Find emails that are NOT SENT and NOT failed too many times
    List<EmailRequest> findByStatusNotAndRetryTimesLessThan(String status, int retryTimes);

}
