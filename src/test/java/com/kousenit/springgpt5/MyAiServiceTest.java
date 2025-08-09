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
            case ApiResponse.Success(var text, var effort, var trace, var input, var output, var raw) -> {
                System.out.println(text);
                System.out.println(input);
                System.out.println(output);
                System.out.println(effort);
                assertTrue(text.contains("data-oriented programming"));
            }
            case ApiResponse.Error(var message, var code, var raw) -> {
                fail("Expected success but got error: " + message);
            }
            case ApiResponse.Partial(var availableText, var reason, var raw) -> {
                fail("Expected success but got partial: " + reason);
            }
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
            case ApiResponse.Success(var text, var effort, var trace, var input, var output, var raw) -> {
                System.out.println(text);
                System.out.println("Input tokens: " + input);
                System.out.println("Output tokens: " + output);
                System.out.println("Effort: " + effort);
                assertThat(text).containsIgnoringCase("data-oriented programming");
            }
            case ApiResponse.Error(var message, var code, var raw) -> {
                fail("Expected success but got error: " + message);
            }
            case ApiResponse.Partial(var availableText, var reason, var raw) -> {
                fail("Expected success but got partial: " + reason);
            }
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