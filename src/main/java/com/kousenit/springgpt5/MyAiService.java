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

    public Gpt5NativeClient.Result gpt5ReasoningAnswer(String prompt) throws Exception {
        return gpt5.chatWithReasoning(prompt, ReasoningEffort.MEDIUM);
    }

    public Gpt5NativeClient.Result gpt5ReasoningAnswer(String prompt, ReasoningEffort effort) throws Exception {
        return gpt5.chatWithReasoning(prompt, effort);
    }

    public String gpt5TextAnswer(String prompt, ReasoningEffort effort) throws Exception {
        return gpt5.chatText(prompt, effort);
    }
}