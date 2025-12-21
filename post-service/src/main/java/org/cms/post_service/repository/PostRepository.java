package org.cms.post_service.repository;

import org.cms.post_service.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findBySchemaSchemaName(String schemaName);

    List<Post> findBySchemaSchemaNameAndIsPublishedTrue(String schemaName);

    boolean existsBySchemaId(Long schemaId);
}
