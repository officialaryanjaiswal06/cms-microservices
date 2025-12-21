
package org.cms.post_service.service; // Verify your package

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cms.post_service.client.SecurityClient;
import org.cms.post_service.dto.ModuleDto;
import org.cms.post_service.model.Post;
import org.cms.post_service.model.PostSchema;
import org.cms.post_service.repository.PostRepository;
import org.cms.post_service.repository.PostSchemaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final PostSchemaRepository schemaRepository;
    private final SecurityClient securityClient;
    private final ObjectMapper mapper;
    private final FileStorageService fileService;

    // ==========================================
    // 1. ADMIN: CREATE SCHEMA DEFINITION
    // ==========================================


    @Transactional
    public PostSchema createDefinition(PostSchema inputSchema) {

        // 1. Safety Check (Prevent 500 Server Error if JSON is missing keys)
        if (inputSchema.getSchemaName() == null || inputSchema.getSchemaType() == null) {
            throw new RuntimeException("Error: 'schemaName' and 'schemaType' are required.");
        }

        // 2. Standardize Inputs (Uppercase, No spaces)
        String modKey = inputSchema.getSchemaName().trim().toUpperCase().replace(" ", "_");
        String typeKey = inputSchema.getSchemaType().trim().toUpperCase().replace(" ", "_");

        log.info("Creating definition for Module: {}, Type: {}", modKey, typeKey);

        // A. Ensure Security Permissions Exist for this Module
        securityClient.generatePermissions(new ModuleDto(modKey));

        // B. Update Object fields (Fixing the Inconsistency Risk)

        // 1. Update the Child Field (Actual logic usage)
        inputSchema.setSchemaName(modKey);
        inputSchema.setSchemaType(typeKey);

        // 2. Update the Parent CommonTable Field (For Audit/Filtering)
        inputSchema.setModuleName(modKey);

        return schemaRepository.save(inputSchema);
    }

    // ==========================================
    // 2. EDITOR: CREATE CONTENT (POST)
    // ==========================================


    @Transactional
    public Post createDynamicPost(String moduleName, String schemaType, String jsonStringData, MultipartFile file, boolean isPublished) {

        String modKey = moduleName.toUpperCase().trim();
        String typeKey = schemaType.toUpperCase().trim();

        // Security Check
        checkPermission(modKey, "CREATE");

        // Find Blueprint
        PostSchema schema = schemaRepository.findBySchemaNameAndSchemaType(modKey, typeKey)
                .orElseThrow(() -> new RuntimeException("Schema definition not found"));

        try {
            Map<String, Object> data = mapper.readValue(jsonStringData, new TypeReference<>() {});

            if (file != null && !file.isEmpty()) {
                String path = fileService.saveFile(file);
                data.put("file_path", path);
            }

            Post post = new Post();
            post.setSchema(schema);
            post.setData(data);
            post.setAttachmentPath((String)data.get("file_path"));

            // ✅ Set Status
            post.setPublished(isPublished);

            return postRepository.save(post);

        } catch (IOException e) {
            throw new RuntimeException("Error processing content/file", e);
        }
    }

    // ==========================================
    // 3. READ OPERATIONS
    // ==========================================
// ==========================================
    // NEW: TOGGLE STATUS (Publish/Unpublish)
    // ==========================================
    @Transactional
    public Post togglePublishStatus(Long id, boolean status) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found ID: " + id));

        // Must be Editor/Admin to change status
        String moduleName = post.getSchema().getSchemaName();
        checkPermission(moduleName, "UPDATE");

        post.setPublished(status);
        return postRepository.save(post);
    }


    public List<Post> getPublicPostsByModule(String moduleName) {
        String key = moduleName.trim().toUpperCase();


        return postRepository.findBySchemaSchemaNameAndIsPublishedTrue(key);
    }

    // this fetches the schemas for the given module
    public List<PostSchema> getSchemasByModule(String moduleName) {
        String key = moduleName.trim().toUpperCase().replace(" ", "_");

        // This will allow SUPERADMIN (you) to pass
        checkPermission(key, "READ");

        return schemaRepository.findBySchemaName(key);
    }

    // this fetches the posts for the given module
    public List<Post> getPostsByModule(String moduleName) {
        String key = moduleName.trim().toUpperCase();
        checkPermission(key, "READ");
        // NOTE: This uses the derived query we defined in Repo earlier
        // (findBySchemaModuleName)
        return postRepository.findBySchemaSchemaName(key);
    }

    public Post getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found ID: " + id));


        String realModuleKey = post.getSchema().getSchemaName();

        // Ensure user has PROGRAM_READ or ACADEMIC_READ
//        checkPermission(realModuleKey, "READ");

        return post;
    }

    // Allow frontend to fetch the JSON blueprint
//    public PostSchema getSchemaDefinition(String moduleName, String schemaType) {
//        // Typically public or READ access
//        // checkPermission(moduleName, "READ"); // Optional
//        return schemaRepository.findByModuleNameAndSchemaType(moduleName, schemaType)
//                .orElseThrow(() -> new RuntimeException("Schema definition not found"));
//    }

    public PostSchema getSchemaDefinition(String moduleName, String schemaType) {

        // 1. Standardize (Clean spaces, Uppercase)
        String modKey = moduleName.trim().toUpperCase().replace(" ", "_");
        String typeKey = schemaType.trim().toUpperCase().replace(" ", "_");

        // 2. Fetch using schemaName (not moduleName)
        // This ensures we find the blueprint based on the primary definition logic.
        return schemaRepository.findBySchemaNameAndSchemaType(modKey, typeKey)
                .orElseThrow(() -> new RuntimeException("Schema definition not found for " + modKey + " / " + typeKey));
    }

    // ==========================================
    // 4. UPDATE / DELETE
    // ==========================================

    @Transactional
    public Post updatePost(Long id, String jsonString, MultipartFile file) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found ID: " + id));

        String moduleName = post.getSchema().getModuleName();
        checkPermission(moduleName, "UPDATE");

        try {
            // Update JSON Data
            if (jsonString != null && !jsonString.isEmpty()) {
                Map<String, Object> newData = mapper.readValue(jsonString, new TypeReference<>() {});
                // Replacing entirely (or you can merge)
                post.getData().putAll(newData);
            }

            // Update File
            if (file != null && !file.isEmpty()) {
                String newPath = fileService.saveFile(file);
                post.setAttachmentPath(newPath);
                post.getData().put("file_path", newPath);
            }

            return postRepository.save(post);
        } catch (IOException e) {
            throw new RuntimeException("Update failed", e);
        }
    }

    @Transactional
    public void deletePost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        checkPermission(post.getSchema().getModuleName(), "DELETE");
        postRepository.delete(post);
    }

    // ==========================================
    // HELPER: PERMISSION CHECKER
    // ==========================================
    private void checkPermission(String moduleName, String action) {
        // e.g. "ACADEMIC" + "_" + "READ" = "ACADEMIC_READ"
        String requiredAuth = moduleName.toUpperCase() + "_" + action;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 1. SuperAdmin Bypass
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPERADMIN"));

        if (isAdmin) return;

        // 2. Explicit Permission Check
        boolean hasPerm = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(requiredAuth));

        if (!hasPerm) {
            throw new AccessDeniedException("Access Denied. Required Permission: " + requiredAuth);
        }
    }
}