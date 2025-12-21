
package org.cms.post_service.controller;

import org.springframework.core.io.Resource;
import lombok.RequiredArgsConstructor;
import org.cms.post_service.model.Post;
import org.cms.post_service.model.PostSchema;
import org.cms.post_service.service.FileStorageService;
import org.cms.post_service.service.PostService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import org.springframework.http.HttpHeaders;

@RestController
@RequestMapping("/content")
@RequiredArgsConstructor
public class ContentController {

    private final PostService service;
    private final FileStorageService fileService;

    // ==========================================
    // 1. ADMIN: DEFINE STRUCTURE
    // ==========================================

    /**
     * Define a new Schema.
     * Body JSON must include: { "moduleName": "ACADEMIC", "schemaType": "EVENT", "structure": [...] }
     */
    @PostMapping("/schema")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<?> createSchema(@RequestBody PostSchema schema) {
        try {
            return ResponseEntity.ok(service.createDefinition(schema));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Get a specific Schema Blueprint (used by Frontend to draw forms)
     * URL Example: /content/schema/ACADEMIC/EVENT
     */
    @GetMapping("/schema/{moduleName}/{schemaType}")
    public ResponseEntity<?> getSchemaDefinition(
            @PathVariable String moduleName,
            @PathVariable String schemaType
    ) {
        try {
            PostSchema schema = service.getSchemaDefinition(moduleName, schemaType);
            return ResponseEntity.ok(schema);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    // ==========================================
    // 2. EDITOR: CREATE & READ CONTENT
    // ==========================================

    /**
     * Create Content for a specific Type in a Module
     * URL Example: POST /content/post/ACADEMIC/EVENT
     * Checks Permission: ACADEMIC_CREATE
     */
    @PostMapping("/post/{moduleName}/{schemaType}")
    public ResponseEntity<?> createPost(
            @PathVariable String moduleName,
            @PathVariable String schemaType,
            @RequestPart("data") String json,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "isPublished", defaultValue = "false") boolean isPublished
    ) {
        try {
            // Note: Service method is 'createDynamicPost', takes 2 separate ID strings
            return ResponseEntity.ok(service.createDynamicPost(moduleName, schemaType, json, file,isPublished));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }


    @PatchMapping("/post/entry/{id}/status")
    public ResponseEntity<?> setStatus(@PathVariable Long id, @RequestParam boolean publish) {
        try {
            return ResponseEntity.ok(service.togglePublishStatus(id, publish));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @GetMapping("/public/posts/{moduleName}")
    public ResponseEntity<?> getPublicPosts(@PathVariable String moduleName) {
        try {
            // Returns filtered list
            return ResponseEntity.ok(service.getPublicPostsByModule(moduleName));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    /**
     * Get All Posts for a whole Module (e.g. everything in Academic)
     * URL Example: GET /content/posts/ACADEMIC
     */
    @GetMapping("/posts/{moduleName}")
    public ResponseEntity<?> getPostsByModule(@PathVariable String moduleName) {
        try {
            List<Post> posts = service.getPostsByModule(moduleName);
            return ResponseEntity.ok(posts);
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }


    /**
     * Get Single Post
     */

    @GetMapping("/post/entry/{id}")
    public ResponseEntity<?> getPostById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.getPostById(id));
        } catch (AccessDeniedException e) {
            // ✅ Return 403 for Permissions errors
            return ResponseEntity.status(403).body("Access Denied: " + e.getMessage());
        } catch (RuntimeException e) {
            // ✅ Return 404 ONLY if it is truly Not Found
            if (e.getMessage().contains("Post not found")) {
                return ResponseEntity.status(404).body(e.getMessage());
            }
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    /**
     * Update Post
     * Checks permission dynamically based on the existing post's module.
     */
    @PutMapping("/post/entry/{id}")
    public ResponseEntity<?> updatePost(
            @PathVariable Long id,
            @RequestPart(value = "data", required = false) String json,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        try {
            return ResponseEntity.ok(service.updatePost(id, json, file));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    /**
     * Delete Post
     */
    @DeleteMapping("/post/entry/{id}")
    public ResponseEntity<?> deletePost(@PathVariable Long id) {
        try {
            service.deletePost(id);
            return ResponseEntity.ok("Post deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }


    }

    @GetMapping("/schemas/{moduleName}")
    public ResponseEntity<?> getSchemasByModule(@PathVariable String moduleName) {
        try {
            // Returns a LIST of blueprints (e.g. EVENT, NOTICE) for that module
            List<PostSchema> schemas = service.getSchemasByModule(moduleName);
            return ResponseEntity.ok(schemas);
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }


    @PostMapping("/upload")
    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            // Save to Disk
            String filename = fileService.saveFile(file);

            // Generate the Public URL (This is what the frontend puts in the JSON form)
            // Note: Returns a URL pointing to the GET endpoint below
            String fileUrl = "http://localhost:8080/content/images/" + filename;

            return ResponseEntity.ok(fileUrl);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        }
    }

    // 2. Serving Logic (So the browser can see the image)
    // Endpoint: GET /content/images/{filename}
    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> serveImage(@PathVariable String filename) {
        try {
            // Re-using logic assuming fileService has a 'load' method.
            // If you only implemented 'saveFile' in service, see Step 2 below.
            org.springframework.core.io.Resource file = fileService.loadFileAsResource(filename);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFilename() + "\"")
                    .contentType(MediaType.IMAGE_JPEG) // Or determine dynamically
                    .body(file);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
