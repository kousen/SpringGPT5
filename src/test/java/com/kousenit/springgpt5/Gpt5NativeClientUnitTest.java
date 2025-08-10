package com.kousenit.springgpt5;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Gpt5NativeClient using WireMock to avoid mocking Spring Framework classes.
 * This follows the "don't mock what you don't own" principle from the Mockito team.
 */
class Gpt5NativeClientUnitTest {

    private WireMockServer wireMockServer;
    private Gpt5NativeClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().port(8089));
        wireMockServer.start();
        
        RestClient restClient = RestClient.builder()
                .baseUrl("http://localhost:8089/v1")
                .defaultHeader("Authorization", "Bearer test-key")
                .build();
                
        client = new Gpt5NativeClient(restClient, mapper, "gpt-5-nano");
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
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
        
        wireMockServer.stubFor(post(urlEqualTo("/v1/responses"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseJson)));

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
        
        wireMockServer.stubFor(post(urlEqualTo("/v1/responses"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(errorJson)));

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
        
        wireMockServer.stubFor(post(urlEqualTo("/v1/responses"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(partialJson)));

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
        
        wireMockServer.stubFor(post(urlEqualTo("/v1/responses"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseJson)));

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
        
        wireMockServer.stubFor(post(urlEqualTo("/v1/responses"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(errorJson)));

        // When
        String result = client.chatText("test prompt", ReasoningEffort.HIGH);

        // Then
        assertNull(result);
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
        
        wireMockServer.stubFor(post(urlEqualTo("/v1/responses"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseJson)));

        // When
        ApiResponse result = client.send(messages, ReasoningEffort.MEDIUM);

        // Then
        ApiResponse.Success success = assertInstanceOf(ApiResponse.Success.class, result);
        assertEquals("Multi-message response", success.text());
    }

    @Test
    void shouldThrowOpenAiClientExceptionOnHttpError() {
        // Given
        wireMockServer.stubFor(post(urlEqualTo("/v1/responses"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // When & Then
        OpenAiClientException exception = assertThrows(OpenAiClientException.class, () -> 
            client.chatWithReasoning("test", ReasoningEffort.MEDIUM)
        );
        
        assertEquals("Failed to send request to OpenAI API", exception.getMessage());
    }

    @Test
    void shouldThrowOpenAiClientExceptionOnJsonError() {
        // Given - Mock server to return invalid JSON
        wireMockServer.stubFor(post(urlEqualTo("/v1/responses"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("invalid json")));

        // When & Then
        OpenAiClientException exception = assertThrows(OpenAiClientException.class, () -> 
            client.chatWithReasoning("test", ReasoningEffort.MEDIUM)
        );
        
        assertEquals("Failed to send request to OpenAI API", exception.getMessage());
    }
}