package com.kousenit.springgpt5;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
class AiController {
    private final MyAiService service;

    AiController(MyAiService service) {
        this.service = service;
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody ChatRequest request) {
        if (request == null || request.prompt() == null || request.prompt().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("prompt must not be blank", "validation_error"));
        }
        var text = service.normalAnswer(request.prompt());
        return ResponseEntity.ok(new ChatResponse(text));
    }

    @PostMapping("/reason")
    public ResponseEntity<?> reason(@RequestBody ReasonRequest request) throws OpenAiClientException {
        if (request == null || request.prompt() == null || request.prompt().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("prompt must not be blank", "validation_error"));
        }
        var effort = request.effort() != null ? request.effort() : ReasoningEffort.MEDIUM;
        var response = service.gpt5ReasoningAnswer(request.prompt(), effort);

        return switch (response) {
            case ApiResponse.Success s -> ResponseEntity.ok(
                    new ReasonSuccess(s.text(), s.reasoningEffort(), s.reasoningTrace(), s.inputTokens(), s.outputTokens())
            );
            case ApiResponse.Partial p -> ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(
                    new ReasonPartial(p.availableText(), p.reason())
            );
            case ApiResponse.Error e -> ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                    new ErrorResponse(e.message(), e.code())
            );
        };
    }

    @PostMapping("/reason/text")
    public ResponseEntity<?> reasonText(@RequestBody ReasonRequest request) throws OpenAiClientException {
        if (request == null || request.prompt() == null || request.prompt().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("prompt must not be blank", "validation_error"));
        }
        var effort = request.effort() != null ? request.effort() : ReasoningEffort.MEDIUM;
        var text = service.gpt5TextAnswer(request.prompt(), effort);
        return ResponseEntity.ok(new ChatResponse(text));
    }

    @GetMapping("/health")
    public String health() {
        return "ok";
    }

    public record ChatRequest(String prompt) {}
    public record ReasonRequest(String prompt, ReasoningEffort effort) {}
    public record ChatResponse(String text) {}
    public record ReasonSuccess(String text, String reasoningEffort, String reasoningTrace, Integer inputTokens, Integer outputTokens) {}
    public record ReasonPartial(String availableText, String reason) {}
    public record ErrorResponse(String message, String code) {}

    @ExceptionHandler(OpenAiClientException.class)
    public ResponseEntity<ErrorResponse> handleClient(OpenAiClientException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new ErrorResponse(ex.getMessage(), "upstream_error"));
    }
}
