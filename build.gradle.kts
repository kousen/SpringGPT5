plugins {
    java
    jacoco
    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.sonarqube") version "6.2.0.5505"
}

group = "com.kousenit"
version = "1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

extra["springAiVersion"] = "1.0.1"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.ai:spring-ai-starter-model-openai")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform {
        excludeTags("slow")
    }
    jvmArgs = listOf("-Xshare:off", "-XX:+EnableDynamicAgentLoading")
    finalizedBy(tasks.jacocoTestReport)
}

tasks.register<Test>("integrationTest") {
    description = "Run integration tests including slow OpenAI API tests"
    group = "verification"
    useJUnitPlatform {
        includeTags("slow")
    }
    jvmArgs = listOf("-Xshare:off", "-XX:+EnableDynamicAgentLoading")
    finalizedBy(tasks.jacocoTestReport)
    shouldRunAfter(tasks.test)
}

tasks.register<Test>("allTests") {
    description = "Run all tests including integration tests"
    group = "verification"
    useJUnitPlatform()
    jvmArgs = listOf("-Xshare:off", "-XX:+EnableDynamicAgentLoading")
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    executionData.setFrom(fileTree(layout.buildDirectory.dir("jacoco")).include("**/*.exec"))
    reports {
        xml.required = true
        html.required = true
        csv.required = false
    }
}

jacoco {
    toolVersion = "0.8.13"
}

sonar {
    properties {
        property("sonar.projectKey", "kousen_SpringGPT5")
        property("sonar.organization", "kousen-it-inc")
        property("sonar.host.url", "https://sonarcloud.io")
        
        // Disable rules that add unnecessary complexity for small projects
        property("sonar.issue.ignore.multicriteria", "e1,e2,e3")
        property("sonar.issue.ignore.multicriteria.e1.ruleKey", "java:S1192") // String literal duplication
        property("sonar.issue.ignore.multicriteria.e1.resourceKey", "**/*.java")
        property("sonar.issue.ignore.multicriteria.e2.ruleKey", "java:S112")  // Generic exception throwing
        property("sonar.issue.ignore.multicriteria.e2.resourceKey", "**/*.java")
        property("sonar.issue.ignore.multicriteria.e3.ruleKey", "java:S4144") // Duplicate code blocks in switch
        property("sonar.issue.ignore.multicriteria.e3.resourceKey", "**/*.java")
    }
}


