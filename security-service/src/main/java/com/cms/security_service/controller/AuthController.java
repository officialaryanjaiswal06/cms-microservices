package com.cms.security_service.controller;

import com.cms.security_service.model.Role;
import com.cms.security_service.model.Users;
import com.cms.security_service.repository.RoleRepo;
import com.cms.security_service.repository.UsersRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.HashSet;
import java.util.Set;

@RestController
@CrossOrigin(origins = "http://localhost:6969")
public class AuthController {

    @Autowired
    private UsersRepo usersRepo;

    @Autowired
    private RoleRepo roleRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<String> registeruser (@RequestBody Users users){
        if (usersRepo.existsByUsername(users.getUsername())){
            return ResponseEntity.badRequest().body("Error : username is already taken!");
        }

        Users users1 = new Users();
        users1.setUsername(users.getUsername());
        users1.setPassword(passwordEncoder.encode(users.getPassword()));
        users1.setEmail(users.getEmail());

        // Registration endpoint is public: always register as plain USER, regardless of requested roles
        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepo.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Role Not Found"));
        roles.add(userRole);

        users1.setRoles(roles);
        usersRepo.save(users1);
        return ResponseEntity.ok("User Registered Successfully");



    }




}