package com.kousenit.springgpt5;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class Gpt5NativeClientHelperTest {

    private ObjectMapper mapper;
    private Gpt5NativeClient client;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        client = new Gpt5NativeClient(null, mapper, "gpt-5");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldTestFirstTextHelper() throws Exception {
        // Given
        String json = """
            {
                "output": {
                    "content": {
                        "text": "Found text"
                    }
                },
                "response": {
                    "content": {
                        "text": "Alternative text"
                    }
                }
            }
            """;
        
        JsonNode node = mapper.readTree(json);
        
        // Use reflection to test private static method
        Method firstTextMethod = Gpt5NativeClient.class.getDeclaredMethod("firstText", JsonNode.class, String[].class);
        firstTextMethod.setAccessible(true);
        
        // When
        Optional<String> result = (Optional<String>) firstTextMethod.invoke(null, node, 
            new String[]{"/output/content/text", "/response/content/text"});
        
        // Then
        assertTrue(result.isPresent());
        assertEquals("Found text", result.get());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldTestFirstTextHelperWithFallback() throws Exception {
        // Given
        String json = """
            {
                "response": {
                    "content": {
                        "text": "Fallback text"
                    }
                }
            }
            """;
        
        JsonNode node = mapper.readTree(json);
        
        // Use reflection to test private static method
        Method firstTextMethod = Gpt5NativeClient.class.getDeclaredMethod("firstText", JsonNode.class, String[].class);
        firstTextMethod.setAccessible(true);
        
        // When
        Optional<String> result = (Optional<String>) firstTextMethod.invoke(null, node, 
            new String[]{"/nonexistent/path", "/response/content/text"});
        
        // Then
        assertTrue(result.isPresent());
        assertEquals("Fallback text", result.get());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldTestFirstTextHelperNotFound() throws Exception {
        // Given
        String json = """
            {
                "other": "data"
            }
            """;
        
        JsonNode node = mapper.readTree(json);
        
        // Use reflection to test private static method
        Method firstTextMethod = Gpt5NativeClient.class.getDeclaredMethod("firstText", JsonNode.class, String[].class);
        firstTextMethod.setAccessible(true);
        
        // When
        Optional<String> result = (Optional<String>) firstTextMethod.invoke(null, node, 
            new String[]{"/nonexistent/path", "/another/missing/path"});
        
        // Then
        assertFalse(result.isPresent());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldTestFirstNodeHelper() throws Exception {
        // Given
        String json = """
            {
                "reasoning": {
                    "effort": "high",
                    "trace": "detailed thinking"
                },
                "meta": {
                    "reasoning": {
                        "effort": "medium"
                    }
                }
            }
            """;
        
        JsonNode node = mapper.readTree(json);
        
        // Use reflection to test private static method
        Method firstNodeMethod = Gpt5NativeClient.class.getDeclaredMethod("firstNode", JsonNode.class, String[].class);
        firstNodeMethod.setAccessible(true);
        
        // When
        Optional<JsonNode> result = (Optional<JsonNode>) firstNodeMethod.invoke(null, node, 
            new String[]{"/reasoning", "/meta/reasoning"});
        
        // Then
        assertTrue(result.isPresent());
        assertEquals("high", result.get().path("effort").asText());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldTestTextHelper() throws Exception {
        // Given
        String json = """
            {
                "reasoning": {
                    "effort": "medium",
                    "trace": "step by step"
                }
            }
            """;
        
        JsonNode node = mapper.readTree(json);
        JsonNode reasoningNode = node.path("reasoning");
        
        // Use reflection to test private static method
        Method textMethod = Gpt5NativeClient.class.getDeclaredMethod("text", JsonNode.class, String.class);
        textMethod.setAccessible(true);
        
        // When
        Optional<String> effortResult = (Optional<String>) textMethod.invoke(null, reasoningNode, "/effort");
        Optional<String> traceResult = (Optional<String>) textMethod.invoke(null, reasoningNode, "/trace");
        Optional<String> missingResult = (Optional<String>) textMethod.invoke(null, reasoningNode, "/missing");
        
        // Then
        assertTrue(effortResult.isPresent());
        assertEquals("medium", effortResult.get());
        assertTrue(traceResult.isPresent());
        assertEquals("step by step", traceResult.get());
        assertFalse(missingResult.isPresent());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldTestIntValueHelper() throws Exception {
        // Given
        String json = """
            {
                "usage": {
                    "input_tokens": 150,
                    "output_tokens": 250,
                    "invalid_tokens": "not a number"
                }
            }
            """;
        
        JsonNode node = mapper.readTree(json);
        
        // Use reflection to test private static method
        Method intValueMethod = Gpt5NativeClient.class.getDeclaredMethod("intValue", JsonNode.class, String.class);
        intValueMethod.setAccessible(true);
        
        // When
        Optional<Integer> inputResult = (Optional<Integer>) intValueMethod.invoke(null, node, "/usage/input_tokens");
        Optional<Integer> outputResult = (Optional<Integer>) intValueMethod.invoke(null, node, "/usage/output_tokens");
        Optional<Integer> missingResult = (Optional<Integer>) intValueMethod.invoke(null, node, "/usage/missing_tokens");
        
        // Then
        assertTrue(inputResult.isPresent());
        assertEquals(150, inputResult.get());
        assertTrue(outputResult.isPresent());
        assertEquals(250, outputResult.get());
        assertFalse(missingResult.isPresent());
    }

    @Test
    void shouldTestParseApiResponseWithSuccess() throws Exception {
        // Given
        String json = """
            {
                "output": [
                    {
                        "type": "message",
                        "content": [
                            {
                                "type": "text",
                                "text": "Successful response"
                            }
                        ]
                    }
                ],
                "reasoning": {
                    "effort": "high",
                    "trace": "Detailed thinking process"
                },
                "usage": {
                    "input_tokens": 100,
                    "output_tokens": 200
                }
            }
            """;
        
        JsonNode node = mapper.readTree(json);
        
        // Use reflection to test private method
        Method parseMethod = Gpt5NativeClient.class.getDeclaredMethod("parseApiResponse", JsonNode.class);
        parseMethod.setAccessible(true);
        
        // When
        ApiResponse result = (ApiResponse) parseMethod.invoke(client, node);
        
        // Then
        ApiResponse.Success success = assertInstanceOf(ApiResponse.Success.class, result);
        assertEquals("Successful response", success.text());
        assertEquals("high", success.reasoningEffort());
        assertEquals("Detailed thinking process", success.reasoningTrace());
        assertEquals(100, success.inputTokens());
        assertEquals(200, success.outputTokens());
    }

    @Test
    void shouldTestParseApiResponseWithError() throws Exception {
        // Given
        String json = """
            {
                "error": {
                    "message": "Authentication failed",
                    "code": "auth_error"
                }
            }
            """;
        
        JsonNode node = mapper.readTree(json);
        
        // Use reflection to test private method
        Method parseMethod = Gpt5NativeClient.class.getDeclaredMethod("parseApiResponse", JsonNode.class);
        parseMethod.setAccessible(true);
        
        // When
        ApiResponse result = (ApiResponse) parseMethod.invoke(client, node);
        
        // Then
        ApiResponse.Error error = assertInstanceOf(ApiResponse.Error.class, result);
        assertEquals("Authentication failed", error.message());
        assertEquals("auth_error", error.code());
    }

    @Test
    void shouldTestParseApiResponseWithPartialContent() throws Exception {
        // Given - response with empty text
        String json = """
            {
                "status": "incomplete",
                "metadata": "some data"
            }
            """;
        
        JsonNode node = mapper.readTree(json);
        
        // Use reflection to test private method
        Method parseMethod = Gpt5NativeClient.class.getDeclaredMethod("parseApiResponse", JsonNode.class);
        parseMethod.setAccessible(true);
        
        // When
        ApiResponse result = (ApiResponse) parseMethod.invoke(client, node);
        
        // Then
        ApiResponse.Partial partial = assertInstanceOf(ApiResponse.Partial.class, result);
        assertEquals("", partial.availableText());
        assertEquals("No text content available", partial.reason());
    }

    @Test
    void shouldTestParseApiResponseWithMissingReasoning() throws Exception {
        // Given
        String json = """
            {
                "output": [
                    {
                        "type": "message",
                        "content": [
                            {
                                "type": "text",
                                "text": "Response without reasoning"
                            }
                        ]
                    }
                ],
                "usage": {
                    "input_tokens": 50
                }
            }
            """;
        
        JsonNode node = mapper.readTree(json);
        
        // Use reflection to test private method
        Method parseMethod = Gpt5NativeClient.class.getDeclaredMethod("parseApiResponse", JsonNode.class);
        parseMethod.setAccessible(true);
        
        // When
        ApiResponse result = (ApiResponse) parseMethod.invoke(client, node);
        
        // Then
        ApiResponse.Success success = assertInstanceOf(ApiResponse.Success.class, result);
        assertEquals("Response without reasoning", success.text());
        assertEquals("unknown", success.reasoningEffort()); // Default value
        assertEquals("", success.reasoningTrace()); // Default value
        assertEquals(50, success.inputTokens());
        assertEquals(0, success.outputTokens()); // Default value
    }
}