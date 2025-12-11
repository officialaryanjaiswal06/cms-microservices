package com.cms.security_service.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@NoArgsConstructor // ✅ Critical for JSON
@AllArgsConstructor
public class UpdateRolesRequest {
//    private Set<String> roles;
private Set<String> roles = new HashSet<>();
}