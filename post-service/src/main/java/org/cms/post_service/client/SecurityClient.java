package org.cms.post_service.client;

import org.cms.post_service.dto.ModuleDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "SECURITY-SERVICE")
public interface SecurityClient {

    @GetMapping("/internal/modules/exists/{moduleName}")
    boolean checkModuleExists(@PathVariable("moduleName") String moduleName);

    @PostMapping("/internal/modules/generate")
    String generatePermissions(@RequestBody ModuleDto dto);
}
