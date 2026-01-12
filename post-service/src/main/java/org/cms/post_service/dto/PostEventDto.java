package org.cms.post_service.dto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class PostEventDto {
    private String postId;
    private String schemaType;
    private Map<String, Object> content;
}
