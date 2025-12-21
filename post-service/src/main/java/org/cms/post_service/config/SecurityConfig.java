package org.cms.post_service.config;


import org.cms.post_service.security.JwtTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtTokenFilter jwtTokenFilter;
    public SecurityConfig(JwtTokenFilter jwtTokenFilter) { this.jwtTokenFilter = jwtTokenFilter; }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        // Example: Public Read
                                .requestMatchers("/internal/**").permitAll()
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/content/images/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/content/public/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/content/post/entry/**").permitAll()

                                // Allow public list (if you haven't already)
                                .requestMatchers(HttpMethod.GET, "/content/public/posts/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/content/schema/**").authenticated()
//                        .requestMatchers(HttpMethod.GET, "/content/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}