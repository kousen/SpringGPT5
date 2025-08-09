package com.kousenit.springgpt5;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MyAiServiceTest {
    @Autowired
    private MyAiService service;

    @Test
    void normalAnswer() {
        String answer = service.normalAnswer("""
                What is the answer to life, the universe, and everything?
                """);
        assertNotNull(answer);
        System.out.println(answer);
        assertTrue(answer.contains("42"));
    }

    @SlowIntegrationTest
    void gpt5ReasoningAnswer() throws Exception {
        ApiResponse response = service.gpt5ReasoningAnswer("""
                Explain the benefits of data-oriented programming
                to a Java developer.
                """);
        assertNotNull(response);
        
        switch (response) {
            case ApiResponse.Success success -> {
                System.out.println(success.text());
                System.out.println(success.inputTokens());
                System.out.println(success.outputTokens());
                System.out.println(success.reasoningEffort());
                assertTrue(success.text().contains("data-oriented programming"));
            }
            case ApiResponse.Error error -> 
                fail("Expected success but got error: " + error.message());
            case ApiResponse.Partial partial -> 
                fail("Expected success but got partial: " + partial.reason());
        }
    }

    @SlowIntegrationTest
    void testGpt5ReasoningAnswer() throws Exception {
        ApiResponse response = service.gpt5ReasoningAnswer("""
                Explain the benefits of data-oriented programming
                to a Java developer.
                """, ReasoningEffort.LOW);
        assertNotNull(response);
        
        switch (response) {
            case ApiResponse.Success success -> {
                System.out.println(success.text());
                System.out.println("Input tokens: " + success.inputTokens());
                System.out.println("Output tokens: " + success.outputTokens());
                System.out.println("Effort: " + success.reasoningEffort());
                assertThat(success.text()).containsIgnoringCase("data-oriented programming");
            }
            case ApiResponse.Error error -> 
                fail("Expected success but got error: " + error.message());
            case ApiResponse.Partial partial -> 
                fail("Expected success but got partial: " + partial.reason());
        }
    }

    @SlowIntegrationTest
    void gpt5TextAnswer() throws Exception {
        String answer = service.gpt5TextAnswer("""
                Why should I use Java for
                AI integration instead of
                Python or JavaScript/TypeScript?
                """, ReasoningEffort.LOW);
        assertThat(answer)
                .isNotEmpty()
                .contains("Java");
        System.out.println(answer);
    }
}