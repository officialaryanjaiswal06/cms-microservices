package org.cms.post_service.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.Map;

@Entity
@Table(name = "post_tb")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Post extends CommonTable{
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "schema_id", nullable = false)
    private PostSchema schema;

    // DYNAMIC CONTENT
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> data;

    private String attachmentPath;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean isPublished;

    @Override protected String provideEntryName() {
        return (data != null && data.containsKey("Title")) ?
                "Post: " + data.get("Title") : "Post ID: " + id;
    }
    @Override protected String provideEntryType() { return "DYNAMIC_POST"; }
    @Override protected String provideModuleName() { return schema.getSchemaName(); }
}
