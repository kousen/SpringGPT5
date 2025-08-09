plugins {
    java
    jacoco
    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.kousenit"
version = "0.0.1-SNAPSHOT"

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
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
        csv.required = false
    }
}

jacoco {
    toolVersion = "0.8.13"
}
