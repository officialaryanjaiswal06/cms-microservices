package com.cms.security_service.model;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "role_module_function_tb",
        uniqueConstraints = { @UniqueConstraint(columnNames = { "role_id", "module_id" }) })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class RoleModulePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK to Role
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    // FK to Module
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;

    // Static Functionalities (CRUD)
    @Column(name = "can_select", nullable = false)
    private boolean canSelect = false;

    @Column(name = "can_create", nullable = false)
    private boolean canCreate = false;

    @Column(name = "can_update", nullable = false)
    private boolean canUpdate = false;

    @Column(name = "can_delete", nullable = false)
    private boolean canDelete = false;
}