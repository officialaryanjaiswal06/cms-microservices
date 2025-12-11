package com.cms.security_service.security;

import com.cms.security_service.repository.UsersRepo;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("userSecurity")
public class UserSecurity {
    private final UsersRepo usersRepo;

    public UserSecurity(UsersRepo usersRepo) {
        this.usersRepo = usersRepo;
    }

    public boolean isSelf(Long userId, Authentication authentication) {
        return usersRepo.findById(userId)
                .map(u -> u.getUsername().equals(authentication.getName()))
                .orElse(false);
    }
}