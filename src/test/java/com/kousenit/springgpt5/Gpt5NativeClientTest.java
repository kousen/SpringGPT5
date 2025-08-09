package com.kousenit.springgpt5;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class Gpt5NativeClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldExtractTextFromDirectOutputText() throws Exception {
        String json = """
                {
                  "output_text": "This is the direct response"
                }
                """;
        JsonNode node = mapper.readTree(json);
        String result = Gpt5NativeClient.extractText(node);
        assertEquals("This is the direct response", result);
    }

    @Test
    void shouldExtractTextFromOutputArray() throws Exception {
        String json = """
                {
                  "output": [
                    {
                      "type": "message",
                      "content": [
                        {
                          "type": "output_text",
                          "text": "Response from output array"
                        }
                      ]
                    }
                  ]
                }
                """;
        JsonNode node = mapper.readTree(json);
        String result = Gpt5NativeClient.extractText(node);
        assertEquals("Response from output array", result);
    }

    @Test
    void shouldExtractTextFromMultipleContentItems() throws Exception {
        String json = """
                {
                  "output": [
                    {
                      "type": "message",
                      "content": [
                        {
                          "type": "output_text",
                          "text": "First part"
                        },
                        {
                          "type": "output_text",
                          "text": " Second part"
                        }
                      ]
                    }
                  ]
                }
                """;
        JsonNode node = mapper.readTree(json);
        String result = Gpt5NativeClient.extractText(node);
        assertEquals("First part Second part", result);
    }

    @Test
    void shouldReturnNullForEmptyResponse() throws Exception {
        String json = """
                {
                  "status": "completed"
                }
                """;
        JsonNode node = mapper.readTree(json);
        String result = Gpt5NativeClient.extractText(node);
        assertNull(result);
    }

    @Test
    void shouldUseFallbackPaths() throws Exception {
        String json = """
                {
                  "output": [
                    {
                      "content": [
                        {
                          "text": "Fallback text"
                        }
                      ]
                    }
                  ]
                }
                """;
        JsonNode node = mapper.readTree(json);
        String result = Gpt5NativeClient.extractText(node);
        assertEquals("Fallback text", result);
    }

    @Test
    void shouldTestResultRecord() {
        JsonNode rawNode = mapper.createObjectNode();
        Gpt5NativeClient.Result result = new Gpt5NativeClient.Result(
                "test response",
                "medium",
                "reasoning trace here",
                100,
                50,
                rawNode
        );

        assertEquals("test response", result.text());
        assertEquals("medium", result.reasoningEffort());
        assertEquals("reasoning trace here", result.reasoningTrace());
        assertEquals(Integer.valueOf(100), result.inputTokens());
        assertEquals(Integer.valueOf(50), result.outputTokens());
        assertEquals(rawNode, result.raw());
    }

    @Test
    void shouldTestResultRecordWithNulls() {
        Gpt5NativeClient.Result result = new Gpt5NativeClient.Result(
                null, null, null, null, null, null
        );

        assertNull(result.text());
        assertNull(result.reasoningEffort());
        assertNull(result.reasoningTrace());
        assertNull(result.inputTokens());
        assertNull(result.outputTokens());
        assertNull(result.raw());
    }

    @Test
    void shouldTestResultRecordEquality() {
        JsonNode rawNode = mapper.createObjectNode();
        Gpt5NativeClient.Result result1 = new Gpt5NativeClient.Result(
                "test", "medium", "trace", 100, 50, rawNode
        );
        Gpt5NativeClient.Result result2 = new Gpt5NativeClient.Result(
                "test", "medium", "trace", 100, 50, rawNode
        );

        assertEquals(result1, result2);
        assertEquals(result1.hashCode(), result2.hashCode());
        assertThat(result1.toString()).contains("test", "medium", "trace");
    }
}