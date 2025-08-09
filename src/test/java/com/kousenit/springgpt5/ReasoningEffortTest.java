package com.kousenit.springgpt5;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ReasoningEffortTest {

    @Test
    void shouldReturnCorrectValues() {
        assertEquals("minimal", ReasoningEffort.MINIMAL.getValue());
        assertEquals("low", ReasoningEffort.LOW.getValue());
        assertEquals("medium", ReasoningEffort.MEDIUM.getValue());
        assertEquals("high", ReasoningEffort.HIGH.getValue());
    }

    @Test
    void shouldHaveAllExpectedValues() {
        ReasoningEffort[] values = ReasoningEffort.values();
        assertThat(values).hasSize(4);
        assertThat(values).containsExactly(
                ReasoningEffort.MINIMAL,
                ReasoningEffort.LOW,
                ReasoningEffort.MEDIUM,
                ReasoningEffort.HIGH
        );
    }

    @Test
    void shouldSupportValueOf() {
        assertEquals(ReasoningEffort.MINIMAL, ReasoningEffort.valueOf("MINIMAL"));
        assertEquals(ReasoningEffort.LOW, ReasoningEffort.valueOf("LOW"));
        assertEquals(ReasoningEffort.MEDIUM, ReasoningEffort.valueOf("MEDIUM"));
        assertEquals(ReasoningEffort.HIGH, ReasoningEffort.valueOf("HIGH"));
    }
}