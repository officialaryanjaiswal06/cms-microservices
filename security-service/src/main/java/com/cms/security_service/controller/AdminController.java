package com.cms.security_service.controller;

import com.cms.security_service.DTO.CreateModuleDto;
import com.cms.security_service.DTO.CreateRoleDto;
import com.cms.security_service.DTO.PermissionDto;
import com.cms.security_service.model.Module;
import com.cms.security_service.model.Role;
import com.cms.security_service.model.RoleModulePermission;
import com.cms.security_service.model.Users;
import com.cms.security_service.repository.ModuleRepository;
import com.cms.security_service.repository.RoleModulePermissionRepository;
import com.cms.security_service.repository.RoleRepo;
import com.cms.security_service.repository.UsersRepo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.*;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final UsersRepo usersRepo;
    private final RoleRepo roleRepo;
    private final ModuleRepository moduleRepo;
    private final RoleModulePermissionRepository roleModulePermissionRepository;

    public AdminController(UsersRepo usersRepo, RoleRepo roleRepo, ModuleRepository moduleRepo, RoleModulePermissionRepository roleModulePermissionRepository) {
        this.usersRepo = usersRepo;
        this.roleRepo = roleRepo;
        this.moduleRepo = moduleRepo;
        this.roleModulePermissionRepository = roleModulePermissionRepository;
    }

    // 1. VIEW EFFECTIVE PERMISSIONS
    @PreAuthorize("hasAuthority('IAM:MANAGE_USER_PERMISSIONS')")
    @GetMapping("/users/{id}/effective-permissions")
    public ResponseEntity<List<String>> getUserEffectivePermissions(@PathVariable Long id) {
        Users user = usersRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Set<String> effectivePermissions = new HashSet<>();

        for (Role role : user.getRoles()) {
            for (RoleModulePermission perm : role.getPermissions()) {
                String modulePrefix = perm.getModule().getModuleName().toUpperCase().replace(" ", "_");

                if (perm.isCanSelect()) effectivePermissions.add(modulePrefix + "_READ");
                if (perm.isCanCreate()) effectivePermissions.add(modulePrefix + "_CREATE");
                if (perm.isCanUpdate()) effectivePermissions.add(modulePrefix + "_UPDATE");
                if (perm.isCanDelete()) effectivePermissions.add(modulePrefix + "_DELETE");
            }
        }
        return ResponseEntity.ok(effectivePermissions.stream().sorted().toList());
    }

    // 2. ASSIGN ROLES TO USER
    @PreAuthorize("hasAuthority('IAM:MANAGE_USER_PERMISSIONS')")
    @PutMapping("/users/{id}/roles")
    public ResponseEntity<Void> updateUserRoles(@PathVariable Long id, @RequestBody List<String> roleNames) {
        Users user = usersRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Set<Role> newRoles = new HashSet<>();
        for (String name : roleNames) {
            // ✅ Fix: Changed findByRoleName -> findByName (as per your previous request)
            Role r = roleRepo.findByName(name)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role not found: " + name));
            newRoles.add(r);
        }

        user.setRoles(newRoles);
        usersRepo.save(user);

        return ResponseEntity.noContent().build();
    }

    // 3. GET ROLE PERMISSIONS
    @PreAuthorize("hasAuthority('IAM:MANAGE_USER_PERMISSIONS')")
    @GetMapping("/roles/{roleId}/permissions")
    public ResponseEntity<List<PermissionDto>> getRolePermissions(@PathVariable Long roleId) {
        Role role = roleRepo.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<PermissionDto> dtos = role.getPermissions().stream().map(p -> {
            PermissionDto dto = new PermissionDto();
            dto.setModuleName(p.getModule().getModuleName());
            dto.setCanSelect(p.isCanSelect());
            dto.setCanCreate(p.isCanCreate());
            dto.setCanUpdate(p.isCanUpdate());
            dto.setCanDelete(p.isCanDelete());
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // 4. CONFIGURE A ROLE
//    @PreAuthorize("hasAuthority('IAM:MANAGE_USER_PERMISSIONS')")
//    @PutMapping("/roles/{roleId}/permissions")
//    @Transactional
//    public ResponseEntity<Void> updateRolePermissions(@PathVariable Long roleId,
//                                                      @RequestBody List<PermissionDto> permissionDtos) {
//        Role role = roleRepo.findById(roleId)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
//
//        role.getPermissions().clear();
//
//        for (PermissionDto dto : permissionDtos) {
//
//            // ✅ THIS LINE NOW WORKS because we imported the custom 'Module' class at the top
//            Module module = moduleRepo.findByModuleName(dto.getModuleName())
//                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Module not found: " + dto.getModuleName()));
//
//            RoleModulePermission permission = new RoleModulePermission();
//            permission.setRole(role);
//            permission.setModule(module);
//            permission.setCanSelect(dto.isCanSelect());
//            permission.setCanCreate(dto.isCanCreate());
//            permission.setCanUpdate(dto.isCanUpdate());
//            permission.setCanDelete(dto.isCanDelete());
//
//            role.getPermissions().add(permission);
//        }
//
//        roleRepo.save(role);
//        return ResponseEntity.noContent().build();
//    }
    @PreAuthorize("hasAuthority('IAM:MANAGE_USER_PERMISSIONS')")
    @PutMapping("/roles/{roleId}/permissions")
    @Transactional
    public ResponseEntity<Void> updateRolePermissions(@PathVariable Long roleId,
                                                      @RequestBody List<PermissionDto> permissionDtos) {

        // 1. Fetch Role (and load existing permissions via @Transactional)
        Role role = roleRepo.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));

        // 2. To avoid duplicates in the REQUEST itself
        Set<String> processedModules = new HashSet<>();

        for (PermissionDto dto : permissionDtos) {
            String targetModuleName = dto.getModuleName();

            // Guard: If frontend sent the same module twice, skip the second one
            if (processedModules.contains(targetModuleName)) {
                continue;
            }

            // 3. Find the Module Entity
            Module module = moduleRepo.findByModuleName(targetModuleName)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Module not found: " + targetModuleName));

            // 4. Check if Role ALREADY has permission for this module
            // We use Stream API on the EXISTING collection.
            // Warning: Do NOT use module IDs for comparison unless you are sure both are un-proxied. Name comparison is safer.
            Optional<RoleModulePermission> existingPerm = role.getPermissions().stream()
                    .filter(p -> p.getModule().getModuleName().equals(targetModuleName))
                    .findFirst();

            if (existingPerm.isPresent()) {
                // ✅ UPDATE (Existing Row)
                RoleModulePermission perm = existingPerm.get();
                perm.setCanSelect(dto.isCanSelect());
                perm.setCanCreate(dto.isCanCreate());
                perm.setCanUpdate(dto.isCanUpdate());
                perm.setCanDelete(dto.isCanDelete());
                // No "save" needed explicitly on 'perm', JPA Dirty Checking handles it because of @Transactional
            } else {
                // ✅ CREATE (New Row)
                RoleModulePermission perm = new RoleModulePermission();
                perm.setRole(role);
                perm.setModule(module);
                perm.setCanSelect(dto.isCanSelect());
                perm.setCanCreate(dto.isCanCreate());
                perm.setCanUpdate(dto.isCanUpdate());
                perm.setCanDelete(dto.isCanDelete());

                // Add to the PARENT's collection.
                // Hibernate sees a new object added to the relationship and schedules an INSERT.
                role.getPermissions().add(perm);
            }

            // Mark this module as processed
            processedModules.add(targetModuleName);
        }

        // 5. Save parent (Cascades updates to children)
        roleRepo.save(role);
        return ResponseEntity.noContent().build();
    }

    // 5. HELPER: GET AVAILABLE MODULES
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/modules")
    public ResponseEntity<List<Module>> getAllModules() {
        return ResponseEntity.ok(moduleRepo.findAll());
    }

        @PreAuthorize("hasAuthority('IAM:MANAGE_USER_PERMISSIONS')")
        @GetMapping("/roles")
        public ResponseEntity<List<Map<String, Object>>> getAllRoles() {
            List<Map<String, Object>> roles = roleRepo.findAll().stream()
                    .map(role -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", role.getId());
                        map.put("name", role.getName());
                        return map;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(roles);
        }


    @PreAuthorize("hasAuthority('IAM:MANAGE_USER_PERMISSIONS')")
    @PostMapping("/modules")
    public ResponseEntity<Module> createModule(@RequestBody CreateModuleDto request) {

        // 1. Sanitize input (Prevent empty names & Remove leading/trailing spaces)
        if (request.getModuleName() == null || request.getModuleName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Module name cannot be empty");
        }

        String standardizedName = request.getModuleName().trim();

        // 2. Strict Check: Prevent "Gallery", "GALLERY", or "gallery" if one exists
        if (moduleRepo.existsByModuleNameIgnoreCase(standardizedName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Module already exists (Duplicate)");
        }

        Module module = new Module();
        module.setModuleName(standardizedName);

        return ResponseEntity.ok(moduleRepo.save(module));
    }

    @PreAuthorize("hasAuthority('IAM:MANAGE_USER_PERMISSIONS')")
    @DeleteMapping("/modules/{id}")
    public ResponseEntity<Void> deleteModule(@PathVariable Long id) {
        if (!moduleRepo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Module not found");
        }
        moduleRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }


    // ==========================================
    // ROLE MANAGEMENT (Dynamic Role Creation)
    // ==========================================

    @PreAuthorize("hasAuthority('IAM:MANAGE_USER_PERMISSIONS')")
    @PostMapping("/roles")
    public ResponseEntity<Role> createRole(@RequestBody CreateRoleDto request) {
        String roleName = request.getName().toUpperCase(); // Convention: Roles usually Uppercase

        if (roleRepo.findByName(roleName).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role already exists");
        }

        Role role = new Role();
        role.setName(roleName);
        // role.setDescription(request.getDescription()); // if you added description field to entity

        return ResponseEntity.ok(roleRepo.save(role));
    }



    @PreAuthorize("hasAuthority('IAM:MANAGE_USER_PERMISSIONS')")
//    @DeleteMapping("/roles/{id}")
//    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
//        Role role = roleRepo.findById(id)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
//
//        // Prevent deleting Critical Roles
//        if (role.getName().equalsIgnoreCase("SUPERADMIN") || role.getName().equalsIgnoreCase("USER")) {
//            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete system default roles");
//        }
//
//        roleRepo.delete(role);
//        return ResponseEntity.noContent().build();
//    }
    @DeleteMapping("/roles/{id}")
    @Transactional // <--- IMPORTANT: Required for @Modifying actions
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        Role role = roleRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));

        // 1. Protection: Don't delete SuperAdmin/User
        if (role.getName().equalsIgnoreCase("SUPERADMIN") || role.getName().equalsIgnoreCase("USER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete system default roles");
        }

        // 2. Fix: Remove this role from any users currently holding it
        // This clears the "foreign key constraint" error from 'user_role_tb'
        roleRepo.detachRoleFromUsers(id);

        // 3. Now delete the Role itself
        roleRepo.delete(role);

        return ResponseEntity.noContent().build();
    }

}