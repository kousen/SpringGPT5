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
        Gpt5NativeClient.Result result = service.gpt5ReasoningAnswer("""
                Explain the benefits of data-oriented programming
                to a Java developer.
                """);
        assertNotNull(result);
        System.out.println(result.text());
        System.out.println(result.inputTokens());
        System.out.println(result.outputTokens());
        System.out.println(result.reasoningEffort());
        assertTrue(result.text().contains("data-oriented programming"));
    }

    @SlowIntegrationTest
    void testGpt5ReasoningAnswer() throws Exception {
        Gpt5NativeClient.Result result = service.gpt5ReasoningAnswer("""
                Explain the benefits of data-oriented programming
                to a Java developer.
                """, ReasoningEffort.LOW);
        assertNotNull(result);
        System.out.println(result.text());
        System.out.println("Input tokens: " + result.inputTokens());
        System.out.println("Output tokens: " + result.outputTokens());
        System.out.println("Effort: " + result.reasoningEffort());
        assertThat(result.text())
                .containsIgnoringCase("data-oriented programming");

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