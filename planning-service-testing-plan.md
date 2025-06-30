# Planning Service: Test Refactoring Plan (PostgreSQL & Testcontainers)

## 1. Goal

The primary goal is to refactor the entire test suite for the `Planning-Service` to use **Testcontainers with PostgreSQL**. This will replace the in-memory H2 database for all layers of testing, including repositories, services, and controllers, ensuring that tests run against a production-like environment.

## 2. Benefits

- **Production Parity**: Tests run against a real PostgreSQL database, catching native query issues and behavioral differences that H2 might miss.
- **Data Consistency**: Ensures that the schema, constraints, and functions behave exactly as they do in production.
- **Simplified Configuration**: A single, shared Testcontainers setup can be used across all integration tests, reducing configuration duplication.
- **Reliable Integration Tests**: Controller and service integration tests will validate the full stack, from the API endpoint down to the database, in a realistic environment.

## 3. Core Technologies

- **Spring Boot Test**: For integration testing (`@SpringBootTest`, `@DataR2dbcTest`).
- **Testcontainers**: To manage the lifecycle of a PostgreSQL Docker container.
- **PostgreSQL**: The target database for all tests.
- **R2DBC**: For reactive database access.
- **Flyway**: To manage database schema migrations.
- **JUnit 5**: The testing framework.
- **MockK**: For mocking dependencies in unit tests.

## 4. High-Level Refactoring Strategy

The strategy is to create a centralized Testcontainers configuration that can be imported into any test class requiring a database. We will then refactor tests from the bottom up (repository to controller).

1.  **Add Dependencies**: Update `build.gradle.kts` with the necessary Testcontainers and PostgreSQL dependencies.
2.  **Create Shared Test Configuration**: Implement a reusable `TestcontainersConfiguration` class to manage the PostgreSQL container.
3.  **Refactor Repository Tests**: Convert `@DataR2dbcTest` classes to use the PostgreSQL container instead of H2.
4.  **Refactor Service Tests**: Service tests that require database access will be converted into integration tests using `@SpringBootTest`.
5.  **Refactor Controller Tests**: Controller tests will become full integration tests using `@SpringBootTest` and `WebTestClient`, backed by the Testcontainers database.
6.  **Remove H2**: Completely remove the H2 database dependency and any related configuration.

---

## 5. Step-by-Step Implementation Plan

### Phase 1: Update Dependencies

Modify `microservices/Planning-Service-Clockwise/build.gradle.kts` to include the Testcontainers dependencies and remove H2.

```kotlin
// build.gradle.kts

// ... other dependencies

dependencies {
    // ...
    // Remove H2
    // testImplementation("com.h2database:h2")
    // testImplementation("io.r2dbc:r2dbc-h2")

    // Add Testcontainers
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
}
```

### Phase 2: Create Shared Testcontainers Configuration

Create a new file `src/test/kotlin/com/clockwise/planningservice/TestcontainersConfiguration.kt`. This class will be the single source of truth for our test database.

```kotlin
package com.clockwise.planningservice

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    companion object {
        @JvmStatic
        @Container
        @ServiceConnection
        val postgresqlContainer = PostgreSQLContainer(DockerImageName.parse("postgres:14-alpine"))

        @JvmStatic
        @DynamicPropertySource
        fun setProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.r2dbc.url") { 
                "r2dbc:postgresql://${postgresqlContainer.host}:${postgresqlContainer.firstMappedPort}/${postgresqlContainer.databaseName}" 
            }
            registry.add("spring.r2dbc.username", postgresqlContainer::getUsername)
            registry.add("spring.r2dbc.password", postgresqlContainer::getPassword)
            
            registry.add("spring.flyway.url", postgresqlContainer::getJdbcUrl)
            registry.add("spring.flyway.user", postgresqlContainer::getUsername)
            registry.add("spring.flyway.password", postgresqlContainer::getPassword)
        }
    }
}
```

### Phase 3: Refactor Repository Tests

Update all repository tests (e.g., `ScheduleRepositoryTest`) to use the new Testcontainers setup.

**Before (Example with H2):**
```kotlin
@DataR2dbcTest
class ScheduleRepositoryTest {
    // ... tests using H2
}
```

**After (Using Testcontainers):**
```kotlin
import org.springframework.context.annotation.Import
import org.testcontainers.junit.jupiter.Testcontainers

@DataR2dbcTest
@Testcontainers
@Import(TestcontainersConfiguration::class)
class ScheduleRepositoryTest(@Autowired private val repository: ScheduleRepository) {

    @Test
    fun `should save and retrieve a schedule`() = runBlocking {
        // Given
        val schedule = Schedule(...)
        
        // When
        val saved = repository.save(schedule)
        val found = repository.findById(saved.id!!)

        // Then
        assertNotNull(found)
        assertEquals(schedule.name, found.name)
    }
}
```
*Apply this pattern to all repository tests.*

### Phase 4: Refactor Service Integration Tests

For service tests that require interaction with the database, convert them to integration tests.

**Before (Example with Mocks):**
```kotlin
class ScheduleServiceTest {
    private val repository: ScheduleRepository = mockk()
    private val service = ScheduleService(repository)
    // ... unit tests
}
```

**After (Integration Test):**
```kotlin
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@Testcontainers
@Import(TestcontainersConfiguration::class)
class ScheduleServiceIntegrationTest(@Autowired private val service: ScheduleService) {

    @Test
    fun `should create schedule and store it in database`() = runBlocking {
        // Given
        val createRequest = CreateScheduleRequest(...)

        // When
        val createdSchedule = service.createSchedule(createRequest)

        // Then
        assertNotNull(createdSchedule.id)
        // Further assertions against the database state if needed
    }
}
```
*Note: Pure unit tests for services (those without database interaction) can remain as they are, using mocks.*

### Phase 5: Refactor Controller Integration Tests

Convert controller tests to full integration tests that use a real database.

**Before (Example with Mocked Service):**
```kotlin
@WebFluxTest(ScheduleController::class)
class ScheduleControllerTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var service: ScheduleService
    
    // ... tests with mocked service responses
}
```

**After (Full Integration Test):**
```kotlin
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.context.annotation.Import
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Import(TestcontainersConfiguration::class)
class ScheduleControllerIntegrationTest(@Autowired private val webTestClient: WebTestClient) {

    @Test
    fun `POST to schedules should create a new schedule`() {
        // Given
        val requestBody = CreateScheduleRequest(name = "New Year's Schedule", ...)

        // When & Then
        webTestClient.post().uri("/schedules")
            .bodyValue(requestBody)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.id").isNotEmpty
            .jsonPath("$.name").isEqualTo("New Year's Schedule")
    }
}
```

### Phase 6: Remove H2 Database

After migrating all tests, perform the final cleanup:
1.  **Delete H2 Dependency**: Ensure the H2 dependency is removed from `build.gradle.kts`.
2.  **Remove H2 Configuration**: Delete any files in `src/test/resources` that are specific to H2 (e.g., `schema-h2.sql`, `application-test.properties` with H2 settings).
3.  **Update `application-test.properties`**: Ensure this file does not contain any H2-specific properties. The R2DBC properties will be provided dynamically by Testcontainers.

```properties
# src/test/resources/application-test.properties
# This file can be kept for other test-specific properties if needed,
# but all spring.r2dbc.* and spring.flyway.* properties should be removed
# as they are now controlled by TestcontainersConfiguration.

spring.flyway.enabled=true
```

By following this plan, the `Planning-Service` will have a robust, reliable, and production-like testing environment. 