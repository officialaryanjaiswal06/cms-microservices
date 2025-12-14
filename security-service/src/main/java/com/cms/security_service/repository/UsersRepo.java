package com.cms.security_service.repository;
import com.cms.security_service.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsersRepo extends JpaRepository<Users, Long> {

    Optional<Users> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<Users> findByEmail(String email);


    @Query("SELECT u.email FROM Users u JOIN u.roles r WHERE r.name = :roleName AND SIZE(u.roles) = 1")
    List<String> findEmailsByStrictRole(@Param("roleName") String roleName);
}