package org.cms.chatbot_service.controller;
import lombok.RequiredArgsConstructor;
import org.cms.chatbot_service.service.CmsAssistant;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class ChatController {

    private final CmsAssistant assistant;

    record ChatRequest(String question) {}

//    @PostMapping("/chat")
//    public Map<String, String> chat(@RequestBody ChatRequest request) {
//        String answer = assistant.chat(request.question());
//        return Map.of("answer", answer);
//    }

    @PostMapping("/chat")
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public Map<String, String> chat(@RequestBody ChatRequest request) {
        String answer = assistant.chat(request.question());
        return Map.of("answer", answer);
    }
}
