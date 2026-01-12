package org.cms.chatbot_service.service;


import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import lombok.RequiredArgsConstructor;
import org.cms.chatbot_service.dto.PostEventDto;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import java.util.Map;
import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

@Service
@RequiredArgsConstructor
public class IngestionService {

    private final EmbeddingStore<TextSegment> vectorStore;
    private final EmbeddingModel embeddingModel;

    @RabbitListener(queues = "ai_knowledge_queue")
    public void ingestContent(PostEventDto event) {
        try {
            vectorStore.removeAll(metadataKey("post_id").isEqualTo(event.getPostId()));

            if(event.getContent() == null || event.getContent().isEmpty()) {
                return;
            }

            String text = convertMapToString(event.getSchemaType(), event.getContent());

            Metadata metadata = new Metadata();
            metadata.add("post_id", event.getPostId());
            metadata.add("schema", event.getSchemaType());

            Document doc = Document.from(text, metadata);

            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .documentSplitter(DocumentSplitters.recursive(1000, 100))
                    .embeddingModel(embeddingModel)
                    .embeddingStore(vectorStore)
                    .build();

            ingestor.ingest(doc);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String convertMapToString(String type, Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("Details for ").append(type).append(": ");
        data.forEach((k, v) -> {
            if(v != null && !k.equals("file_path")) {
                sb.append("The ").append(k).append(" is ").append(v).append(". ");
            }
        });
        return sb.toString();
    }
}