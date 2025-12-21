package org.cms.post_service.model;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

@Entity
@Table(name = "schema_tb")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class PostSchema extends CommonTable{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column( nullable = false)
    private String schemaName; // Matches Security Module "ACADEMIC"

    @Column(nullable = false)
    private String schemaType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Map<String, Object>> structure; // Stores Fields: [{name, type, required}]

    @Override protected String provideEntryName() { return "Schema: " + schemaName; }
    @Override protected String provideEntryType() { return "SCHEMA_DEFINITION"; }
    @Override protected String provideModuleName() { return "POST-SERVICE"; }
}
