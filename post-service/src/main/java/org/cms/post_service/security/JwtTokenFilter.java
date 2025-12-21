package org.cms.post_service.security;

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

import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtTokenFilter extends OncePerRequestFilter {
    @Value("${spring.app.JwtSecret}")
    private String jwtSecret;

//    @Override
//    protected void doFilterInternal(HttpServletRequest request,
//                                    HttpServletResponse response,
//                                    FilterChain filterChain) throws ServletException, IOException {
//
//        try {
//            String jwt = parseJwt(request);
//
//            if (jwt != null && validateJwtToken(jwt)) {
//
//                // 1. Decode JWT
//                Claims claims = Jwts.parser()
//                        .setSigningKey(key())
//                        .build()
//                        .parseClaimsJws(jwt)
//                        .getBody();
//
//                String username = claims.getSubject();
//
//                // 2. Prepare Authorities List
//                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
//
//                // --- LOGIC FOR PERMISSIONS ---
//                // Maps direct permission strings (e.g. "NOTIFICATION_CREATE")
//                List<String> permissions = claims.get("permissions", List.class);
//                if (permissions != null) {
//                    for (String perm : permissions) {
//                        authorities.add(new SimpleGrantedAuthority(perm));
//                    }
//                }
//
//                // --- LOGIC FOR ROLES (CRITICAL FIX) ---
//                // Maps Roles and adds 'ROLE_' prefix if missing (e.g. "ADMIN" -> "ROLE_ADMIN")
//                // This makes @PreAuthorize("hasRole('ADMIN')") work properly.
//                List<String> roles = claims.get("roles", List.class);
//                if (roles != null) {
//                    for (String role : roles) {
//                        if (!role.startsWith("ROLE_")) {
//                            role = "ROLE_" + role;
//                        }
//                        authorities.add(new SimpleGrantedAuthority(role));
//                    }
//                }
//
//                // 3. Set User into Spring Security Context
//                UsernamePasswordAuthenticationToken authentication =
//                        new UsernamePasswordAuthenticationToken(username, null, authorities);
//
//                SecurityContextHolder.getContext().setAuthentication(authentication);
//            }
//        } catch (Exception e) {
//            logger.error("Cannot set user authentication: {}", e);
//        }
//
//        filterChain.doFilter(request, response);
//    }
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

    try {
        String jwt = parseJwt(request);

        if (jwt != null) {
            // Debugging Log 1
            System.out.println("DEBUG: Found Token: " + jwt.substring(0, 10) + "...");

            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));

            // 1. Verify
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(jwt) // If signature invalid, code CRASHES here
                    .getPayload();

            String username = claims.getSubject();
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();

            // 2. Roles Processing
            List<String> roles = claims.get("roles", List.class);
            if (roles != null) {
                for (String role : roles) {
                    // Logic: Add "ROLE_" prefix if missing
                    String auth = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                    authorities.add(new SimpleGrantedAuthority(auth));
                }
            }

            // 3. Permissions Processing
            List<String> permissions = claims.get("permissions", List.class);
            if (permissions != null) {
                for (String perm : permissions) {
                    authorities.add(new SimpleGrantedAuthority(perm));
                }
            }

            // Debugging Log 2
            System.out.println("DEBUG: Username: " + username);
            System.out.println("DEBUG: Final Authorities loaded in Spring: " + authorities);

            // 4. Authenticate
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            System.out.println("DEBUG: No JWT found in header.");
        }
    } catch (Exception e) {
        // Debugging Log 3 (THE MOST IMPORTANT ONE)
        System.out.println("DEBUG: Critical Authentication Error: " + e.getMessage());
        // e.printStackTrace(); // Uncomment if you want full stack trace
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
