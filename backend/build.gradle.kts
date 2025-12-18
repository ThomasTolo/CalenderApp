import org.gradle.api.tasks.GradleBuild

plugins {
	java
	id("org.springframework.boot") version "3.5.5"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "CalenderApp"
version = "0.0.1-SNAPSHOT"
description = "Demo project for Spring Boot"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-validation")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:2.6.0")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.h2database:h2")
	runtimeOnly("org.postgresql:postgresql")

    implementation("redis.clients:jedis:6.2.0")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.apache.kafka:kafka-clients:3.7.0")

    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation ("org.springframework.boot:spring-boot-starter-actuator")

    implementation("org.springframework.boot:spring-boot-starter-security")

    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
}

val frontendDir = layout.projectDirectory.dir("../frontend")
val frontendDistDir = frontendDir.dir("dist")

val buildFrontend by tasks.registering(GradleBuild::class) {
	dir = frontendDir.asFile
	tasks = listOf("runBuild")
}

tasks.processResources {
	dependsOn(buildFrontend)

	// Add Vite build output to the backend classpath at META-INF/resources/*
	from(frontendDistDir) {
		into("META-INF/resources")
	}
}

// Optional: ensure `bootRun` always has fresh resources when you start the backend
tasks.named("bootRun") {
	dependsOn(tasks.named("processResources"))
}

tasks.withType<Test> {
	useJUnitPlatform()
}
