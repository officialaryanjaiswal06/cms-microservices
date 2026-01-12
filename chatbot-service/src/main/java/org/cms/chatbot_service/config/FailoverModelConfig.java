
package org.cms.chatbot_service.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

@Configuration
@Slf4j
public class FailoverModelConfig {

    @Value("${cms.ai.api-keys}")
    private List<String> apiKeys;

    // Database connection details
    @Value("${spring.datasource.username}") private String jdbcUser;
    @Value("${spring.datasource.password}") private String jdbcPass;
    private static final String DB_HOST = "127.0.0.1";
    private static final int DB_PORT = 5435;
    private static final String DB_NAME = "cms_ai_db";

    @Bean
    @Primary
    public ChatLanguageModel failoverChatModel() {
        if (apiKeys == null || apiKeys.isEmpty()) {
            throw new RuntimeException("CRITICAL: No API Keys found in properties!");
        }

        // 1. Take the first key and Clean it
        String primaryKey = apiKeys.get(0).trim();

        log.info("--------------------------------------------------");
        log.info("🤖 AI CONFIG: Connecting using 'gemini-pro'");
        log.info("🔑 KEY USED: ...{}", primaryKey.substring(Math.max(0, primaryKey.length() - 5)));
        log.info("--------------------------------------------------");


        return GoogleAiGeminiChatModel.builder()
                .apiKey(primaryKey)
                .modelName("gemini-2.5-flash")
                .temperature(0.7)
                .build();
    }

    @Bean
    @Primary
    public EmbeddingModel localEmbeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    @Bean
    public EmbeddingStore<dev.langchain4j.data.segment.TextSegment> embeddingStore() {
        return PgVectorEmbeddingStore.builder()
                .host(DB_HOST)
                .port(DB_PORT)
                .database(DB_NAME)
                .user(jdbcUser)
                .password(jdbcPass)
                .table("embeddings")
                .dimension(384)
                .dropTableFirst(false)
                .build();
    }

    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore<dev.langchain4j.data.segment.TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(3)
                .minScore(0.5)
                .build();
    }
}