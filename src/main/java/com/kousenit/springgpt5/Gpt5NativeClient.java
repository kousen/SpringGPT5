package com.kousenit.springgpt5;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class Gpt5NativeClient {
    private final RestClient rc;      // points at /v1
    private final ObjectMapper mapper;
    private final String model;

    public Gpt5NativeClient(RestClient openAiResponses, ObjectMapper mapper, 
                           @Value("${spring.ai.openai.chat.options.model:gpt-5-nano}") String model) {
        this.rc = openAiResponses;
        this.mapper = mapper;
        this.model = model;
    }

    /**
     * Quick single-message call with a reasoning knob.
     */
    public Result chatWithReasoning(String userPrompt, ReasoningEffort effort) throws Exception {
        var messages = List.of(Map.of("role", "user", "content", userPrompt));
        return send(messages, effort);
    }

    /**
     * Multi-message variant (roles already user/system/assistant).
     */
    public Result send(List<Map<String, String>> messages, ReasoningEffort effort) throws Exception {
        // Build dynamic bits with Jackson so newlines/quotes are escaped
        String messagesJson = mapper.writeValueAsString(messages);
        String effortJson = mapper.writeValueAsString(effort.getValue());

        // Readable request template; plug in the JSON fragments safely with %s
        String body = """
                {
                  "model": "%s",
                  "input": %s,
                  "reasoning": { "effort": %s }
                }
                """.formatted(model, messagesJson, effortJson);

        JsonNode resp = rc.post()
                .uri("/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        // Prefer output_text when available; otherwise fall back to common shapes
        assert resp != null;
        String text = extractText(resp);

        JsonNode reasoning = firstNode(resp, "/reasoning", "/meta/reasoning");
        String reasoningEffort = text(reasoning, "/effort");
        String reasoningTrace = text(reasoning, "/trace");

        // (Optional) common metrics if present
        Integer inputTokens = intOrNull(resp, "/usage/input_tokens");
        Integer outputTokens = intOrNull(resp, "/usage/output_tokens");

        return new Result(text, reasoningEffort, reasoningTrace, inputTokens, outputTokens, resp);
    }

    // ----- tiny tree helpers -----
    private static String firstText(JsonNode root, String... pointers) {
        return Arrays.stream(pointers)
                .map(p -> root.at(p))
                .filter(n -> n != null && !n.isMissingNode() && !n.isNull())
                .findFirst()
                .map(n -> n.asText())
                .orElse(null);
    }

    private static JsonNode firstNode(JsonNode root, String... pointers) {
        return Arrays.stream(pointers)
                .map(p -> root.at(p))
                .filter(n -> n != null && !n.isMissingNode() && !n.isNull())
                .findFirst()
                .orElse(null);
    }

    private static String text(JsonNode node, String pointer) {
        return Optional.ofNullable(node)
                .map(n -> n.at(pointer))
                .filter(n -> !n.isMissingNode() && !n.isNull())
                .map(n -> n.asText())
                .orElse(null);
    }

    private static Integer intOrNull(JsonNode root, String pointer) {
        return Optional.ofNullable(root.at(pointer))
                .filter(n -> !n.isMissingNode() && !n.isNull())
                .map(n -> n.asInt())
                .orElse(null);
    }

    /** Return the assistant's plain text, if any, from a Responses API payload. */
    static String extractText(JsonNode resp) {
        // 1) Fast path if OpenAI ever includes a top-level output_text
        return Optional.ofNullable(resp.get("output_text"))
                .filter(node -> !node.isNull())
                .map(JsonNode::asText)
                .or(() -> extractFromOutputArray(resp))
                .or(() -> extractFromFallbackPaths(resp))
                .orElse(null);
    }

    private static Optional<String> extractFromOutputArray(JsonNode resp) {
        return Optional.ofNullable(resp.get("output"))
                .filter(JsonNode::isArray)
                .map(outputArray -> {
                    StringBuilder sb = new StringBuilder();
                    outputArray.forEach(item -> {
                        String itemType = item.path("type").asText();
                        if ("message".equals(itemType)) {
                            item.path("content").forEach(content -> {
                                String contentType = content.path("type").asText();
                                if ("output_text".equals(contentType)) {
                                    String text = content.path("text").asText(null);
                                    if (text != null) sb.append(text);
                                }
                            });
                        }
                    });
                    return sb.isEmpty() ? null : sb.toString();
                })
                .filter(Objects::nonNull);
    }

    private static Optional<String> extractFromFallbackPaths(JsonNode resp) {
        return Optional.ofNullable(firstText(resp,
                "/output/0/content/0/text",
                "/response/0/content/0/text"
        ));
    }

    /** Convenience call: send prompt, return just the text (null if none). */
    public String chatText(String userPrompt, ReasoningEffort effort) throws Exception {
        var res = chatWithReasoning(userPrompt, effort);
        return res.text(); // now filled via extractText(...)
    }

    /**
     * Modern pattern matching approach returning sealed ApiResponse types
     */
    public ApiResponse chatWithReasoningModern(String userPrompt, ReasoningEffort effort) throws Exception {
        var messages = List.of(Map.of("role", "user", "content", userPrompt));
        return sendModern(messages, effort);
    }

    /**
     * Enhanced send method using pattern matching and sealed interfaces
     */
    public ApiResponse sendModern(List<Map<String, String>> messages, ReasoningEffort effort) throws Exception {
        var messagesJson = mapper.writeValueAsString(messages);
        var effortJson = mapper.writeValueAsString(effort.getValue());

        var body = """
                {
                  "model": "%s",
                  "input": %s,
                  "reasoning": { "effort": %s }
                }
                """.formatted(model, messagesJson, effortJson);

        var resp = rc.post()
                .uri("/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        return parseApiResponse(resp);
    }

    /**
     * Parse API response using advanced pattern matching features
     */
    private ApiResponse parseApiResponse(JsonNode resp) {
        // Check for error first using pattern matching with instanceof
        if (resp instanceof JsonNode errorNode && errorNode.has("error")) {
            var errorInfo = errorNode.get("error");
            var message = errorInfo.path("message").asText("Unknown error");
            var code = errorInfo.path("code").asText("unknown");
            return new ApiResponse.Error(message, code, resp);
        }

        // Extract text using our existing method
        var text = extractTextModern(resp);
        
        if (text == null || text.isEmpty()) {
            return new ApiResponse.Partial("", "No text content available", resp);
        }

        // Extract metadata using pattern matching
        var reasoning = firstNode(resp, "/reasoning", "/meta/reasoning");
        var reasoningEffort = reasoning != null ? text(reasoning, "/effort") : null;
        var reasoningTrace = reasoning != null ? text(reasoning, "/trace") : null;
        
        var inputTokens = intOrNull(resp, "/usage/input_tokens");
        var outputTokens = intOrNull(resp, "/usage/output_tokens");

        return new ApiResponse.Success(text, reasoningEffort, reasoningTrace, inputTokens, outputTokens, resp);
    }

    /**
     * Modern text extraction using pattern matching for switch
     */
    private static String extractTextModern(JsonNode resp) {
        // Fast path with pattern matching instanceof
        if (resp instanceof JsonNode directNode && directNode.has("output_text")) {
            var outputText = directNode.get("output_text");
            if (!outputText.isNull()) return outputText.asText();
        }

        // Process output array with enhanced pattern matching
        var outputNode = resp.get("output");
        if (outputNode instanceof JsonNode output && output.isArray()) {
            var sb = new StringBuilder();
            
            output.forEach(item -> {
                var itemType = item.path("type").asText();
                
                // Pattern matching for switch on item type
                switch (itemType) {
                    case "message" -> processMessageContent(item, sb);
                    case "function_call" -> processFunctionCall(item, sb);
                    case "error" -> { /* Skip errors in partial responses */ }
                    case null, default -> { /* Ignore unknown types */ }
                }
            });
            
            if (!sb.isEmpty()) return sb.toString();
        }

        // Fallback using existing method
        return firstText(resp,
                "/output/0/content/0/text",
                "/response/0/content/0/text"
        );
    }

    /**
     * Process message content using pattern matching
     */
    private static void processMessageContent(JsonNode item, StringBuilder sb) {
        var contentArray = item.path("content");
        if (contentArray instanceof JsonNode content && content.isArray()) {
            content.forEach(contentItem -> {
                var contentType = contentItem.path("type").asText();
                
                switch (contentType) {
                    case "output_text", "text" -> {
                        var text = contentItem.path("text").asText(null);
                        if (text != null && !text.isBlank()) {
                            sb.append(text);
                        }
                    }
                    case "markdown" -> {
                        var markdown = contentItem.path("content").asText(null);
                        if (markdown != null && !markdown.isBlank()) {
                            sb.append(markdown);
                        }
                    }
                    case null, default -> { /* Ignore unknown content types */ }
                }
            });
        }
    }

    /**
     * Process function calls (placeholder for future extension)
     */
    private static void processFunctionCall(JsonNode item, StringBuilder sb) {
        // Future: Could extract function call results
        var functionName = item.path("function").path("name").asText();
        if (!functionName.isEmpty()) {
            sb.append("[Function: ").append(functionName).append("] ");
        }
    }

    /**
     * Result holder that keeps raw JSON for anything new the API adds.
     * 
     * @deprecated Use {@link ApiResponse} sealed interface for better type safety
     */
    @Deprecated(since = "1.0", forRemoval = false)
    public record Result(
            String text,
            String reasoningEffort,
            String reasoningTrace,
            Integer inputTokens,
            Integer outputTokens,
            JsonNode raw
    ) {
        /**
         * Convert to modern ApiResponse using record pattern matching
         */
        public ApiResponse toApiResponse() {
            return switch (this) {
                case Result(var text, var effort, var trace, var input, var output, var raw) 
                    when text != null && !text.isEmpty() -> 
                        new ApiResponse.Success(text, effort, trace, input, output, raw);
                case Result(var text, var effort, var trace, var input, var output, var raw) -> 
                        new ApiResponse.Partial(text != null ? text : "", "Incomplete result", raw);
            };
        }
    }
}