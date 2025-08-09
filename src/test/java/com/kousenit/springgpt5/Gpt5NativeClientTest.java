package com.kousenit.springgpt5;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

class Gpt5NativeClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldExtractTextFromDirectOutputText() throws Exception {
        String json = """
                {
                  "output_text": "This is the direct response"
                }
                """;
        JsonNode node = mapper.readTree(json);
        String result = Gpt5NativeClient.extractText(node);
        assertEquals("This is the direct response", result);
    }

    @Test
    void shouldExtractTextFromOutputArray() throws Exception {
        String json = """
                {
                  "output": [
                    {
                      "type": "message",
                      "content": [
                        {
                          "type": "output_text",
                          "text": "Response from output array"
                        }
                      ]
                    }
                  ]
                }
                """;
        JsonNode node = mapper.readTree(json);
        String result = Gpt5NativeClient.extractText(node);
        assertEquals("Response from output array", result);
    }

    @Test
    void shouldExtractTextFromMultipleContentItems() throws Exception {
        String json = """
                {
                  "output": [
                    {
                      "type": "message",
                      "content": [
                        {
                          "type": "output_text",
                          "text": "First part"
                        },
                        {
                          "type": "output_text",
                          "text": " Second part"
                        }
                      ]
                    }
                  ]
                }
                """;
        JsonNode node = mapper.readTree(json);
        String result = Gpt5NativeClient.extractText(node);
        assertEquals("First part Second part", result);
    }

    @Test
    void shouldReturnNullForEmptyResponse() throws Exception {
        String json = """
                {
                  "status": "completed"
                }
                """;
        JsonNode node = mapper.readTree(json);
        String result = Gpt5NativeClient.extractText(node);
        assertNull(result);
    }

    @Test
    void shouldUseFallbackPaths() throws Exception {
        String json = """
                {
                  "output": [
                    {
                      "content": [
                        {
                          "text": "Fallback text"
                        }
                      ]
                    }
                  ]
                }
                """;
        JsonNode node = mapper.readTree(json);
        String result = Gpt5NativeClient.extractText(node);
        assertEquals("Fallback text", result);
    }

    @Test
    void shouldTestApiResponseSuccess() {
        JsonNode rawNode = mapper.createObjectNode();
        ApiResponse response = new ApiResponse.Success(
                "test response",
                "medium",
                "reasoning trace here",
                100,
                50,
                rawNode
        );

        if (response instanceof ApiResponse.Success success) {
            assertEquals("test response", success.text());
            assertEquals("medium", success.reasoningEffort());
            assertEquals("reasoning trace here", success.reasoningTrace());
            assertEquals(Integer.valueOf(100), success.inputTokens());
            assertEquals(Integer.valueOf(50), success.outputTokens());
            assertEquals(rawNode, success.raw());
        } else {
            fail("Expected Success response");
        }
    }

    @Test
    void shouldTestApiResponseWithNulls() {
        ApiResponse response = new ApiResponse.Success(
                null, null, null, null, null, null
        );

        if (response instanceof ApiResponse.Success success) {
            assertNull(success.text());
            assertNull(success.reasoningEffort());
            assertNull(success.reasoningTrace());
            assertNull(success.inputTokens());
            assertNull(success.outputTokens());
            assertNull(success.raw());
        } else {
            fail("Expected Success response");
        }
    }

    @Test
    void shouldTestApiResponseEquality() {
        JsonNode rawNode = mapper.createObjectNode();
        ApiResponse response1 = new ApiResponse.Success(
                "test", "medium", "trace", 100, 50, rawNode
        );
        ApiResponse response2 = new ApiResponse.Success(
                "test", "medium", "trace", 100, 50, rawNode
        );

        assertEquals(response1, response2);
        assertEquals(response1.hashCode(), response2.hashCode());
        assertThat(response1.toString()).contains("test", "medium", "trace");
    }

    @Test
    void shouldCreateSuccessResponseUsingRecordPatterns() {
        JsonNode rawNode = mapper.createObjectNode();
        
        // Test direct Success creation
        var apiResponse = new ApiResponse.Success(
                "Hello, World!", "medium", "trace", 100, 50, rawNode
        );
        
        assertThat(apiResponse).isInstanceOf(ApiResponse.Success.class);
        
        if (apiResponse instanceof ApiResponse.Success success) {
            assertEquals("Hello, World!", success.text());
            assertEquals("medium", success.reasoningEffort());
            assertEquals("trace", success.reasoningTrace());
            assertEquals(Integer.valueOf(100), success.inputTokens());
            assertEquals(Integer.valueOf(50), success.outputTokens());
        } else {
            fail("Expected Success response");
        }
    }

    @Test
    void shouldHandlePartialResponse() {
        JsonNode rawNode = mapper.createObjectNode();
        
        // Create as generic ApiResponse to allow pattern matching
        ApiResponse apiResponse = new ApiResponse.Partial(
                "", "Incomplete response", rawNode
        );
        
        assertThat(apiResponse).isInstanceOf(ApiResponse.Partial.class);
        
        // Test pattern matching on sealed interface
        var textContent = switch (apiResponse) {
            case ApiResponse.Success success -> success.text();
            case ApiResponse.Error error -> null;
            case ApiResponse.Partial partial -> partial.availableText();
        };
        
        assertEquals("", textContent);
    }

    @Test
    void shouldDemonstrateAdvancedJsonNodePatternMatching() {
        // Test different JsonNode types with pattern matching
        var objectNode = mapper.createObjectNode();
        objectNode.put("type", "message");
        
        var arrayNode = mapper.createArrayNode();
        arrayNode.add("item1");
        arrayNode.add("item2");
        
        // Pattern matching with instanceof for JsonNode types
        String result1 = processJsonNode(objectNode);
        String result2 = processJsonNode(arrayNode);
        
        assertEquals("Found object with type: message", result1);
        assertEquals("Found array with 2 elements", result2);
    }
    
    private String processJsonNode(JsonNode node) {
        return switch (node) {
            case JsonNode n when n.isObject() -> 
                "Found object with type: " + n.path("type").asText("unknown");
            case JsonNode n when n.isArray() -> 
                "Found array with " + n.size() + " elements";
            case JsonNode n when n.isTextual() -> 
                "Found text: " + n.asText();
            default -> "Unknown node type";
        };
    }

    @Test
    void shouldTestSealedInterfaceExhaustiveness() {
        var successResponse = new ApiResponse.Success(
                "test", "medium", "trace", 100, 50, mapper.createObjectNode()
        );
        var errorResponse = new ApiResponse.Error(
                "API Error", "invalid_request", mapper.createObjectNode()
        );
        var partialResponse = new ApiResponse.Partial(
                "partial text", "timeout", mapper.createObjectNode()
        );
        
        // Test exhaustive pattern matching - compiler ensures all cases covered
        assertEquals("test", extractContent(successResponse));
        assertNull(extractContent(errorResponse));
        assertEquals("partial text", extractContent(partialResponse));
    }
    
    private String extractContent(ApiResponse response) {
        return switch (response) {
            case ApiResponse.Success success -> success.text();
            case ApiResponse.Error ignored -> null;
            case ApiResponse.Partial partial -> partial.availableText();
            // No default needed - sealed interface guarantees exhaustiveness
        };
    }
}