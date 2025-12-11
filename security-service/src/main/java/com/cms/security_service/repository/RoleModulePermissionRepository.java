package com.cms.security_service.repository;

import com.cms.security_service.model.RoleModulePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleModulePermissionRepository extends JpaRepository<RoleModulePermission, Long> {
    Optional<RoleModulePermission> findByRoleIdAndModuleId(Long roleId, Long moduleId);

    List<RoleModulePermission> findByRoleId(Long roleId);
    void deleteByModuleId(Long moduleId);


    @Query("SELECT p FROM RoleModulePermission p " +
            "WHERE p.role.name = :roleName " +
            "AND p.module.moduleName = :moduleName")
    Optional<RoleModulePermission> findPermission(@Param("roleName") String roleName,
                                                  @Param("moduleName") String moduleName);
}