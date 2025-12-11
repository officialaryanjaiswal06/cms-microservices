package com.cms.security_service.DTO;


import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Setter
@Getter
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private Set<String> roles;

}
