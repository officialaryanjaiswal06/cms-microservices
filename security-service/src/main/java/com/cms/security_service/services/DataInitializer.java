package com.cms.security_service.services;

import com.cms.security_service.model.Role;
import com.cms.security_service.repository.RoleRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initRoles(RoleRepo roleRepo) {
        return args -> {
            // Check if "USER" role exists; if not, save it.
            if (roleRepo.findByName("USER").isEmpty()) {
                Role userRole = new Role();
                userRole.setName("USER");
                roleRepo.save(userRole);
                System.out.println("✅ Role 'USER' inserted into DB");
            }

            if (roleRepo.findByName("SUPERADMIN").isEmpty()) {
                Role adminRole = new Role();
                adminRole.setName("SUPERADMIN");
                roleRepo.save(adminRole);
                System.out.println("✅ Role 'SUPERADMIN' inserted into DB");
            }
        };
    }
}