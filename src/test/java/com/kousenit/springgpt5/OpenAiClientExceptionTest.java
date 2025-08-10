package com.kousenit.springgpt5;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiClientExceptionTest {

    @Test
    void shouldCreateExceptionWithMessage() {
        String message = "API request failed";
        
        OpenAiClientException exception = new OpenAiClientException(message);
        
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldCreateExceptionWithMessageAndCause() {
        String message = "API request failed";
        RuntimeException cause = new RuntimeException("Network error");
        
        OpenAiClientException exception = new OpenAiClientException(message, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}