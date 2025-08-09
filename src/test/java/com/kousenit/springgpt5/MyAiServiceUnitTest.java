package com.kousenit.springgpt5;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyAiServiceUnitTest {
    @Mock
    private ChatClient chatClient;

    @Mock
    private Gpt5NativeClient gpt5NativeClient;

    private MyAiService service;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new MyAiService(chatClient, gpt5NativeClient);
    }

    @Test
    void shouldCallGpt5ReasoningAnswerWithDefaultEffort() throws Exception {
        // Given
        var expectedResponse = new ApiResponse.Success(
                "Data-oriented programming focuses on organizing code around data structures rather than objects.",
                "medium",
                "reasoning trace",
                100,
                75,
                mapper.createObjectNode()
        );
        when(gpt5NativeClient.chatWithReasoning("Explain DOP", ReasoningEffort.MEDIUM))
                .thenReturn(expectedResponse);

        // When
        ApiResponse result = service.gpt5ReasoningAnswer("Explain DOP");

        // Then
        assertNotNull(result);
        assertThat(result).isInstanceOf(ApiResponse.Success.class);
        if (result instanceof ApiResponse.Success success) {
            assertThat(success.text()).contains("Data-oriented programming");
            assertEquals("medium", success.reasoningEffort());
            assertEquals(Integer.valueOf(100), success.inputTokens());
            assertEquals(Integer.valueOf(75), success.outputTokens());
        }
    }

    @Test
    void shouldCallGpt5ReasoningAnswerWithCustomEffort() throws Exception {
        // Given
        var expectedResponse = new ApiResponse.Success(
                "Brief explanation of DOP",
                "low",
                "minimal reasoning",
                50,
                25,
                mapper.createObjectNode()
        );
        when(gpt5NativeClient.chatWithReasoning("Brief DOP explanation", ReasoningEffort.LOW))
                .thenReturn(expectedResponse);

        // When
        ApiResponse result = service.gpt5ReasoningAnswer("Brief DOP explanation", ReasoningEffort.LOW);

        // Then
        assertNotNull(result);
        assertThat(result).isInstanceOf(ApiResponse.Success.class);
        if (result instanceof ApiResponse.Success success) {
            assertThat(success.text()).contains("Brief explanation");
            assertEquals("low", success.reasoningEffort());
        }
    }

    @Test
    void shouldCallGpt5TextAnswer() throws Exception {
        // Given
        when(gpt5NativeClient.chatText("Why use Java for AI?", ReasoningEffort.HIGH))
                .thenReturn("Java provides strong typing, excellent performance, and rich ecosystem for AI applications.");

        // When
        String result = service.gpt5TextAnswer("Why use Java for AI?", ReasoningEffort.HIGH);

        // Then
        assertNotNull(result);
        assertThat(result)
                .contains("Java")
                .contains("AI");
    }

    @Test
    void shouldHandleErrorResponseInReasoningAnswer() throws Exception {
        // Given
        var errorResponse = new ApiResponse.Error(
                "API limit exceeded",
                "rate_limit",
                mapper.createObjectNode()
        );
        when(gpt5NativeClient.chatWithReasoning("test", ReasoningEffort.LOW))
                .thenReturn(errorResponse);

        // When
        ApiResponse result = service.gpt5ReasoningAnswer("test", ReasoningEffort.LOW);

        // Then
        assertThat(result).isInstanceOf(ApiResponse.Error.class);
        if (result instanceof ApiResponse.Error error) {
            assertEquals("API limit exceeded", error.message());
            assertEquals("rate_limit", error.code());
        }
    }

    @Test
    void shouldHandlePartialResponseInReasoningAnswer() throws Exception {
        // Given
        var partialResponse = new ApiResponse.Partial(
                "Incomplete response due to...",
                "timeout",
                mapper.createObjectNode()
        );
        when(gpt5NativeClient.chatWithReasoning(any(), eq(ReasoningEffort.MEDIUM)))
                .thenReturn(partialResponse);

        // When
        ApiResponse result = service.gpt5ReasoningAnswer("complex question");

        // Then
        assertThat(result).isInstanceOf(ApiResponse.Partial.class);
        if (result instanceof ApiResponse.Partial partial) {
            assertThat(partial.availableText()).startsWith("Incomplete response");
            assertEquals("timeout", partial.reason());
        }
    }

    @Test
    void shouldSummarizeSuccessResponse() {
        // Given
        var successResponse = new ApiResponse.Success(
                "This is a successful response with detailed information.",
                "high",
                "detailed reasoning trace",
                150,
                200,
                mapper.createObjectNode()
        );

        // When
        MyAiService.ResponseSummary summary = service.summarizeResponse(successResponse);

        // Then
        assertThat(summary.status()).isInstanceOf(MyAiService.ResponseSummary.SUCCESS.class);
        assertEquals(56, summary.contentLength()); // Length of "This is a successful response with detailed information."
        assertEquals(350, summary.totalTokens()); // 150 + 200
        assertEquals("high", summary.details());
    }

    @Test
    void shouldSummarizeSuccessResponseWithNullTokens() {
        // Given
        var successResponse = new ApiResponse.Success(
                "Response text",
                "medium",
                "trace",
                null, // null input tokens
                100,  // only output tokens
                mapper.createObjectNode()
        );

        // When
        MyAiService.ResponseSummary summary = service.summarizeResponse(successResponse);

        // Then
        assertThat(summary.status()).isInstanceOf(MyAiService.ResponseSummary.SUCCESS.class);
        assertEquals(13, summary.contentLength());
        assertEquals(100, summary.totalTokens()); // 0 + 100
        assertEquals("medium", summary.details());
    }

    @Test
    void shouldSummarizeErrorResponse() {
        // Given
        var errorResponse = new ApiResponse.Error(
                "Authentication failed",
                "auth_error",
                mapper.createObjectNode()
        );

        // When
        MyAiService.ResponseSummary summary = service.summarizeResponse(errorResponse);

        // Then
        assertThat(summary.status()).isInstanceOf(MyAiService.ResponseSummary.ERROR.class);
        assertEquals(0, summary.contentLength());
        assertEquals(0, summary.totalTokens());
        assertEquals("auth_error", summary.details());
    }

    @Test
    void shouldSummarizePartialResponse() {
        // Given
        var partialResponse = new ApiResponse.Partial(
                "Partial content available here",
                "request_timeout",
                mapper.createObjectNode()
        );

        // When
        MyAiService.ResponseSummary summary = service.summarizeResponse(partialResponse);

        // Then
        assertThat(summary.status()).isInstanceOf(MyAiService.ResponseSummary.PARTIAL.class);
        assertEquals(30, summary.contentLength()); // Length of "Partial content available here"
        assertEquals(0, summary.totalTokens());
        assertEquals("request_timeout", summary.details());
    }

    @Test
    void shouldTestResponseSummaryRecordFeatures() {
        // Test record equals, hashCode, and toString for same types
        var status1 = new MyAiService.ResponseSummary.SUCCESS();
        var status2 = new MyAiService.ResponseSummary.SUCCESS();

        assertEquals(status1, status2);
        assertEquals(status1.hashCode(), status2.hashCode());

        // Test ResponseSummary record functionality
        var summary1 = new MyAiService.ResponseSummary(status1, 100, 50, "test");
        var summary2 = new MyAiService.ResponseSummary(status2, 100, 50, "test");
        var summary3 = new MyAiService.ResponseSummary(status1, 200, 50, "test");

        assertEquals(summary1, summary2);
        assertNotEquals(summary1, summary3);
        assertThat(summary1.toString()).contains("100", "50", "test");

        // Test that different status types exist and can be instantiated
        var errorStatus = new MyAiService.ResponseSummary.ERROR();
        var partialStatus = new MyAiService.ResponseSummary.PARTIAL();
        assertThat(errorStatus).isNotNull();
        assertThat(partialStatus).isNotNull();
    }
}