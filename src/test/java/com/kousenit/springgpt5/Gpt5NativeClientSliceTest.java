package com.kousenit.springgpt5;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Tests for Gpt5NativeClient using Spring Boot's @RestClientTest slice and MockRestServiceServer.
 * This keeps the existing WireMock tests intact while showing the Spring-native alternative.
 */
@RestClientTest(Gpt5NativeClient.class)
class Gpt5NativeClientSliceTest {

    @Autowired
    private MockRestServiceServer server;
    
    @Autowired
    private Gpt5NativeClient client;

    @AfterEach
    void verifyServer() {
        server.verify();
    }

    @Test
    void shouldTestChatWithReasoningSuccess_withMockServer() throws Exception {
        String responseJson = """
            {
              "output": [
                {
                  "type": "message",
                  "content": [
                    { "type": "text", "text": "This is a test response" }
                  ]
                }
              ],
              "reasoning": { "effort": "medium", "trace": "thinking step by step" },
              "usage": { "input_tokens": 10, "output_tokens": 20 }
            }
            """;

        server.expect(requestTo(org.hamcrest.Matchers.containsString("/v1/responses")))
              .andExpect(method(HttpMethod.POST))
              .andExpect(header("Authorization", "Bearer test-key"))
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$.model").value("gpt-5-nano"))
              .andExpect(jsonPath("$.reasoning.effort").value("medium"))
              .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        ApiResponse result = client.chatWithReasoning("test prompt", ReasoningEffort.MEDIUM);
        ApiResponse.Success success = assertInstanceOf(ApiResponse.Success.class, result);
        assertEquals("This is a test response", success.text());
        assertEquals("medium", success.reasoningEffort());
        assertEquals("thinking step by step", success.reasoningTrace());
        assertEquals(Integer.valueOf(10), success.inputTokens());
        assertEquals(Integer.valueOf(20), success.outputTokens());
    }

    @Test
    void shouldThrowOpenAiClientExceptionOnHttpError_withMockServer() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/v1/responses")))
              .andExpect(method(HttpMethod.POST))
              .andRespond(withServerError());

        OpenAiClientException ex = assertThrows(OpenAiClientException.class,
                () -> client.chatWithReasoning("test", ReasoningEffort.MEDIUM));
        assertEquals("Failed to send request to OpenAI API", ex.getMessage());
    }

    @TestConfiguration
    static class TestConfig {
        // Provide a RestClient bean matching what Gpt5NativeClient expects.
        // MockRestServiceServer will intercept any HTTP calls from this client;
        // the exact host is irrelevant, but we keep the same base path structure.
        @Bean
        @Primary
        RestClient openAiRestClient(RestClient.Builder builder) {
            // Use the slice-provided Builder so MockRestServiceServer can bind and intercept
            return builder
                    .baseUrl("http://localhost:8080/v1")
                    .defaultHeader("Authorization", "Bearer test-key")
                    .build();
        }
    }
}
