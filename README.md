# SpringGPT5

A Spring Boot application demonstrating integration with OpenAI's GPT-5 model using both Spring AI framework and native OpenAI API calls.

## Features

- **Dual Integration Approach**: Uses both Spring AI's `ChatClient` and direct OpenAI API calls
- **GPT-5 Reasoning API**: Direct access to OpenAI's reasoning capabilities with configurable effort levels
- **Type-Safe Configuration**: Uses `ReasoningEffort` enum for parameter validation
- **Comprehensive Testing**: Integration tests for both Spring AI and native client approaches
- **Modern Java**: Built with Java 21 and Spring Boot 3.5.4

## Architecture

### Components

- **`MyAiService`**: Main service providing both Spring AI and native GPT-5 integration
- **`Gpt5NativeClient`**: Direct OpenAI API client with reasoning support
- **`AiClientsConfig`**: Configuration for Spring AI and REST clients
- **`ReasoningEffort`**: Type-safe enum for reasoning effort levels (`MINIMAL`, `LOW`, `MEDIUM`, `HIGH`)

### Integration Approaches

1. **Spring AI Path**: Standard Spring AI integration using `ChatClient`
2. **Native API Path**: Direct OpenAI API calls with reasoning capabilities

## Prerequisites

- Java 21
- OpenAI API key with GPT-5 access
- Gradle 8.x

## Setup

1. Clone the repository
2. Set your OpenAI API key:
   ```bash
   export OPENAI_API_KEY=your_api_key_here
   ```
   Or add it to `application.properties`:
   ```properties
   openai.api.key=your_api_key_here
   ```

3. Run the application:
   ```bash
   ./gradlew bootRun
   ```

## Usage

### Spring AI Integration
```java
@Autowired
private MyAiService service;

String response = service.normalAnswer("Explain quantum computing");
```

### GPT-5 Reasoning API
```java
// With default medium effort
ApiResponse response = service.gpt5ReasoningAnswer(
    "Explain the benefits of functional programming"
);

// With custom effort level
ApiResponse response = service.gpt5ReasoningAnswer(
    "Solve this complex problem", 
    ReasoningEffort.HIGH
);

// Pattern matching for safe access
switch (response) {
    case ApiResponse.Success(var text, var effort, var trace, var input, var output, var raw) -> {
        System.out.println("Response: " + text);
        System.out.println("Tokens used: " + (input + output));
        System.out.println("Effort: " + effort);
    }
    case ApiResponse.Error(var message, var code, var raw) -> {
        System.err.println("Error: " + message);
    }
    case ApiResponse.Partial(var availableText, var reason, var raw) -> {
        System.out.println("Partial: " + availableText);
    }
}
```

## Testing

### Fast Tests (Default)
Run unit tests and fast integration tests (excludes slow OpenAI API calls):
```bash
./gradlew test
```

### Integration Tests
Run slow integration tests that make actual OpenAI API calls:
```bash
./gradlew integrationTest
```

### All Tests
Run both fast and slow tests:
```bash
./gradlew allTests
```

### Code Coverage
Generate code coverage report:
```bash
./gradlew jacocoTestReport
```

View coverage report at `build/reports/jacoco/test/html/index.html`

### Test Categories
- **Fast tests**: Unit tests and mocked integrations (run by default)
- **Slow integration tests**: Marked with `@SlowIntegrationTest`, require OpenAI API key and take 2+ minutes

The integration tests require a valid OpenAI API key to be set in the environment.

## Configuration

### Application Properties
```properties
spring.application.name=SpringGPT5
spring.ai.openai.chat.options.model=gpt-5-nano
spring.ai.openai.chat.options.temperature=1.0
```

### Model Configuration
Both Spring AI `ChatClient` and the native `Gpt5NativeClient` use the same model configured via `spring.ai.openai.chat.options.model`. This ensures consistency across both integration approaches:
- **Development/Testing**: `gpt-5-nano` (faster, cheaper)
- **Production**: Change to `gpt-5` for full capabilities

### Reasoning Effort Levels
- `MINIMAL`: Basic reasoning
- `LOW`: Limited reasoning depth
- `MEDIUM`: Balanced reasoning (default)
- `HIGH`: Deep reasoning analysis

## CI/CD

The project includes GitHub Actions workflow that:
- Runs on Java 21 with Temurin distribution
- Executes fast tests on all PRs and pushes
- Runs integration tests only on main branch pushes
- Caches Gradle dependencies
- Uploads test reports and coverage

### GitHub Secrets Setup
Add your OpenAI API key as a repository secret:
- Go to Settings → Secrets and variables → Actions
- Add secret: `OPENAI_API_KEY`

## API Response Structure

The native client returns type-safe sealed interface responses:
```java
public sealed interface ApiResponse 
        permits ApiResponse.Success, ApiResponse.Error, ApiResponse.Partial {
    
    record Success(String text, String reasoningEffort, String reasoningTrace, 
                   Integer inputTokens, Integer outputTokens, JsonNode raw) 
                   implements ApiResponse {}
    
    record Error(String message, String code, JsonNode raw) 
                 implements ApiResponse {}
    
    record Partial(String availableText, String reason, JsonNode raw) 
                   implements ApiResponse {}
}
```

This provides:
- **Type Safety**: Compiler ensures all response types are handled
- **Pattern Matching**: Use switch expressions for elegant response processing  
- **Exhaustiveness**: No runtime surprises from unhandled cases

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## Dependencies

- Spring Boot 3.5.4
- Spring AI 1.0.1
- Java 21
- JUnit 5
- AssertJ
- JaCoCo (code coverage)

## Advanced Java 21 Features Showcase

This project showcases cutting-edge Java 21 features for modern application development:

### Sealed Interfaces (Java 17+)
Type-safe API response modeling with exhaustive pattern matching:
```java
public sealed interface ApiResponse 
        permits ApiResponse.Success, ApiResponse.Error, ApiResponse.Partial {
    
    record Success(String text, String reasoningEffort, String reasoningTrace, 
                   Integer inputTokens, Integer outputTokens, JsonNode raw) 
                   implements ApiResponse {}
    record Error(String message, String code, JsonNode raw) 
                 implements ApiResponse {}
    record Partial(String availableText, String reason, JsonNode raw) 
                   implements ApiResponse {}

    // Default methods with pattern matching
    default boolean isSuccess() {
        return this instanceof Success;
    }
    
    default String getTextContent() {
        return switch (this) {
            case Success(var text, var effort, var trace, var input, var output, var raw) -> text;
            case Error(var message, var code, var raw) -> null;
            case Partial(var availableText, var reason, var raw) -> availableText;
        };
    }
}
```

### Pattern Matching for Switch (Java 17-21)
Enhanced switch expressions with pattern matching:
```java
public String extractSafeContent(ApiResponse response) {
    return switch (response) {
        case ApiResponse.Success(var text, var effort, var trace, var input, var output, var raw) -> text;
        case ApiResponse.Error(var message, var code, var raw) -> null;
        case ApiResponse.Partial(var availableText, var reason, var raw) -> availableText;
    };
}
```

### Record Pattern Matching with Guards (Java 19-21)
Pattern matching with when clauses for advanced logic:
```java
private String classifyResponse(ApiResponse response) {
    return switch (response) {
        case ApiResponse.Success(var text, var effort, var trace, var input, var output, var raw) 
            when text.length() > 50 && "high".equals(effort) -> 
                "Long successful response with high effort";
        case ApiResponse.Success(var text, var effort, var trace, var input, var output, var raw) 
            when text.length() > 50 -> 
                "Long successful response";
        case ApiResponse.Success success -> "Short successful response";
        case ApiResponse.Error error -> "Error response";
        case ApiResponse.Partial partial -> "Partial response";
    };
}
```

### Pattern Matching with instanceof (Java 16+)
Modern type checking and variable extraction:
```java
private String processJsonNode(JsonNode node) {
    return switch (node) {
        case JsonNode n when n.isObject() -> 
            "Found object with type: " + n.path("type").asText("unknown");
        case JsonNode n when n.isArray() -> 
            "Found array with " + n.size() + " elements";
        case JsonNode n when n.isTextual() -> 
            "Found text: " + n.asText();
        default -> "Unknown node type";
    };
}
```

### Enhanced var Usage (Java 10+21)
Local variable type inference for complex generic types:
```java
// Complex generics made readable
var responseMap = Map.of(
    "success", new ApiResponse.Success("OK", "low", "trace", 10, 5, rawNode),
    "error", new ApiResponse.Error("Failed", "400", rawNode)
);

var results = responseMap.entrySet().stream()
    .collect(Collectors.toMap(
        Map.Entry::getKey,
        entry -> entry.getValue().isSuccess()
    ));
```

### Nested Sealed Interfaces
Advanced type modeling with nested sealed hierarchies:
```java
public sealed interface ResponseSummary {
    record SUCCESS() implements ResponseSummary {}
    record ERROR() implements ResponseSummary {}
    record PARTIAL() implements ResponseSummary {}
}

// Usage with pattern matching
var statusType = switch (summary.status()) {
    case SUCCESS success -> "success";
    case ERROR error -> "error";
    case PARTIAL partial -> "partial";
};
```

### Records (Java 14+)
Immutable data carriers with automatic implementations:
```java
public record Result(
    String text,
    String reasoningEffort,
    String reasoningTrace,
    Integer inputTokens,
    Integer outputTokens,
    JsonNode raw
) {
    // Convert to sealed interface
    public ApiResponse toApiResponse() {
        if (text == null || text.isEmpty()) {
            return new ApiResponse.Partial(text != null ? text : "", 
                                           "Empty response", raw);
        }
        return new ApiResponse.Success(text, reasoningEffort, reasoningTrace, 
                                       inputTokens, outputTokens, raw);
    }
}
```

### Text Blocks (Java 15+)
Multiline string literals:
```java
String body = """
        {
          "model": "%s",
          "input": %s,
          "reasoning": { "effort": %s }
        }
        """.formatted(model, messagesJson, effortJson);
```

### Functional Programming Patterns
Modern Java idioms with Optional and Stream APIs:
```java
return Optional.ofNullable(resp.get("output_text"))
        .filter(node -> !node.isNull())
        .map(JsonNode::asText)
        .or(() -> extractFromOutputArray(resp))
        .or(() -> extractFromFallbackPaths(resp))
        .orElse(null);
```

### Benefits of These Features
- **Type Safety**: Sealed interfaces provide compile-time guarantees about all possible types
- **Exhaustiveness**: Pattern matching ensures all cases are handled without runtime errors
- **Readability**: Less boilerplate code and more expressive syntax
- **Performance**: Some patterns can be optimized by the JVM
- **Maintainability**: Changes to sealed hierarchies are caught at compile time