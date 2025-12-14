package com.cms.security_service.controller;

import com.cms.security_service.DTO.CreateUserRequest;
import com.cms.security_service.DTO.UpdateRolesRequest;
import com.cms.security_service.DTO.UpdateUserRequest;
import com.cms.security_service.DTO.UserResponse;
import com.cms.security_service.model.Users;
import com.cms.security_service.repository.RoleRepo;
import com.cms.security_service.repository.UsersRepo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.cms.security_service.model.Role;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UsersRepo usersRepo;
    private final RoleRepo roleRepo;
    private final PasswordEncoder passwordEncoder;

    // Safety constant for your 'God Mode' role
    private static final String SUPER_ADMIN_ROLE = "SuperAdmin";

    public UserController(UsersRepo usersRepo, RoleRepo roleRepo, PasswordEncoder passwordEncoder) {
        this.usersRepo = usersRepo;
        this.roleRepo = roleRepo;
        this.passwordEncoder = passwordEncoder;
    }

    // --------- Helpers ---------
// Security Helper: Robust Rank Calculation
    private int calculateRank(String roleName) {
        if (roleName == null) return 0;
        String normalized = roleName.trim().toUpperCase();

        if (normalized.equals("SUPERADMIN")) return 3; // Highest
        if (normalized.equals("ADMIN")) return 2;      // Middle
        return 1;                                      // Lowest (User, Editor, etc.)
    }

    private int getUserMaxRank(Users user) {
        return user.getRoles().stream()
                .map(r -> calculateRank(r.getName()))
                .max(Integer::compareTo)
                .orElse(0);
    }
    private UserResponse toResponse(Users u) {
        UserResponse res = new UserResponse();
        res.setId(u.getId());
        res.setUsername(u.getUsername());
        res.setEmail(u.getEmail());
        // Map dynamic Role Entity names
        res.setRoles(u.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet()));
        return res;
    }

    /**
     * Safety Guard: Prevents non-SuperAdmins from modifying SuperAdmins.
     * Even if you have 'USER_UPDATE' permission, you shouldn't be able to disable the owner.
     */
    private void checkSafeguards(Users targetUser, Authentication auth) {
        boolean targetIsSuper = targetUser.getRoles().stream()
                .anyMatch(r -> r.getName().equalsIgnoreCase(SUPER_ADMIN_ROLE));

        boolean callerIsSuper = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + SUPER_ADMIN_ROLE.toUpperCase()));

        if (targetIsSuper && !callerIsSuper) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only SuperAdmin can modify/delete other SuperAdmin accounts.");
        }
    }

    // --------- Endpoints ---------

    // 1. Get Self Profile (Available to any authenticated user)
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication auth) {
        Users u = usersRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok(toResponse(u));
    }

    // 2. List All Users (Requires SELECT permission on 'USER' module)
    @GetMapping
//    @PreAuthorize("hasAuthority('USER_SELECT')")
    @PreAuthorize("hasAuthority('IAM:MANAGE_USER_PERMISSIONS')")
    public ResponseEntity<List<UserResponse>> list() {
        List<UserResponse> res = usersRepo.findAll().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(res);
    }

    // 3. Get User by ID (Requires SELECT permission on 'USER' module)
    @GetMapping("/{id}")
//    @PreAuthorize("hasAuthority('USER_SELECT')")
    @PreAuthorize("hasAuthority('IAM:MANAGE_USER_PERMISSIONS')")
    public ResponseEntity<UserResponse> get(@PathVariable Long id) {
        Users u = usersRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok(toResponse(u));
    }

    // 4. Create User (Requires CREATE permission on 'USER' module)
@PostMapping
@PreAuthorize("hasAuthority('IAM:MANAGE_USER_PERMISSIONS')")
@Transactional
public ResponseEntity<UserResponse> create(@RequestBody CreateUserRequest req, Authentication auth) {

    // 1. Who is calling this endpoint? (The Requester)
    Users requester = usersRepo.findByUsername(auth.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    int requesterRank = getUserMaxRank(requester);

    if (usersRepo.existsByUsername(req.getUsername())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is already taken");
    }

    Users u = new Users();
    u.setUsername(req.getUsername());
    u.setEmail(req.getEmail());
    u.setPassword(passwordEncoder.encode(req.getPassword()));

    // 2. Logic: Process Roles
    if (req.getRoles() != null && !req.getRoles().isEmpty()) {
        Set<Role> rolesToAssign = new HashSet<>();

        for (String rName : req.getRoles()) {
            Role r = roleRepo.findByName(rName)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role not found: " + rName));

            // 🛑 HIERARCHY PROTECTION 🛑
            // Check the rank of the specific role being added.
            int roleTargetRank = calculateRank(r.getName());

            // Rule: You cannot assign a role EQUAL to or HIGHER than your own.
            // Example: Admin (2) cannot create another Admin (2) or SuperAdmin (3).
            if (roleTargetRank >= requesterRank) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Insufficient privileges: You cannot assign role '" + rName + "'. Rank too high.");
            }

            rolesToAssign.add(r);
        }
        u.setRoles(rolesToAssign);
    } else {
        // Default Role Fallback (Usually "USER")
        Role userRole = roleRepo.findByName("USER")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Default role missing"));

        // Even default roles must be checked (safety first)
        if (calculateRank(userRole.getName()) >= requesterRank) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient privileges for default role.");
        }
        u.setRoles(Set.of(userRole));
    }

    Users saved = usersRepo.save(u);
    return ResponseEntity.created(URI.create("/users/" + saved.getId())).body(toResponse(saved));
}

    // 5. Update User Roles (Requires UPDATE permission on 'USER' module)
@PutMapping("/{id}/roles")
@PreAuthorize("hasAuthority('IAM:MANAGE_USER_PERMISSIONS')")
@Transactional
public ResponseEntity<UserResponse> updateRoles(@PathVariable Long id,
                                                @RequestBody UpdateRolesRequest req,
                                                Authentication auth) {

    // 1. Who is calling?
    Users requester = usersRepo.findByUsername(auth.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    int requesterRank = getUserMaxRank(requester);

    // 2. Who is being modified?
    Users targetUser = usersRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    // Safeguard: Do not allow modifying a user who already outranks you
    int currentTargetRank = getUserMaxRank(targetUser);
    if (currentTargetRank >= requesterRank) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "You cannot modify the roles of a user with equal or higher rank.");
    }

    Set<Role> newRoles = new HashSet<>();

    // 3. Process new role list
    for (String rName : req.getRoles()) {
        Role r = roleRepo.findByName(rName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role not found: " + rName));

        // 🛑 HIERARCHY PROTECTION 🛑
        // Example: Admin (2) tries to give role SuperAdmin (3) -> Blocked.
        int roleTargetRank = calculateRank(r.getName());

        if (roleTargetRank >= requesterRank) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Insufficient privileges: You cannot assign role '" + rName + "'. Rank too high.");
        }

        newRoles.add(r);
    }

    targetUser.setRoles(newRoles);
    Users saved = usersRepo.save(targetUser);
    return ResponseEntity.ok(toResponse(saved));
}




    private int getRoleLevel(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) return 0;

        // Use standard names from your DataInitializer
        if (roles.stream().anyMatch(r -> r.getName().equalsIgnoreCase("SUPERADMIN"))) {
            return 3;
        }
        if (roles.stream().anyMatch(r -> r.getName().equalsIgnoreCase("ADMIN"))) {
            return 2;
        }
        // All others (EDITOR, USER) are level 1
        return 1;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('IAM:MANAGE_USER_PERMISSIONS')") // Checks your SecurityConfig permissions
    @Transactional
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, Authentication auth) {

        // 1. Fetch the user trying to perform the delete (Requester)
        Users requester = usersRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        // 2. Fetch the user who is about to be deleted (Target)
        Users targetUser = usersRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // 3. Safety Guard: Prevent Self-Deletion (Admin can't delete themselves here)
        if (requester.getId().equals(targetUser.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot delete your own account.");
        }

        // 4. Calculate Ranks
        int requesterRank = getRoleLevel(requester.getRoles());
        int targetRank = getRoleLevel(targetUser.getRoles());


        if (requesterRank <= targetRank) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You do not have permission to delete a user with equal or higher rank.");
        }

        // 6. Delete
        usersRepo.delete(targetUser);
        return ResponseEntity.noContent().build();
    }

    // this is to get all the email from the role
//    @GetMapping("/emails")
//    public List<String> getAllEmailsByRole(@RequestParam String roleName) {
//        // Assuming your Users entity has a relationship with Roles
//        // Logic: Find all users where roles.name = roleName
//        return usersRepo.findAll().stream()
//                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName().equals(roleName)))
//                .map(Users::getEmail)
//                .toList();
//    }

    @GetMapping("/emails")
    public List<String> getEmailsByRole(
            @RequestParam String roleName,
            @RequestParam(defaultValue = "false") boolean strict) {

        if (strict) {
            // New Logic: Get users with ONLY this role
            return usersRepo.findEmailsByStrictRole(roleName);
        } else {
            // Old Logic: Get anyone who has this role (plus maybe others)
            return usersRepo.findAll().stream()
                    .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName().equals(roleName)))
                    .map(u -> u.getEmail())
                    .toList();
        }
    }
}