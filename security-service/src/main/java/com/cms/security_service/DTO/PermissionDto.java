package com.cms.security_service.DTO;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionDto {

    // Identifies which module this permission block belongs to (e.g., "Academic")
    private String moduleName;

    // The granular 4-bit switches for this specific module
    private boolean canSelect;
    private boolean canCreate;
    private boolean canUpdate;
    private boolean canDelete;
}

