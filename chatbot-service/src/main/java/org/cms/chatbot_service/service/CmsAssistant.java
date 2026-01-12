package org.cms.chatbot_service.service;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface CmsAssistant {

    @SystemMessage("""
        You are a friendly assistant for the CMS system of Universal College Of Management.
        Answer the user's question accurately based strictly on the provided context.
        If the information is missing from the database, please respond:
        'I do not have enough information to answer that question.'
    """)
    String chat(String userMessage);
}
