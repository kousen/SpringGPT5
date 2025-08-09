package com.kousenit.springgpt5;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Sealed interface representing different types of API responses from OpenAI.
 * This provides exhaustive pattern matching and type safety for response handling.
 */
public sealed interface ApiResponse 
        permits ApiResponse.Success, ApiResponse.Error, ApiResponse.Partial {

    /**
     * Successful response containing the expected data
     */
    record Success(
            String text,
            String reasoningEffort,
            String reasoningTrace,
            Integer inputTokens,
            Integer outputTokens,
            JsonNode raw
    ) implements ApiResponse {}

    /**
     * Error response when the API call fails
     */
    record Error(
            String message,
            String code,
            JsonNode raw
    ) implements ApiResponse {}

    /**
     * Partial response when some data is available but incomplete
     */
    record Partial(
            String availableText,
            String reason,
            JsonNode raw
    ) implements ApiResponse {}

    /**
     * Extract the raw JSON from any response type using pattern matching
     */
    default JsonNode getRawJson() {
        return switch (this) {
            case Success(var text, var effort, var trace, var input, var output, var raw) -> raw;
            case Error(var message, var code, var raw) -> raw;
            case Partial(var availableText, var reason, var raw) -> raw;
        };
    }

    /**
     * Check if the response represents a successful operation
     */
    default boolean isSuccess() {
        return this instanceof Success;
    }

    /**
     * Get text content if available, using pattern matching
     */
    default String getTextContent() {
        return switch (this) {
            case Success(var text, var effort, var trace, var input, var output, var raw) -> text;
            case Error(var message, var code, var raw) -> null;
            case Partial(var availableText, var reason, var raw) -> availableText;
        };
    }
}