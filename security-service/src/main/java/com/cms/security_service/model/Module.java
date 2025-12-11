package com.cms.security_service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "module_tb")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Module {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String moduleName; // e.g., "About Us", "Academic", "Gallery"
    @JsonIgnore
    @OneToMany(mappedBy = "module",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private Set<RoleModulePermission> roleModulePermissions;
}