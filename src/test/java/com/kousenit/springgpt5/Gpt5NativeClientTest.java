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
    void shouldTestResultRecord() {
        JsonNode rawNode = mapper.createObjectNode();
        Gpt5NativeClient.Result result = new Gpt5NativeClient.Result(
                "test response",
                "medium",
                "reasoning trace here",
                100,
                50,
                rawNode
        );

        assertEquals("test response", result.text());
        assertEquals("medium", result.reasoningEffort());
        assertEquals("reasoning trace here", result.reasoningTrace());
        assertEquals(Integer.valueOf(100), result.inputTokens());
        assertEquals(Integer.valueOf(50), result.outputTokens());
        assertEquals(rawNode, result.raw());
    }

    @Test
    void shouldTestResultRecordWithNulls() {
        Gpt5NativeClient.Result result = new Gpt5NativeClient.Result(
                null, null, null, null, null, null
        );

        assertNull(result.text());
        assertNull(result.reasoningEffort());
        assertNull(result.reasoningTrace());
        assertNull(result.inputTokens());
        assertNull(result.outputTokens());
        assertNull(result.raw());
    }

    @Test
    void shouldTestResultRecordEquality() {
        JsonNode rawNode = mapper.createObjectNode();
        Gpt5NativeClient.Result result1 = new Gpt5NativeClient.Result(
                "test", "medium", "trace", 100, 50, rawNode
        );
        Gpt5NativeClient.Result result2 = new Gpt5NativeClient.Result(
                "test", "medium", "trace", 100, 50, rawNode
        );

        assertEquals(result1, result2);
        assertEquals(result1.hashCode(), result2.hashCode());
        assertThat(result1.toString()).contains("test", "medium", "trace");
    }

    @Test
    void shouldConvertResultToApiResponseUsingRecordPatterns() {
        JsonNode rawNode = mapper.createObjectNode();
        
        // Test successful conversion
        var successResult = new Gpt5NativeClient.Result(
                "Hello, World!", "medium", "trace", 100, 50, rawNode
        );
        
        var apiResponse = successResult.toApiResponse();
        assertThat(apiResponse).isInstanceOf(ApiResponse.Success.class);
        
        if (apiResponse instanceof ApiResponse.Success(var text, var effort, var trace, var input, var output, var raw)) {
            assertEquals("Hello, World!", text);
            assertEquals("medium", effort);
            assertEquals("trace", trace);
            assertEquals(Integer.valueOf(100), input);
            assertEquals(Integer.valueOf(50), output);
        } else {
            fail("Expected Success response");
        }
    }

    @Test
    void shouldHandleEmptyResultAsPartial() {
        JsonNode rawNode = mapper.createObjectNode();
        
        var emptyResult = new Gpt5NativeClient.Result(
                "", "low", "trace", 10, 5, rawNode
        );
        
        var apiResponse = emptyResult.toApiResponse();
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
    void shouldDemonstrateAdvancedJsonNodePatternMatching() throws Exception {
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
    void shouldTestSealedInterfaceExhaustiveness() throws Exception {
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
            case ApiResponse.Success(var text, var effort, var trace, var input, var output, var raw) -> text;
            case ApiResponse.Error(var message, var code, var raw) -> null;
            case ApiResponse.Partial(var availableText, var reason, var raw) -> availableText;
            // No default needed - sealed interface guarantees exhaustiveness
        };
    }
}