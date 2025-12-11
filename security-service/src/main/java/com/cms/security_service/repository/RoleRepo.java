package com.cms.security_service.repository;
import com.cms.security_service.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RoleRepo extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);

    @Modifying
    @Query(value = "DELETE FROM user_role_tb WHERE role_id = :roleId", nativeQuery = true)
    void detachRoleFromUsers(@Param("roleId") Long roleId);
}