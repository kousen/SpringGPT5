package com.kousenit.springgpt5;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests showcasing advanced Java 21 features with sealed interfaces and pattern matching
 */
class ModernApiResponseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldDemonstrateRecordPatternMatchingInSwitchExpression() {
        // Test different response types
        var responses = java.util.List.of(
                new ApiResponse.Success("Hello from GPT!", "high", "detailed reasoning", 150, 75, mapper.createObjectNode()),
                new ApiResponse.Error("Not found", "404", mapper.createObjectNode()),
                new ApiResponse.Partial("Partial data", "timeout", mapper.createObjectNode())
        );
        
        var summaries = responses.stream()
                .map(response -> switch (response) {
                    case ApiResponse.Success(var text, var effort, var trace, var inputTokens, var outputTokens, var raw) -> {
                        var totalTokens = (inputTokens != null ? inputTokens : 0) + (outputTokens != null ? outputTokens : 0);
                        yield String.format("Success: %d chars, %d tokens, effort: %s", 
                            text.length(), totalTokens, effort);
                    }
                    case ApiResponse.Error(var message, var code, var raw) -> 
                        String.format("Error [%s]: %s", code, message);
                    case ApiResponse.Partial(var availableText, var reason, var raw) -> 
                        String.format("Partial (%s): %s", reason, availableText);
                })
                .toList();
        
        assertEquals("Success: 15 chars, 225 tokens, effort: high", summaries.get(0));
        assertEquals("Error [404]: Not found", summaries.get(1));
        assertEquals("Partial (timeout): Partial data", summaries.get(2));
    }

    @Test
    void shouldTestSealedInterfaceTypeGuards() {
        var responses = java.util.List.of(
                new ApiResponse.Success("Success!", "medium", "trace", 100, 50, mapper.createObjectNode()),
                new ApiResponse.Error("Not found", "404", mapper.createObjectNode()),
                new ApiResponse.Partial("Partial data", "timeout", mapper.createObjectNode())
        );
        
        var successCount = responses.stream()
                .mapToInt(response -> switch (response) {
                    case ApiResponse.Success success -> 1;
                    case ApiResponse.Error error -> 0;
                    case ApiResponse.Partial partial -> 0;
                })
                .sum();
        
        assertEquals(1, successCount);
    }

    @Test
    void shouldDemonstrateGuardedPatternMatching() {
        var longResponse = new ApiResponse.Success(
                "This is a very long response that exceeds our length limit for processing",
                "high", "complex reasoning", 200, 100, mapper.createObjectNode()
        );
        
        var shortResponse = new ApiResponse.Success(
                "Short", "low", "simple", 10, 5, mapper.createObjectNode()
        );
        
        // Pattern matching with guards (when clauses)
        String longClassification = classifyResponse(longResponse);
        String shortClassification = classifyResponse(shortResponse);
        
        assertEquals("Long successful response with high effort", longClassification);
        assertEquals("Short successful response", shortClassification);
    }
    
    private String classifyResponse(ApiResponse response) {
        return switch (response) {
            case ApiResponse.Success(var text, var effort, var trace, var input, var output, var raw) 
                when text.length() > 50 && "high".equals(effort) -> 
                    "Long successful response with high effort";
            case ApiResponse.Success(var text, var effort, var trace, var input, var output, var raw) 
                when text.length() > 50 -> 
                    "Long successful response";
            case ApiResponse.Success success -> "Short successful response";
            case ApiResponse.Error error -> "Error response";
            case ApiResponse.Partial partial -> "Partial response";
        };
    }

    @Test
    void shouldTestDefaultMethodsInSealedInterface() {
        var successResponse = new ApiResponse.Success(
                "Test content", "medium", "trace", 50, 25, mapper.createObjectNode()
        );
        var errorResponse = new ApiResponse.Error(
                "Something went wrong", "500", mapper.createObjectNode()
        );
        var partialResponse = new ApiResponse.Partial(
                "Some content", "timeout", mapper.createObjectNode()
        );
        
        // Test default methods that use pattern matching internally
        assertTrue(successResponse.isSuccess());
        assertEquals("Test content", successResponse.getTextContent());
        
        assertTrue(!errorResponse.isSuccess());
        assertEquals(null, errorResponse.getTextContent());
        
        assertTrue(!partialResponse.isSuccess());
        assertEquals("Some content", partialResponse.getTextContent());
        
        // All responses should have raw JSON
        assertThat(successResponse.getRawJson()).isNotNull();
        assertThat(errorResponse.getRawJson()).isNotNull();
        assertThat(partialResponse.getRawJson()).isNotNull();
    }

    @Test
    void shouldTestNestedSealedInterfacesInResponseSummary() {
        var service = new MyAiService(null, null);
        
        var successResponse = new ApiResponse.Success(
                "Great response!", "high", "detailed", 100, 50, mapper.createObjectNode()
        );
        
        var summary = service.summarizeResponse(successResponse);
        
        // Test nested sealed interface
        var statusType = switch (summary.status()) {
            case MyAiService.ResponseSummary.SUCCESS success -> "success";
            case MyAiService.ResponseSummary.ERROR error -> "error";
            case MyAiService.ResponseSummary.PARTIAL partial -> "partial";
        };
        
        assertEquals("success", statusType);
        assertEquals(15, summary.contentLength());  // "Great response!" length
        assertEquals(150, summary.totalTokens());   // 100 + 50
        assertEquals("high", summary.details());
    }

    @Test
    void shouldDemonstrateVarUsageForComplexTypes() {
        // Modern var usage with complex generic types
        var responseMap = java.util.Map.of(
                "success", new ApiResponse.Success("OK", "low", "trace", 10, 5, mapper.createObjectNode()),
                "error", new ApiResponse.Error("Failed", "400", mapper.createObjectNode())
        );
        
        var results = responseMap.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        java.util.Map.Entry::getKey,
                        entry -> entry.getValue().isSuccess()
                ));
        
        assertTrue(results.get("success"));
        assertTrue(!results.get("error"));
    }
}