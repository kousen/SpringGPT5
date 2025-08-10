package com.kousenit.springgpt5;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@SpringBootTest
class Gpt5NativeClientUnitTest {

    @Autowired
    private Gpt5NativeClient client;
    
    @Autowired
    private RestClient openAiRestClient;
    
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        server = MockRestServiceServer.createServer(openAiRestClient);
    }

    @Test
    void shouldTestChatWithReasoningSuccess() throws Exception {
        // Given
        String responseJson = """
            {
                "output": [
                    {
                        "type": "message",
                        "content": [
                            {
                                "type": "text",
                                "text": "This is a test response"
                            }
                        ]
                    }
                ],
                "reasoning": {
                    "effort": "medium",
                    "trace": "thinking step by step"
                },
                "usage": {
                    "input_tokens": 10,
                    "output_tokens": 20
                }
            }
            """;
        
        server.expect(requestTo("https://api.openai.com/v1/responses"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        // When
        ApiResponse result = client.chatWithReasoning("test prompt", ReasoningEffort.MEDIUM);

        // Then
        ApiResponse.Success success = assertInstanceOf(ApiResponse.Success.class, result);
        assertEquals("This is a test response", success.text());
        assertEquals("medium", success.reasoningEffort());
        assertEquals("thinking step by step", success.reasoningTrace());
        assertEquals(Integer.valueOf(10), success.inputTokens());
        assertEquals(Integer.valueOf(20), success.outputTokens());
        
        server.verify();
    }

    @Test
    void shouldTestChatWithReasoningError() throws Exception {
        // Given
        String errorJson = """
            {
                "error": {
                    "message": "Invalid API key",
                    "code": "invalid_api_key"
                }
            }
            """;
        
        server.expect(requestTo("https://api.openai.com/v1/responses"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andRespond(withSuccess(errorJson, MediaType.APPLICATION_JSON));

        // When
        ApiResponse result = client.chatWithReasoning("test prompt", ReasoningEffort.HIGH);

        // Then
        ApiResponse.Error error = assertInstanceOf(ApiResponse.Error.class, result);
        assertEquals("Invalid API key", error.message());
        assertEquals("invalid_api_key", error.code());
        
        server.verify();
    }

    @Test
    void shouldTestChatWithReasoningPartialResponse() throws Exception {
        // Given - response with no text content
        String partialJson = """
            {
                "status": "partial",
                "usage": {
                    "input_tokens": 5
                }
            }
            """;
        
        server.expect(requestTo("https://api.openai.com/v1/responses"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andRespond(withSuccess(partialJson, MediaType.APPLICATION_JSON));

        // When
        ApiResponse result = client.chatWithReasoning("test prompt", ReasoningEffort.LOW);

        // Then
        ApiResponse.Partial partial = assertInstanceOf(ApiResponse.Partial.class, result);
        assertEquals("", partial.availableText());
        assertEquals("No text content available", partial.reason());
        
        server.verify();
    }

    @Test
    void shouldTestChatTextSuccess() throws Exception {
        // Given
        String responseJson = """
            {
                "output": [
                    {
                        "type": "message",
                        "content": [
                            {
                                "type": "text",
                                "text": "Chat text response"
                            }
                        ]
                    }
                ]
            }
            """;
        
        server.expect(requestTo("https://api.openai.com/v1/responses"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        // When
        String result = client.chatText("test prompt", ReasoningEffort.MEDIUM);

        // Then
        assertEquals("Chat text response", result);
        
        server.verify();
    }

    @Test
    void shouldTestChatTextWithError() throws Exception {
        // Given
        String errorJson = """
            {
                "error": {
                    "message": "Rate limit exceeded",
                    "code": "rate_limit"
                }
            }
            """;
        
        server.expect(requestTo("https://api.openai.com/v1/responses"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andRespond(withSuccess(errorJson, MediaType.APPLICATION_JSON));

        // When
        String result = client.chatText("test prompt", ReasoningEffort.HIGH);

        // Then
        assertNull(result);
        
        server.verify();
    }

    @Test
    void shouldTestChatTextWithPartial() throws Exception {
        // Given
        String partialJson = """
            {
                "output": [
                    {
                        "type": "message",
                        "content": [
                            {
                                "type": "text",
                                "text": "Incomplete response..."
                            }
                        ]
                    }
                ]
            }
            """;
        
        server.expect(requestTo("https://api.openai.com/v1/responses"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andRespond(withSuccess(partialJson, MediaType.APPLICATION_JSON));

        // When
        String result = client.chatText("test prompt", ReasoningEffort.LOW);

        // Then
        assertEquals("Incomplete response...", result);
        
        server.verify();
    }

    @Test
    void shouldTestSendWithMultipleMessages() throws Exception {
        // Given
        List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content", "You are helpful"),
            Map.of("role", "user", "content", "Hello")
        );
        
        String responseJson = """
            {
                "output": [
                    {
                        "type": "message",
                        "content": [
                            {
                                "type": "text",
                                "text": "Multi-message response"
                            }
                        ]
                    }
                ]
            }
            """;
        
        server.expect(requestTo("https://api.openai.com/v1/responses"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        // When
        ApiResponse result = client.send(messages, ReasoningEffort.MEDIUM);

        // Then
        ApiResponse.Success success = assertInstanceOf(ApiResponse.Success.class, result);
        assertEquals("Multi-message response", success.text());
        
        server.verify();
    }

    @Test
    void shouldThrowOpenAiClientExceptionOnHttpError() {
        // Given
        server.expect(requestTo("https://api.openai.com/v1/responses"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andRespond(withServerError());

        // When & Then
        OpenAiClientException exception = assertThrows(OpenAiClientException.class, () -> 
            client.chatWithReasoning("test", ReasoningEffort.MEDIUM)
        );
        
        assertEquals("Failed to send request to OpenAI API", exception.getMessage());
        
        server.verify();
    }

    @Test
    void shouldThrowOpenAiClientExceptionOnJsonError() {
        // Given - Mock server to return invalid JSON that will cause parsing issues
        server.expect(requestTo("https://api.openai.com/v1/responses"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andRespond(withSuccess("invalid json", MediaType.APPLICATION_JSON));

        // When & Then
        OpenAiClientException exception = assertThrows(OpenAiClientException.class, () -> 
            client.chatWithReasoning("test", ReasoningEffort.MEDIUM)
        );
        
        assertEquals("Failed to send request to OpenAI API", exception.getMessage());
    }
}