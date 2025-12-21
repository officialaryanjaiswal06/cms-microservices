package org.cms.post_service.repository;

import org.cms.post_service.model.PostSchema;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostSchemaRepository extends JpaRepository<PostSchema, Long> {

    List<PostSchema> findBySchemaName(String schemaName);

    // Useful for validation to prevent duplicate Schemas
    boolean existsBySchemaName(String schemaName);

    // Ignore Case version is safer for inputs
    Optional<PostSchema> findBySchemaNameIgnoreCase(String schemaName);
    // "Get the Event Form for Academic"

    Optional<PostSchema> findBySchemaNameAndSchemaType(String moduleName, String schemaType);

    // "Get ALL forms available in Academic" (For Frontend Dropdown)
    List<PostSchema> findByModuleName(String moduleName);
}
