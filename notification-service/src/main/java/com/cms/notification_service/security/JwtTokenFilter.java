//package com.cms.notification_service.security;
//
//import io.jsonwebtoken.Claims;
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.io.Decoders;
//import io.jsonwebtoken.security.Keys;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import javax.crypto.SecretKey;
//import java.io.IOException;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Component
//public class JwtTokenFilter extends OncePerRequestFilter {
//    @Value("${spring.app.JwtSecret}")
//    private String jwtSecret;
//
//    @Value("${spring.app.JwtExpirationMs}")
//    private int jwtExpirationMs;
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
//        try {
//            // 1. Get Token from Header
//            String jwt = parseJwt(request);
//
//            if (jwt != null && validateJwtToken(jwt)) {
//                // 2. Decode Token
//                Claims claims = Jwts.parser()
//                        .setSigningKey(key())
//                        .build()
//                        .parseClaimsJws(jwt)
//                        .getBody();
//
//                String username = claims.getSubject();
//
//                // 3. EXTRACT PERMISSIONS (CRITICAL STEP)
//                // This maps the JSON list "permissions": [...] into Java Authorities
//                List<String> permissions = claims.get("permissions", List.class);
//
//                if (permissions != null) {
//                    List<GrantedAuthority> authorities = permissions.stream()
//                            .map(SimpleGrantedAuthority::new)
//                            .collect(Collectors.toList());
//
//                    // 4. Authenticate in Context
//                    UsernamePasswordAuthenticationToken authentication =
//                            new UsernamePasswordAuthenticationToken(username, null, authorities);
//
//                    SecurityContextHolder.getContext().setAuthentication(authentication);
//                }
//            }
//        } catch (Exception e) {
//            System.err.println("Cannot set user authentication: " + e.getMessage());
//        }
//
//        filterChain.doFilter(request, response);
//    }
//
//    private String parseJwt(HttpServletRequest request) {
//        String headerAuth = request.getHeader("Authorization");
//        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
//            return headerAuth.substring(7);
//        }
//        return null;
//    }
//    private SecretKey key(){
//        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
//    }
//
//    public boolean validateJwtToken(String authToken) {
//        try {
//            System.out.println("Validate");
//            Jwts.parser()
//                    .verifyWith(key())
//                    .build()
//                    .parseSignedClaims(authToken);
//            return true;
//        } catch (Exception e) {
//            System.out.println("JWT Validation Error: " + e.getMessage());
//            return false;
//        }
//
//    }
//}

package com.cms.notification_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtTokenFilter extends OncePerRequestFilter {

    @Value("${spring.app.JwtSecret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = parseJwt(request);

            if (jwt != null && validateJwtToken(jwt)) {

                // 1. Decode JWT
                Claims claims = Jwts.parser()
                        .setSigningKey(key())
                        .build()
                        .parseClaimsJws(jwt)
                        .getBody();

                String username = claims.getSubject();

                // 2. Prepare Authorities List
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();

                // --- LOGIC FOR PERMISSIONS ---
                // Maps direct permission strings (e.g. "NOTIFICATION_CREATE")
                List<String> permissions = claims.get("permissions", List.class);
                if (permissions != null) {
                    for (String perm : permissions) {
                        authorities.add(new SimpleGrantedAuthority(perm));
                    }
                }

                // --- LOGIC FOR ROLES (CRITICAL FIX) ---
                // Maps Roles and adds 'ROLE_' prefix if missing (e.g. "ADMIN" -> "ROLE_ADMIN")
                // This makes @PreAuthorize("hasRole('ADMIN')") work properly.
                List<String> roles = claims.get("roles", List.class);
                if (roles != null) {
                    for (String role : roles) {
                        if (!role.startsWith("ROLE_")) {
                            role = "ROLE_" + role;
                        }
                        authorities.add(new SimpleGrantedAuthority(role));
                    }
                }

                // 3. Set User into Spring Security Context
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e);
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }

    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    private boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().setSigningKey(key()).build().parse(authToken);
            return true;
        } catch (Exception e) {
            // Logs detailed error (Expired, Malformed, etc) if validation fails
            logger.error("Invalid JWT Token: {}", e);
        }
        return false;
    }
}
