plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.1.4"
  id("org.springdoc.openapi-gradle-plugin") version "1.6.0"
  kotlin("plugin.spring") version "1.8.21"
  id("jacoco")
}

configurations {
  testImplementation {
    exclude(group = "org.junit.vintage")
    exclude(group = "logback-classic")
  }
}

dependencies {
  // Spring boot dependencies
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-actuator")

  compileOnly("javax.servlet:javax.servlet-api:4.0.1")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.1.0")

  // JWT
  implementation("io.jsonwebtoken:jjwt-api:0.11.5")
  runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
  runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

  // Test deps
  testImplementation("com.github.tomakehurst:wiremock-jre8-standalone:2.35.0")
  testImplementation("io.swagger.parser.v3:swagger-parser-v2-converter:2.1.13")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(19))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "19"
    }
  }
}
