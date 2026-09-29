package no.nav.bidrag.behandling

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.postgresql.PostgreSQLContainer

// Postgres-containere som lever hele JVM-en og stoppes av Testcontainers (Ryuk) når JVM-en avsluttes.
// TestContainerRunner og KontrollerTestRunner har hver sin database fordi testene deres ikke rydder etter hverandre.
object TestPostgres {
    val testContainerRunnerDb: PostgreSQLContainer by lazy { startContainer() }
    val kontrollerTestRunnerDb: PostgreSQLContainer by lazy { startContainer() }

    private fun startContainer(): PostgreSQLContainer =
        PostgreSQLContainer("postgres:15-alpine").apply {
            withDatabaseName("bidrag-behandling")
            withUsername("cloudsqliamuser")
            withPassword("admin")
            withInitScript("db/init.sql")
            start()
        }

    fun registrerProperties(
        registry: DynamicPropertyRegistry,
        container: PostgreSQLContainer,
    ) {
        registry.add("spring.jpa.database") { "POSTGRESQL" }
        registry.add("spring.datasource.type") { "com.zaxxer.hikari.HikariDataSource" }
        registry.add("spring.flyway.enabled") { true }
        registry.add("spring.flyway.locations") { "classpath:/db/migration" }
        registry.add("spring.datasource.url", container::getJdbcUrl)
        registry.add("spring.datasource.password", container::getPassword)
        registry.add("spring.datasource.username", container::getUsername)
    }
}
