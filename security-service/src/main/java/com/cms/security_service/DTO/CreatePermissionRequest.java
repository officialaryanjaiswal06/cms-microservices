package com.cms.security_service.DTO;

import lombok.Data;

@Data
public class CreatePermissionRequest{
    private String code;
    private String resource;
    private String action;
    private String description;
}
