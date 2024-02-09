plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.15.1"
  kotlin("plugin.spring") version "1.9.22"
  kotlin("plugin.jpa") version "1.9.22"
  id("se.patrikerdes.use-latest-versions") version "0.2.18"
}

configurations {
  testImplementation {
    exclude(group = "org.junit.vintage")
  }
}

dependencies {
  // Spring boot dependencies
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-actuator")

  // Database dependencies
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.postgresql:postgresql:42.7.1")

  compileOnly("javax.servlet:javax.servlet-api:4.0.1")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

  // JWT
  implementation("io.jsonwebtoken:jjwt-api:0.12.5")
  runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
  runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")

  // Test deps
  testImplementation("com.github.tomakehurst:wiremock-jre8-standalone:3.0.1")
  testImplementation("io.swagger.parser.v3:swagger-parser-v2-converter:2.1.20")
  testImplementation("com.h2database:h2")
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "21"
    }
  }
}
