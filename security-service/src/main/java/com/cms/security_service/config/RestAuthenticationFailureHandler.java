//package com.cms.security_service.config;
//
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.security.core.AuthenticationException;
//import org.springframework.security.web.authentication.AuthenticationFailureHandler;
//import org.springframework.stereotype.Component;
//import tools.jackson.databind.ObjectMapper;
//
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.Map;
//
//@Component
//public class RestAuthenticationFailureHandler implements AuthenticationFailureHandler {
//
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//
//
//    @Override
//    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
//
//        Map<String,String> errorMap = new HashMap<>();
//
//        errorMap.put("Message","Authentication failed");
//        errorMap.put("error",exception.getLocalizedMessage());
//        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//        response.setContentType("application/json");
//        response.getWriter().write(objectMapper.writeValueAsString(errorMap));
//        response.getWriter().flush();
//
//
//    }
//}
package com.cms.security_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
//import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class RestAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("message", "Invalid username or password"); // You can use exception.getMessage() for more detail

        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), body);
    }
}