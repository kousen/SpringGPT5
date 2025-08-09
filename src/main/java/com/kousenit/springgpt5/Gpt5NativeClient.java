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
    public ApiResponse chatWithReasoning(String userPrompt, ReasoningEffort effort) throws OpenAiClientException {
        var messages = List.of(Map.of("role", "user", "content", userPrompt));
        return send(messages, effort);
    }

    /**
     * Multi-message variant (roles already user/system/assistant).
     */
    public ApiResponse send(List<Map<String, String>> messages, ReasoningEffort effort) throws OpenAiClientException {
        try {
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
        } catch (Exception e) {
            throw new OpenAiClientException("Failed to send request to OpenAI API", e);
        }
    }

    // ----- tree helpers -----
    private static Optional<String> firstText(JsonNode root, String... pointers) {
        return Arrays.stream(pointers)
                .map(root::at)
                .filter(n -> n != null && !n.isMissingNode() && !n.isNull())
                .findFirst()
                .map(JsonNode::asText);
    }

    private static Optional<JsonNode> firstNode(JsonNode root, String... pointers) {
        return Arrays.stream(pointers)
                .map(root::at)
                .filter(n -> n != null && !n.isMissingNode() && !n.isNull())
                .findFirst();
    }

    private static Optional<String> text(JsonNode node, String pointer) {
        return Optional.ofNullable(node)
                .map(n -> n.at(pointer))
                .filter(n -> !n.isMissingNode() && !n.isNull())
                .map(JsonNode::asText);
    }

    private static Optional<Integer> intValue(JsonNode root, String pointer) {
        return Optional.ofNullable(root.at(pointer))
                .filter(n -> !n.isMissingNode() && !n.isNull())
                .map(JsonNode::asInt);
    }

    /** Convenience call: send prompt, return just the text (null if none). */
    public String chatText(String userPrompt, ReasoningEffort effort) throws OpenAiClientException {
        var response = chatWithReasoning(userPrompt, effort);
        return switch (response) {
            case ApiResponse.Success success -> success.text();
            case ApiResponse.Error ignored -> null;
            case ApiResponse.Partial partial -> partial.availableText();
        };
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

        // Extract text using pattern matching
        var text = extractText(resp);
        
        if (text == null || text.isEmpty()) {
            return new ApiResponse.Partial("", "No text content available", resp);
        }

        // Extract metadata using Optional chain with meaningful defaults
        var reasoning = firstNode(resp, "/reasoning", "/meta/reasoning");
        var reasoningEffort = reasoning.flatMap(r -> text(r, "/effort")).orElse("unknown");
        var reasoningTrace = reasoning.flatMap(r -> text(r, "/trace")).orElse("");
        
        // Keep as Integer for record compatibility, but use 0 as default instead of null
        var inputTokens = intValue(resp, "/usage/input_tokens").orElse(0);
        var outputTokens = intValue(resp, "/usage/output_tokens").orElse(0);

        return new ApiResponse.Success(text, reasoningEffort, reasoningTrace, inputTokens, outputTokens, resp);
    }

    /**
     * Extract text from API response using pattern matching for switch
     */
    static String extractText(JsonNode resp) {
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
        ).orElse(null);
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

}