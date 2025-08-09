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

    public ApiResponse gpt5ReasoningAnswer(String prompt) throws OpenAiClientException {
        return gpt5.chatWithReasoning(prompt, ReasoningEffort.MEDIUM);
    }

    public ApiResponse gpt5ReasoningAnswer(String prompt, ReasoningEffort effort) throws OpenAiClientException {
        return gpt5.chatWithReasoning(prompt, effort);
    }

    public String gpt5TextAnswer(String prompt, ReasoningEffort effort) throws OpenAiClientException {
        return gpt5.chatText(prompt, effort);
    }


    /**
     * Process response with detailed pattern matching
     */
    public ResponseSummary summarizeResponse(ApiResponse response) {

        return switch (response) {
            case ApiResponse.Success success -> {
                // Handle potential null values for backwards compatibility
                var inputTokens = success.inputTokens() != null ? success.inputTokens() : 0;
                var outputTokens = success.outputTokens() != null ? success.outputTokens() : 0;
                var tokenCount = inputTokens + outputTokens;
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