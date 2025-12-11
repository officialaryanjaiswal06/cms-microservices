package com.cms.security_service.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.cms.security_service.model.Module;
import java.util.Optional;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Long> {
    Optional<Module> findByModuleName(String moduleName);
    boolean existsByModuleNameIgnoreCase(String moduleName);

}