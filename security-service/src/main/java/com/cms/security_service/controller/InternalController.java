package com.cms.security_service.controller;

import com.cms.security_service.repository.ModuleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.cms.security_service.model.Module;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/modules")
@RequiredArgsConstructor
public class InternalController {
    private final ModuleRepository moduleRepo;

    // Checks if module "ACADEMIC" exists
    @GetMapping("/exists/{moduleName}")
    public ResponseEntity<Boolean> checkModuleExists(@PathVariable String moduleName) {
        String key = moduleName.trim().toUpperCase().replace(" ", "_");
        boolean exists = moduleRepo.existsByModuleNameIgnoreCase(key);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/list")
    public ResponseEntity<List<String>> getAllModuleNames() {
        // Returns ["ACADEMIC", "ABOUT_US", "BLOG"]
        List<String> modules = moduleRepo.findAll()
                .stream()
                .map(Module::getModuleName)
                .toList();
        return ResponseEntity.ok(modules);
    }

    @PostMapping("/generate")
    @Transactional
    public ResponseEntity<String> generateModule(@RequestBody Map<String, String> request) {
        // 1. Extract schemaName from the DTO sent by Post Service
        String schemaName = request.get("schemaName");

        if (schemaName == null || schemaName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Schema Name is missing");
        }

        // 2. Format it to Standard Security Module format (ACADEMIC)
        String moduleKey = schemaName.trim().toUpperCase().replace(" ", "_");

        // 3. Create if it doesn't exist
        if (!moduleRepo.existsByModuleNameIgnoreCase(moduleKey)) {
            Module newModule = new Module();
            newModule.setModuleName(moduleKey);
            moduleRepo.save(newModule);
            System.out.println("✅ Sync: Created new Security Module: " + moduleKey);
        } else {
            System.out.println("ℹ️ Sync: Module already exists: " + moduleKey);
        }

        return ResponseEntity.ok("SYNCED");
    }
}
