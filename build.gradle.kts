// build.gradle.kts
plugins {
    java
    id("org.springframework.boot") version "3.2.11"
    id("io.spring.dependency-management") version "1.1.6"
    id("maven-publish")
}

group = "kr.or.komca"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
    all {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot 핵심 의존성
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-log4j2")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // 테스트 코드 작성용
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.3.1")
    testImplementation("org.mockito:mockito-junit-jupiter:5.3.1")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            // url은 https://maven.pkg.github.com/YOUR_ORGANIZATION/githun repository 이름
            url = uri("https://maven.pkg.github.com/KimMunjin/utils")
            credentials {
                username = System.getenv("GITHUB_ACTOR") // workflows를 실행시킨 사용자 자동 설정
                password = System.getenv("GITHUB_TOKEN") // Actions에서 자동 생성되는 토큰
            }
        }
    }

    publications {
        create<MavenPublication>("gpr") {
            from(components["java"])
            version = System.getenv("NEW_VERSION") ?: "0.0.1-SNAPSHOT"  // workflow의 NEW_VERSION 환경변수 사용
            groupId = "kr.or.komca"
            artifactId = "utils"

            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
        }
    }
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.named<Jar>("jar") {
    enabled = true
}