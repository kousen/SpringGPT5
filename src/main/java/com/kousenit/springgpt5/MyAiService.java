package com.kousenit.springgpt5;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
class MyAiService {
    private final ChatClient chat;          // your normal Spring AI path
    private final Gpt5NativeClient gpt5;    // escape hatch

    MyAiService(ChatClient chatClient, Gpt5NativeClient gpt5) {
        this.chat = chatClient;
        this.gpt5 = gpt5;
    }

    public String normalAnswer(String prompt) {
        return chat.prompt(prompt).call().content();
    }

    public ApiResponse gpt5ReasoningAnswer(String prompt) throws Exception {
        return gpt5.chatWithReasoning(prompt, ReasoningEffort.MEDIUM);
    }

    public ApiResponse gpt5ReasoningAnswer(String prompt, ReasoningEffort effort) throws Exception {
        return gpt5.chatWithReasoning(prompt, effort);
    }

    public String gpt5TextAnswer(String prompt, ReasoningEffort effort) throws Exception {
        return gpt5.chatText(prompt, effort);
    }

    /**
     * Extract content safely using pattern matching
     */
    public String extractSafeContent(ApiResponse response) {
        return switch (response) {
            case ApiResponse.Success(var text, var effort, var trace, var input, var output, var raw) -> {
                System.out.printf("Success with %d input tokens, %d output tokens%n", 
                    input != null ? input : 0, 
                    output != null ? output : 0);
                yield text;
            }
            case ApiResponse.Error(var message, var code, var raw) -> {
                System.err.printf("API Error [%s]: %s%n", code, message);
                yield null;
            }
            case ApiResponse.Partial(var availableText, var reason, var raw) -> {
                System.out.printf("Partial response: %s%n", reason);
                yield availableText;
            }
        };
    }

    /**
     * Process response with detailed pattern matching
     */
    public ResponseSummary summarizeResponse(ApiResponse response) {
        var summary = switch (response) {
            case ApiResponse.Success success -> {
                var tokenCount = (success.inputTokens() != null ? success.inputTokens() : 0) + 
                               (success.outputTokens() != null ? success.outputTokens() : 0);
                yield new ResponseSummary(
                    new ResponseSummary.SUCCESS(),
                    success.text().length(),
                    tokenCount,
                    success.reasoningEffort()
                );
            }
            case ApiResponse.Error error -> new ResponseSummary(
                new ResponseSummary.ERROR(),
                0,
                0,
                error.code()
            );
            case ApiResponse.Partial partial -> new ResponseSummary(
                new ResponseSummary.PARTIAL(),
                partial.availableText().length(),
                0,
                partial.reason()
            );
        };

        return summary;
    }

    /**
     * Record for response summaries with sealed status interface
     */
    public record ResponseSummary(
        Status status,
        int contentLength,
        int totalTokens,
        String details
    ) {
        public sealed interface Status permits ResponseSummary.SUCCESS, ResponseSummary.ERROR, ResponseSummary.PARTIAL {}
        public record SUCCESS() implements Status {}
        public record ERROR() implements Status {}
        public record PARTIAL() implements Status {}
    }
}