package com.kousenit.springgpt5;

/**
 * Exception thrown when OpenAI API operations fail
 */
public class OpenAiClientException extends Exception {
    
    public OpenAiClientException(String message) {
        super(message);
    }
    
    public OpenAiClientException(String message, Throwable cause) {
        super(message, cause);
    }
}