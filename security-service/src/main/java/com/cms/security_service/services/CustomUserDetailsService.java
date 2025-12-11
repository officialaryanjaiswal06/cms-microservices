package com.cms.security_service.services;

import com.cms.security_service.model.Role;
import com.cms.security_service.model.RoleModulePermission;
import com.cms.security_service.model.Users;
import com.cms.security_service.repository.UsersRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UsersRepo usersRepo;

    @Override
    @Transactional // Good practice to ensure Lazy loaded collections (permissions) work
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // 1. Fetch User by username
        Users user = usersRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // 2. Use a Set to avoid duplicate permissions (e.g., if two roles give access to the same module)
        Set<GrantedAuthority> authorities = new HashSet<>();

        // 3. Iterate through User's Roles
        for (Role role : user.getRoles()) {

            // Add the Role itself (e.g., "ROLE_SUPERADMIN")
            // Spring Security standards often expect the prefix "ROLE_"
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName().toUpperCase()));

            // 4. Iterate through the Role's Module Permissions
            for (RoleModulePermission perm : role.getPermissions()) {

                String moduleName = perm.getModule().getModuleName().toUpperCase().replace(" ", "_");


                if (role.getName().equalsIgnoreCase("SUPERADMIN") || role.getName().equalsIgnoreCase("ADMIN")) {
                    // This string specifically matches the @PreAuthorize annotation in AdminController
                    authorities.add(new SimpleGrantedAuthority("IAM:MANAGE_USER_PERMISSIONS"));
                }
                // 5. Check boolean flags and create granular authorities
                // Example Result: "ACADEMIC_READ", "ACADEMIC_CREATE"

                if (perm.isCanSelect()) {
                    authorities.add(new SimpleGrantedAuthority(moduleName + "_READ"));
                }
                if (perm.isCanCreate()) {
                    authorities.add(new SimpleGrantedAuthority(moduleName + "_CREATE"));
                }
                if (perm.isCanUpdate()) {
                    authorities.add(new SimpleGrantedAuthority(moduleName + "_UPDATE"));
                }
                if (perm.isCanDelete()) {
                    authorities.add(new SimpleGrantedAuthority(moduleName + "_DELETE"));
                }
            }
        }

        // 6. Return Spring User
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities // The Set is automatically accepted here (or convert to List)
        );
    }
}