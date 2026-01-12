package org.cms.chatbot_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostEventDto {
    private String postId;
    private String schemaType;
    private Map<String, Object> content;
}