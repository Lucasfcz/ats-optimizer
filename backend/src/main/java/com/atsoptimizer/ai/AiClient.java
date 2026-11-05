package com.atsoptimizer.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class AiClient {

    private static final Logger log = LoggerFactory.getLogger(AiClient.class);
    private final ChatClient chatClient;

    public AiClient(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Generic prompt execution method using Spring AI structured output.
     *
     * @param system instructions defining the AI role and behavior
     * @param user user prompt content containing the input data
     * @param entity class type representing the desired structured JSON mapping
     * @param <T> generic return type
     * @return deserialized instance of T from the AI model response
     */
    public <T> T sendPrompt(String system, String user, Class<T> entity) {
        log.info("Sending prompt to AI with target entity: {}", entity.getSimpleName());
        try {
            return chatClient.prompt()
                    .system(system)
                    .user(user)
                    .call()
                    .entity(entity);
        } catch (Exception e) {
            log.error("Error executing AI prompt for entity {}: {}", entity.getSimpleName(), e.getMessage(), e);
            throw new RuntimeException("AI processing failed for entity " + entity.getSimpleName() + ": " + e.getMessage(), e);
        }
    }
}
