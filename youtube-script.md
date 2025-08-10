# GPT-5 Integration in Spring Boot: Two Approaches That Changed Everything

**Target Length**: 18-20 minutes  
**Format**: Educational/Tutorial  
**Audience**: Java developers interested in AI integration  

## Video Title Options (for A/B testing):
1. "GPT-5 + Spring Boot: Why I Built Two Different APIs (One Will Shock You)"
2. "Java 21 Pattern Matching + GPT-5 Reasoning: The Perfect Match?"
3. "Spring AI vs Native OpenAI: Which GPT-5 Integration Should You Choose?"
4. "I Integrated GPT-5 with Spring Boot and Discovered This Game-Changing Feature"
5. "Zero SonarCloud Issues: Building Production-Ready GPT-5 Integration"

## Thumbnail Concept:
- Split screen: Spring AI logo vs OpenAI logo
- Java 21 badge prominently displayed
- Code snippet showing pattern matching
- "94% Test Coverage" badge
- Contrasting colors: green (Spring) vs orange (OpenAI)

---

## SCRIPT

### HOOK (0:00-0:15)
[Scene: Developer at computer, looking frustrated]

**"I spent weeks trying to get GPT-5's reasoning capabilities working with Spring Boot, and what I discovered changed how I think about AI integration forever."**

[Quick montage: Code compilation errors, then success, then excited reaction]

**"In this video, I'm going to show you two completely different approaches to integrating GPT-5 with Spring Boot - one that's ridiculously simple but limited, and another that unlocks GPT-5's full reasoning power. Plus, we'll see how Java 21's modern features make this code not just cleaner, but actually safer."**

[Text overlay: "2 Approaches • Java 21 Features • Real Code Examples"]

---

### INTRODUCTION & SETUP (0:15-1:30)

[Scene: Clean desk setup with IDE open showing project structure]

**"Hey developers! I'm here with a real-world Spring Boot application that integrates GPT-5, and I want to show you something that most tutorials completely miss."**

[Screen: GitHub repository overview]

**"This isn't just another 'Hello World' AI demo. This is a production-ready application with 94% test coverage, zero SonarCloud issues, and comprehensive error handling. But here's what makes it special..."**

[Screen: Two side-by-side code files - MyAiService.java showing both approaches]

**"I built TWO different ways to call GPT-5 from Spring Boot, and they serve completely different purposes. Let me show you why you might need both."**

[Text overlay: "SpringGPT5 Project • Production Ready • Java 21"]

---

### PROBLEM STATEMENT (1:30-2:45)

[Scene: Animated diagram showing API request flow]

**"Here's the problem most developers run into when integrating GPT-5: Spring AI makes it incredibly easy to get started, but GPT-5 has this amazing reasoning feature that Spring AI can't access."**

[Screen: OpenAI API documentation highlighting reasoning parameters]

**"GPT-5 can show you its reasoning process, provide effort levels, and give you detailed traces of how it reached its conclusions. This is huge for debugging AI responses and building trust with users. But if you only use Spring AI, you'll never see any of this."**

[Diagram: Spring AI path vs Direct API path]

**"So I had two choices: stick with Spring AI and miss out on the advanced features, or go native and lose Spring's convenience. Instead, I did both, and I'll show you exactly when to use each approach."**

---

### APPROACH 1: SPRING AI - THE EASY PATH (2:45-6:00)

[Scene: IDE showing MyAiService.java]

**"Let's start with the Spring AI approach because it's beautifully simple. Here's literally all the code you need:"**

[Screen: Highlighting the Spring AI method]

```java
public String normalAnswer(String prompt) {
    return chat.prompt(prompt).call().content();
}
```

**"One line. That's it. Spring handles the HTTP calls, JSON parsing, error handling - everything. But watch what happens when I try to access GPT-5's reasoning..."**

[Screen: Spring AI configuration showing temperature requirement]

**"Here's the first gotcha: GPT-5 models require temperature equals 1.0, or they just won't work. Spring AI doesn't tell you this - you have to figure it out from cryptic error messages."**

[Screen: application.properties file]

```properties
spring.ai.openai.chat.options.model=gpt-5-nano
spring.ai.openai.chat.options.temperature=1.0
```

**"The second limitation is more serious: Spring AI only gives you the final text response. No reasoning traces, no effort levels, no token usage - nothing. It's like asking a student to show their work and only getting the final answer."**

[Demo: Running Spring AI call and showing limited response]

**"Don't get me wrong - this approach is perfect for simple chat features, quick prototypes, or when you just need basic text generation. But for production applications where you need visibility into the AI's reasoning process, we need something more powerful."**

---

### APPROACH 2: NATIVE CLIENT - FULL POWER (6:00-10:30)

[Scene: Switching to Gpt5NativeClient.java]

**"This is where the native approach shines. Instead of hiding the complexity, we embrace it and gain full control over the GPT-5 API."**

[Screen: Gpt5NativeClient constructor]

**"I'm using Spring's RestClient - not the old RestTemplate or WebClient - but the new RestClient that gives us the perfect balance of simplicity and power."**

[Screen: chatWithReasoning method]

**"Now watch this - when I make a native call, I can specify the reasoning effort level:"**

```java
public ApiResponse chatWithReasoning(String userPrompt, ReasoningEffort effort) {
    var messages = List.of(Map.of("role", "user", "content", userPrompt));
    return send(messages, effort);
}
```

**"The reasoning effort enum gives us four levels: minimal, low, medium, and high. Each level tells GPT-5 how much computational effort to spend on reasoning about the problem."**

[Screen: ReasoningEffort enum]

**"But here's where it gets really interesting - look at the response structure:"**

[Screen: ApiResponse sealed interface]

**"This is Java 21's sealed interfaces in action. Instead of returning a generic object and hoping for the best, we get compile-time guarantees about what we can receive: Success, Error, or Partial responses."**

---

### JAVA 21 MODERN FEATURES SPOTLIGHT (10:30-13:00)

[Scene: Animated diagram showing sealed interface hierarchy]

**"Let me pause here and show you why this Java 21 code is so much better than the old way of doing things."**

[Screen: ApiResponse sealed interface definition]

**"Sealed interfaces mean the compiler knows exactly which implementations are possible. No surprises at runtime."**

[Screen: Pattern matching in switch expression]

```java
return switch (response) {
    case ApiResponse.Success success -> success.text();
    case ApiResponse.Error ignored -> null;
    case ApiResponse.Partial partial -> partial.availableText();
};
```

**"Pattern matching in switch expressions eliminates the boilerplate. No more instanceof checks, no more casting, no more forgetting to handle a case. The compiler forces us to handle every possibility."**

[Screen: Before/after comparison showing old vs new pattern matching]

**"Here's what this same code would look like in Java 17:"**

[Split screen showing verbose instanceof chains vs clean pattern matching]

**"Six lines of boilerplate reduced to one elegant switch expression. And if OpenAI adds a new response type, our code won't even compile until we handle it. That's type safety in action."**

[Screen: Record definitions]

**"Records make our data structures immutable and boilerplate-free. Every Success response gives us text, reasoning effort, reasoning traces, token counts, and the raw JSON - all with automatic equals, hashCode, and toString methods."**

---

### ADVANCED RESPONSE PROCESSING (13:00-15:30)

[Scene: Complex JSON response from OpenAI API]

**"Now here's where the native approach really shines - processing complex API responses. GPT-5 doesn't always return data in the same format, so we need robust parsing."**

[Screen: extractText method with pattern matching]

**"Look at this method that extracts text from various response formats:"**

```java
static String extractText(JsonNode resp) {
    if (resp instanceof JsonNode directNode && directNode.has("output_text")) {
        var outputText = directNode.get("output_text");
        if (!outputText.isNull()) return outputText.asText();
    }
    // ... more pattern matching logic
}
```

**"We're using enhanced instanceof with pattern matching to handle different JSON structures. If the response has direct output text, we grab it. Otherwise, we process the output array."**

[Screen: switch expression processing output types]

```java
switch (itemType) {
    case "message" -> processMessageContent(item, sb);
    case "function_call" -> processFunctionCall(item, sb);
    case "error" -> { /* Skip errors in partial responses */ }
    case null, default -> { /* Ignore unknown types */ }
}
```

**"This switch handles multiple output types gracefully. Message content, function calls, errors - each gets its own processing logic. And notice that null case - that's Java 21 protecting us from null pointer exceptions."**

[Demo: API call showing full response with reasoning traces]

**"When we make a native call, we get everything: the reasoning effort level, detailed traces showing how GPT-5 solved the problem, input and output token counts for cost tracking, and the complete raw JSON in case we need to extract additional data later."**

---

### TESTING EXCELLENCE (15:30-17:00)

[Scene: Test file overview]

**"Now, you might be thinking this dual approach makes testing complicated. Actually, it makes testing better. Let me show you the testing strategy."**

[Screen: MyAiServiceUnitTest.java]

**"I've got 99% test coverage on the service class, and here's how I achieve it using modern testing patterns:"**

[Screen: Test using Mockito and AssertJ]

```java
@Test
void shouldHandleErrorResponseInReasoningAnswer() throws Exception {
    var errorResponse = new ApiResponse.Error(
        "API limit exceeded", "rate_limit", mapper.createObjectNode()
    );
    when(gpt5NativeClient.chatWithReasoning("test", ReasoningEffort.LOW))
        .thenReturn(errorResponse);
        
    ApiResponse result = service.gpt5ReasoningAnswer("test", ReasoningEffort.LOW);
    
    assertThat(result).isInstanceOf(ApiResponse.Error.class);
    if (result instanceof ApiResponse.Error error) {
        assertEquals("API limit exceeded", error.message());
        assertEquals("rate_limit", error.code());
    }
}
```

**"Pattern matching makes our test assertions cleaner and type-safe. We're testing all three response types: success, error, and partial responses."**

[Screen: Test coverage report showing 94%]

**"The beauty of sealed interfaces is that our tests can't forget to handle a response type. If we add a new case, the tests won't compile until we cover it."**

**"I even use parameterized tests to consolidate similar test cases:"**

[Screen: Parameterized test extracting text from various JSON structures]

**"Instead of five similar tests, one parameterized test covers all the edge cases. This is modern testing - comprehensive but maintainable."**

[Screen: Gpt5NativeClientUnitTest.java with WireMock setup]

**"But here's something crucial about testing HTTP clients - I learned this the hard way. The Mockito team says 'don't mock what you don't own,' and that includes Spring's RestClient classes."**

[Screen: Before/after comparison showing complex mock chain vs WireMock]

**"I originally tried mocking RestClient with a 4-level mock chain - it was fragile, hard to maintain, and didn't actually test HTTP behavior. So I switched to WireMock, which runs a real HTTP server in tests."**

```java
@BeforeEach
void setUp() {
    wireMockServer = new WireMockServer(WireMockConfiguration.options().port(8089));
    wireMockServer.start();
    
    RestClient restClient = RestClient.builder()
            .baseUrl("http://localhost:8089/v1")
            .build();
            
    client = new Gpt5NativeClient(restClient, mapper, "gpt-5-nano");
}
```

**"Now we're testing real HTTP interactions, not mock objects. This boosted our native client coverage from 34% to 94% and gives us confidence that our HTTP handling actually works."**

---

### PRODUCTION CONSIDERATIONS (17:00-18:30)

[Scene: CI/CD pipeline visualization]

**"Before we wrap up, let's talk about production readiness. This isn't just demo code - it's built for real applications."**

[Screen: SonarCloud analysis showing zero issues]

**"Zero SonarCloud issues. I strategically disabled a few rules that add unnecessary complexity for educational projects, but everything else passes strict quality gates."**

[Screen: GitHub Actions workflow]

**"Continuous integration with separate test categories - fast unit tests for development, slow integration tests for deployment. The pipeline uses GPT-5-nano for cost-effective testing."**

[Screen: Gradle configuration showing Java 21 features]

**"Java 21 toolchain ensures consistent builds across environments. JaCoCo for coverage reporting, dependency verification for security - all the enterprise practices you need."**

**"When should you use each approach? Spring AI for:"**

[Text overlay list appearing]
- Simple chat features
- Rapid prototyping  
- Basic text generation
- When you don't need reasoning traces

**"Native client for:"**

[Text overlay list appearing]
- Production applications requiring reasoning visibility
- Cost tracking and optimization
- Advanced error handling
- Custom response processing
- Integration with existing monitoring systems

---

### CONCLUSION & CALL TO ACTION (18:30-20:00)

[Scene: Side-by-side comparison of both approaches]

**"So there you have it - two completely different approaches to GPT-5 integration, each with its own strengths. The Spring AI path gets you started fast, while the native client gives you production-grade control."**

**"But here's what I find most exciting: Java 21's modern features make complex API integration not just possible, but enjoyable. Pattern matching, sealed interfaces, records - these aren't just syntactic sugar. They're tools that make our code safer, cleaner, and more maintainable."**

[Screen: Final project structure overview]

**"I've open-sourced this entire project - all the code, tests, configuration, and documentation. The link is in the description."**

**"What questions do you have about GPT-5 integration? Are you using Spring AI, going native, or trying something completely different? Let me know in the comments."**

**"And if you're working on AI integration projects, I'd love to see what you're building. Share your experiences, challenges, and wins."**

[Text overlay: Subscribe for more Java + AI content]

**"If this deep dive was helpful, subscribe for more practical Java tutorials. I'm working on a series covering advanced Spring AI patterns, Java 21 features in production, and modern testing strategies."**

**"Next week, I'll show you how to implement streaming responses with Server-Sent Events for real-time AI chat. It builds directly on what we covered today."**

**"Thanks for watching, and I'll see you in the next one!"**

[End screen with related videos and subscribe button]

---

## VIDEO PRODUCTION NOTES

### Key Visual Elements:
- Split-screen comparisons of Spring AI vs Native approaches
- Animated diagrams for API flows and sealed interface hierarchies
- Code highlighting with smooth transitions
- Live coding demonstrations with real API calls
- Test execution and coverage reports

### B-Roll Suggestions:
- GitHub repository browsing
- IDE features highlighting Java 21 syntax
- Test execution in terminal
- SonarCloud dashboard
- Performance monitoring graphs
- API response JSON structures

### Technical Setup:
- Screen recording at 1080p minimum
- Code font size 14pt for readability
- Syntax highlighting for Java, JSON, and configuration files
- Zoom effects for detailed code examination
- Picture-in-picture for presenter during demos

### Engagement Optimization:
- Pattern interrupt every 45-60 seconds with visual transitions
- Strategic pauses for complex concepts
- Rhetorical questions to maintain viewer attention
- Progress indicators for multi-step processes
- Clear section transitions with visual cues

### SEO Keywords:
Primary: GPT-5 Spring Boot integration, Java 21 pattern matching, Spring AI vs native
Secondary: sealed interfaces Java, OpenAI API Spring, RestClient tutorial, modern Java features
Long-tail: How to integrate GPT-5 with Spring Boot, Java 21 sealed interfaces example, Spring AI limitations

### Suggested Tags:
Java21, SpringBoot, GPT5, OpenAI, SpringAI, PatternMatching, SealedInterfaces, RestClient, APIIntegration, ModernJava, TestCoverage, ProductionCode, AIIntegration, JavaDevelopment, EnterpriseJava

### Chapter Markers:
- 0:00 Introduction
- 1:30 The Integration Problem
- 2:45 Spring AI Approach
- 6:00 Native Client Approach  
- 10:30 Java 21 Features
- 13:00 Advanced Response Processing
- 15:30 Testing Strategy
- 17:00 Production Considerations
- 18:30 Conclusion

### End Screen Strategy:
- Related video: "Java 21 Records vs Classes" (if available)
- Series playlist: "Modern Java + AI Integration"
- Subscribe button with notification bell reminder
- Community tab link for project discussions