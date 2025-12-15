package com.cms.notification_service.repository;

import com.cms.notification_service.model.Otp;
import com.cms.notification_service.model.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {
//    Optional<Otp> findByEmail(String email);
//    void deleteByEmail(String email);

    Optional<Otp> findByEmailAndType(String email, OtpType type);

//    void deleteByEmailAndType(String email, OtpType type);

    @Modifying
    @Query("DELETE FROM Otp o WHERE o.email = :email AND o.type = :type")
    void deleteByEmailAndType(@Param("email") String email, @Param("type") OtpType type);

    @Query(value = "SELECT * FROM otp_tb WHERE email = :email ORDER BY id DESC LIMIT 1", nativeQuery = true)
    Optional<Otp> findLatestByEmail(@Param("email") String email);
}
