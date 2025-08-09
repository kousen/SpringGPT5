# SpringGPT5

A Spring Boot application demonstrating integration with OpenAI's GPT-5 model using both Spring AI framework and native OpenAI API calls.

## Features

- **Dual Integration Approach**: Uses both Spring AI's `ChatClient` and direct OpenAI API calls
- **GPT-5 Reasoning API**: Direct access to OpenAI's reasoning capabilities with configurable effort levels
- **Type-Safe Configuration**: Uses `ReasoningEffort` enum for parameter validation
- **Comprehensive Testing**: Integration tests for both Spring AI and native client approaches
- **Modern Java**: Built with Java 24 and Spring Boot 3.5.4

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
Gpt5NativeClient.Result result = service.gpt5ReasoningAnswer(
    "Explain the benefits of functional programming"
);

// With custom effort level
Gpt5NativeClient.Result result = service.gpt5ReasoningAnswer(
    "Solve this complex problem", 
    ReasoningEffort.HIGH
);

// Access reasoning trace
String reasoning = result.reasoningTrace();
String effort = result.reasoningEffort();
Integer tokens = result.inputTokens();
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

The native client returns structured results:
```java
public record Result(
    String text,              // Generated response text
    String reasoningEffort,   // Actual effort level used
    String reasoningTrace,    // Reasoning process trace
    Integer inputTokens,      // Input token count
    Integer outputTokens,     // Output token count
    JsonNode raw             // Raw API response
) {}
```

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