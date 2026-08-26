---
name: spring-boot-scaffold
description: Scaffold en ny Spring Boot Kotlin-app i bidrag-backend-monorepoet med Maven, Nais-konfigurasjon, Flyway og standard mønstre
license: MIT
compatibility: navikt/bidrag-backend (Kotlin, Spring Boot, Maven monorepo, PostgreSQL)
metadata:
  domain: backend
  tags: spring-boot kotlin scaffold nais maven monorepo
---

# Spring Boot Kotlin app-scaffold (bidrag-backend)

Scaffold en ny Spring Boot **app-modul** inne i `bidrag-backend`-Maven-monorepoet, etter samme konvensjoner som eksisterende apper under `apps/`.

> Dette repoet bruker Maven og Spring-Boot, og hver app er en modul av root-`pom.xml` under `apps/<navn>/`. Sjekk alltid en eksisterende, lignende app før du scaffolder en ny – konvensjonene kan endre seg litt over tid.

## Arbeidsflyt

1. Opprett `apps/<navn>/` med sin egen `pom.xml` (modul av root-`pom.xml`)
2. Legg modulen til i root-`pom.xml` sin `<modules>`-seksjon
3. Opprett `Application.kt` og `application.yml`
4. Sett opp Nais-manifest (`.nais/nais.yaml` eller tilsvarende under appen)
5. Skriv `Dockerfile` etter repoets distroless + locale-mønster (se `$docker`-instruksjonene)
6. Legg til controller-, service- og repository-lag
7. Skriv integrasjonstester med MockOAuth2Server + Testcontainers (Kotest + MockK, se `$testing-kotlin`-instruksjonene)
8. Legg til en GitHub Actions-workflow etter det etablerte `detect_changes` → `bygg_og_test` → `deploy_*`-mønsteret (se `$code-review`-skillen)

## Prosjektstruktur

```
apps/mitt-nye-app/
├── .nais/
│   ├── nais.yaml
│   └── nais-dev.yaml
├── src/
│   ├── main/
│   │   ├── kotlin/no/nav/bidrag/mittnyeapp/
│   │   │   ├── Application.kt
│   │   │   ├── config/
│   │   │   │   └── SecurityConfig.kt
│   │   │   ├── controller/
│   │   │   │   └── ResourceController.kt
│   │   │   ├── service/
│   │   │   │   └── ResourceService.kt
│   │   │   ├── repository/
│   │   │   │   └── ResourceRepository.kt
│   │   │   └── model/
│   │   │       ├── Resource.kt
│   │   │       └── ResourceDTO.kt
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-nais.yml
│   │       └── db/migration/
│   │           └── V1_0_0__create-table-resource.sql
│   └── test/
│       └── kotlin/no/nav/bidrag/mittnyeapp/
│           ├── controller/
│           │   └── ResourceControllerTest.kt
│           └── repository/
│               └── ResourceRepositoryTest.kt
├── Dockerfile
└── pom.xml
```

## pom.xml

Arv fra root-`pom.xml` (parent) – ikke re-deklarer versjoner som allerede styres der. Legg kun til modulspesifikke avhengigheter:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>no.nav.bidrag</groupId>
    <artifactId>bidrag-backend</artifactId>
    <version>${revision}</version>
    <relativePath>../../pom.xml</relativePath>
  </parent>

  <artifactId>mitt-nye-app</artifactId>
  <packaging>jar</packaging>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-database-postgresql</artifactId>
    </dependency>
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
    </dependency>
    <dependency>
      <groupId>no.nav.security</groupId>
      <artifactId>token-validation-spring</artifactId>
    </dependency>

    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>no.nav.security</groupId>
      <artifactId>mock-oauth2-server</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>postgresql</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>io.mockk</groupId>
      <artifactId>mockk-jvm</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>com.ninja-squad</groupId>
      <artifactId>springmockk</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>io.kotest</groupId>
      <artifactId>kotest-runner-junit5-jvm</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>
</project>
```

Legg til modulen i root `pom.xml`:

```xml
<modules>
  ...
  <module>apps/mitt-nye-app</module>
</modules>
```

## Application.kt

```kotlin
package no.nav.bidrag.mittnyeapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
```

## application.yml

```yaml
server:
  port: 8080

spring:
  application:
    name: mitt-nye-app
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_DATABASE:mittnyeapp}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
  flyway:
    enabled: true

management:
  endpoints:
    web:
      exposure:
        include: health,prometheus
      base-path: /internal
  endpoint:
    health:
      show-details: always

no.nav.security.jwt:
  issuer:
    azuread:
      discoveryurl: ${AZURE_APP_WELL_KNOWN_URL}
      accepted-audience: ${AZURE_APP_CLIENT_ID}
```

## Nais-manifest (.nais/nais.yaml)

```yaml
apiVersion: nais.io/v1alpha1
kind: Application
metadata:
  name: mitt-nye-app
  namespace: bidrag
  labels:
    team: bidrag
spec:
  image: {{ image }}
  port: 8080
  liveness:
    path: /internal/health/liveness
  readiness:
    path: /internal/health/readiness
  prometheus:
    enabled: true
    path: /internal/prometheus
  replicas:
    min: 2
    max: 4
  resources:
    limits:
      memory: 512Mi
    requests:
      cpu: 50m
      memory: 256Mi
  gcp:
    sqlInstances:
      - type: POSTGRES_15
        databases:
          - name: mittnyeapp
  azure:
    application:
      enabled: true
  accessPolicy:
    inbound:
      rules:
        - application: annen-bidrag-app
    outbound:
      rules: []
```

## Dockerfile

Følg det etablerte mønsteret i repoet — bygg med Maven i CI, kopier kun jar-filen inn (se `$docker`-instruksjonene for full begrunnelse):

```dockerfile
FROM ubuntu:26.04 AS locales
RUN apt-get update && apt-get install -y locales
RUN locale-gen nb_NO.UTF-8 && \
    update-locale LANG=nb_NO.UTF-8 LANGUAGE="nb_NO:nb" LC_ALL=nb_NO.UTF-8

FROM gcr.io/distroless/java21
LABEL maintainer="Team Bidrag" \
      email="bidrag@nav.no"

COPY --from=busybox:1.35.0-glibc /bin/sh /bin/sh
COPY --from=busybox:1.35.0-glibc /bin/printenv /bin/printenv
COPY --from=locales /usr/lib/locale/ /usr/lib/locale/

WORKDIR /app
COPY ./target/app.jar app.jar

EXPOSE 8080
ENV LANG=nb_NO.UTF-8 LANGUAGE='nb_NO:nb' LC_ALL=nb_NO.UTF-8 TZ="Europe/Oslo"
ENV SPRING_PROFILES_ACTIVE=nais

CMD ["app.jar"]
```

## Controller

```kotlin
package no.nav.bidrag.mittnyeapp.controller

import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/resources")
@ProtectedWithClaims(issuer = "azuread")
class ResourceController(
    private val service: ResourceService,
) {
    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): ResponseEntity<ResourceDTO> =
        service.findById(id)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.of(
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Resource $id not found")
            ).build()

    @PostMapping
    fun create(@RequestBody @Valid request: CreateResourceRequest): ResponseEntity<ResourceDTO> {
        val created = service.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }
}
```

## Test med MockOAuth2Server + Testcontainers

```kotlin
package no.nav.bidrag.mittnyeapp.controller

import io.kotest.matchers.shouldBe
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class ResourceControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:15")

        val mockOAuth2Server = MockOAuth2Server()

        @BeforeAll
        @JvmStatic
        fun setup() {
            mockOAuth2Server.start()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            mockOAuth2Server.shutdown()
        }

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("no.nav.security.jwt.issuer.azuread.discoveryurl") {
                mockOAuth2Server.wellKnownUrl("azuread").toString()
            }
            registry.add("no.nav.security.jwt.issuer.azuread.accepted-audience") { "test-aud" }
        }
    }

    private fun token() = mockOAuth2Server.issueToken(
        issuerId = "azuread",
        audience = "test-aud",
        claims = mapOf("preferred_username" to "test@nav.no"),
    ).serialize()

    @Test
    fun `should return 401 without token`() {
        mockMvc.get("/api/resources/123").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `should return resources with valid token`() {
        mockMvc.get("/api/resources") {
            header("Authorization", "******")
        }.andExpect {
            status { isOk() }
        }
    }
}
```

## docker-compose.yml (lokal utvikling)

```yaml
services:
  postgres:
    image: postgres:15
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: mittnyeapp
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
```

## Relatert

| Type | Navn | Når brukes |
|------|------|-------------|
| Instruksjon | `$kotlin-spring` | Controller-/service-/repository-mønstre |
| Instruksjon | `$docker` | Dockerfile-konvensjoner for dette repoet |
| Instruksjon | `$database` | Flyway-navnekonvensjon brukt i dette repoet |
| Instruksjon | `$github-actions` | CI/CD-workflow-mønster (`detect_changes` → `bygg_og_test` → `deploy_*`) |
| Skill | `$flyway-migration` | Databasemigrasjonsmønstre |
| Skill | `$code-review` | Full repo-spesifikk sjekkliste for gjennomgang av den nye modulen |
