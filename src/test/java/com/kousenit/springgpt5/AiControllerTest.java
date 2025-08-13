package com.kousenit.springgpt5;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiController.class)
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MyAiService myAiService;

    @Test
    @DisplayName("/health returns ok")
    void health() throws Exception {
        mockMvc.perform(get("/api/ai/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    @DisplayName("/chat returns text from service")
    void chat() throws Exception {
        given(myAiService.normalAnswer("hello"))
                .willReturn("world");

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prompt":"hello"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"text":"world"}
                        """));
    }

    @Test
    @DisplayName("/chat rejects blank prompt")
    void chatBlank() throws Exception {
        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prompt":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("/reason maps Success to 200")
    void reasonSuccess() throws Exception {
        var success = new ApiResponse.Success("text", "MEDIUM", "trace", 10, 5, null);
        given(myAiService.gpt5ReasoningAnswer("prompt", ReasoningEffort.MEDIUM))
                .willReturn(success);

        mockMvc.perform(post("/api/ai/reason")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prompt":"prompt","effort":"MEDIUM"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "text": "text",
                            "reasoningEffort": "MEDIUM",
                            "reasoningTrace": "trace",
                            "inputTokens": 10,
                            "outputTokens": 5
                        }
                        """));
    }

    @Test
    @DisplayName("/reason maps Partial to 206")
    void reasonPartial() throws Exception {
        var partial = new ApiResponse.Partial("partial", "why", null);
        given(myAiService.gpt5ReasoningAnswer("prompt", ReasoningEffort.LOW))
                .willReturn(partial);

        mockMvc.perform(post("/api/ai/reason")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prompt":"prompt","effort":"LOW"}
                                """))
                .andExpect(status().isPartialContent())
                .andExpect(content().json("""
                        {"availableText":"partial","reason":"why"}
                        """));
    }

    @Test
    @DisplayName("/reason maps Error to 502")
    void reasonError() throws Exception {
        var error = new ApiResponse.Error("bad", "upstream", null);
        given(myAiService.gpt5ReasoningAnswer("prompt", ReasoningEffort.HIGH))
                .willReturn(error);

        mockMvc.perform(post("/api/ai/reason")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prompt":"prompt","effort":"HIGH"}
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(content().json("""
                        {"message":"bad","code":"upstream"}
                        """));
    }

    @Test
    @DisplayName("/reason/text returns text only")
    void reasonText() throws Exception {
        given(myAiService.gpt5TextAnswer("hello", ReasoningEffort.MEDIUM))
                .willReturn("ok");
        mockMvc.perform(post("/api/ai/reason/text")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prompt":"hello","effort":"MEDIUM"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"text":"ok"}
                        """));
    }

    @Test
    @DisplayName("Exception from service -> 502 with body")
    void exceptionHandler() throws Exception {
        given(myAiService.gpt5ReasoningAnswer(any(), any()))
                .willThrow(new OpenAiClientException("boom"));
        mockMvc.perform(post("/api/ai/reason")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prompt":"x"}
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(content().json("""
                        {"message":"boom","code":"upstream_error"}
                        """));
    }
}
