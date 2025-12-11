package com.cms.security_service.utils;//package com.cms.security_service.utils;

import com.cms.security_service.model.Role;
import com.cms.security_service.model.RoleModulePermission;
import com.cms.security_service.model.Users;
import com.cms.security_service.repository.UsersRepo;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class JwtUtils {

    @Value("${spring.app.JwtSecret}")
    private String jwtSecret;

    @Value("${spring.app.JwtExpirationMs}")
    private int jwtExpirationMs;

    private final UsersRepo usersRepo;

    public JwtUtils(UsersRepo usersRepo) {
        this.usersRepo = usersRepo;
    }

    public String getJwtFromHeader(HttpServletRequest request){
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
    @Transactional
    public String generateTokenFromUsername(UserDetails userDetails){

        Users user = usersRepo.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

        // roles claim: only real role names from the DB (e.g. ROLE_ADMIN, ROLE_SUPER_ADMIN)
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        // Extract ALL granular permissions
        Set<String> permissions = new HashSet<>();
        for (Role role : user.getRoles()) {
            // Special IAM permission for admin panel
            if ("SUPERADMIN".equalsIgnoreCase(role.getName()) || "ADMIN".equalsIgnoreCase(role.getName())) {
                permissions.add("IAM:MANAGE_USER_PERMISSIONS");
            }

            for (RoleModulePermission perm : role.getPermissions()) {
                String module = perm.getModule().getModuleName().toUpperCase().replace(" ", "_");
                if (perm.isCanSelect()) permissions.add(module + "_READ");
                if (perm.isCanCreate()) permissions.add(module + "_CREATE");
                if (perm.isCanUpdate()) permissions.add(module + "_UPDATE");
                if (perm.isCanDelete()) permissions.add(module + "_DELETE");
            }
        }

        return Jwts.builder()
                .subject( userDetails.getUsername())
                .claim("roles", roles)
                .claim("permissions", new ArrayList<>(permissions))
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(new Date().getTime() + jwtExpirationMs))
                .signWith(key())
                .compact();
    }

    public String getUsernameFromJwtToken(String token){
        return Jwts.parser()
                .verifyWith((SecretKey) key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public List<String> getRolesFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("roles", List.class);
    }

    public List<String> getPermsFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("permissions", List.class);
    }

    private SecretKey key(){
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public boolean validateJwtToken(String authToken) {
        try {
            System.out.println("Validate");
            Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (Exception e) {
            System.out.println("JWT Validation Error: " + e.getMessage());
            return false;
        }

    }

}


