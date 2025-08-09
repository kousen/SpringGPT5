package com.kousenit.springgpt5;

public enum ReasoningEffort {
    MINIMAL("minimal"),
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high");

    private final String value;

    ReasoningEffort(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}