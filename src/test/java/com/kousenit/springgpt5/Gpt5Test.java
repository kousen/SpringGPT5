package com.kousenit.springgpt5;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class Gpt5Test {
    @Autowired
    private OpenAiChatModel model;

    private ChatClient client;

    @BeforeEach
    void setup() {
        client = ChatClient.create(model);
    }

    @SlowIntegrationTest
    void testChat() {
        ChatResponse chatResponse = client.prompt()
                .user("Explain quines in Java.")
                .call()
                .chatResponse();

        assertNotNull(chatResponse);
        System.out.println(chatResponse.getResult().getOutput().getText());
        ChatResponseMetadata metadata = chatResponse.getMetadata();
        assertNotNull(metadata);
        System.out.printf("Usage: %s%n", metadata.getUsage());
        System.out.printf("Model: %s%n", metadata.getModel());
    }
}
