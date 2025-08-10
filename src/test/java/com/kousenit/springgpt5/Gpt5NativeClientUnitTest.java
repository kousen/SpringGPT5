package com.kousenit.springgpt5;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
class Gpt5NativeClientUnitTest {

    @MockitoBean
    private RestClient restClient;
    
    @MockitoBean
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    
    @MockitoBean
    private RestClient.RequestBodySpec requestBodySpec;
    
    @MockitoBean
    private RestClient.ResponseSpec responseSpec;
    
    private ObjectMapper mapper;
    private Gpt5NativeClient client;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        client = new Gpt5NativeClient(restClient, mapper, "gpt-5-nano");
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
        
        JsonNode responseNode = mapper.readTree(responseJson);
        
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/responses")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(JsonNode.class)).thenReturn(responseNode);

        // When
        ApiResponse result = client.chatWithReasoning("test prompt", ReasoningEffort.MEDIUM);

        // Then
        ApiResponse.Success success = assertInstanceOf(ApiResponse.Success.class, result);
        assertEquals("This is a test response", success.text());
        assertEquals("medium", success.reasoningEffort());
        assertEquals("thinking step by step", success.reasoningTrace());
        assertEquals(Integer.valueOf(10), success.inputTokens());
        assertEquals(Integer.valueOf(20), success.outputTokens());
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
        
        JsonNode errorNode = mapper.readTree(errorJson);
        
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/responses")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(JsonNode.class)).thenReturn(errorNode);

        // When
        ApiResponse result = client.chatWithReasoning("test prompt", ReasoningEffort.HIGH);

        // Then
        ApiResponse.Error error = assertInstanceOf(ApiResponse.Error.class, result);
        assertEquals("Invalid API key", error.message());
        assertEquals("invalid_api_key", error.code());
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
        
        JsonNode partialNode = mapper.readTree(partialJson);
        
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/responses")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(JsonNode.class)).thenReturn(partialNode);

        // When
        ApiResponse result = client.chatWithReasoning("test prompt", ReasoningEffort.LOW);

        // Then
        ApiResponse.Partial partial = assertInstanceOf(ApiResponse.Partial.class, result);
        assertEquals("", partial.availableText());
        assertEquals("No text content available", partial.reason());
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
        
        JsonNode responseNode = mapper.readTree(responseJson);
        
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/responses")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(JsonNode.class)).thenReturn(responseNode);

        // When
        String result = client.chatText("test prompt", ReasoningEffort.MEDIUM);

        // Then
        assertEquals("Chat text response", result);
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
        
        JsonNode errorNode = mapper.readTree(errorJson);
        
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/responses")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(JsonNode.class)).thenReturn(errorNode);

        // When
        String result = client.chatText("test prompt", ReasoningEffort.HIGH);

        // Then
        assertNull(result);
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
        
        JsonNode partialNode = mapper.readTree(partialJson);
        
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/responses")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(JsonNode.class)).thenReturn(partialNode);

        // When
        String result = client.chatText("test prompt", ReasoningEffort.LOW);

        // Then
        assertEquals("Incomplete response...", result);
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
        
        JsonNode responseNode = mapper.readTree(responseJson);
        
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/responses")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(JsonNode.class)).thenReturn(responseNode);

        // When
        ApiResponse result = client.send(messages, ReasoningEffort.MEDIUM);

        // Then
        ApiResponse.Success success = assertInstanceOf(ApiResponse.Success.class, result);
        assertEquals("Multi-message response", success.text());
    }

    @Test
    void shouldThrowOpenAiClientExceptionOnHttpError() {
        // Given
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/responses")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(JsonNode.class)).thenThrow(new RestClientException("Network error"));

        // When & Then
        OpenAiClientException exception = assertThrows(OpenAiClientException.class, () -> 
            client.chatWithReasoning("test", ReasoningEffort.MEDIUM)
        );
        
        assertEquals("Failed to send request to OpenAI API", exception.getMessage());
        assertThat(exception.getCause()).isInstanceOf(RestClientException.class);
    }

    @Test
    void shouldThrowOpenAiClientExceptionOnJsonError() {
        // Given - create a client with a mapper that will fail
        ObjectMapper faultyMapper = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) {
                throw new RuntimeException("JSON serialization failed");
            }
        };
        
        Gpt5NativeClient faultyClient = new Gpt5NativeClient(restClient, faultyMapper, "gpt-5");

        // When & Then
        OpenAiClientException exception = assertThrows(OpenAiClientException.class, () -> 
            faultyClient.chatWithReasoning("test", ReasoningEffort.MEDIUM)
        );
        
        assertEquals("Failed to send request to OpenAI API", exception.getMessage());
        assertThat(exception.getCause()).hasMessageContaining("JSON serialization failed");
    }
}