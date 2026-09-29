package no.nav.bidrag.behandling

import com.ninjasquad.springmockk.MockkSpyBean
import no.nav.bidrag.commons.service.sjablon.SjablonService
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.postgresql.PostgreSQLContainer

@ActiveProfiles(value = ["test", "testcontainer"])
class TestContainerRunner : SpringTestRunner() {
    // Delt spy slik at tester kan stubbe sjabloner uten å opprette egen Spring-kontekst
    @MockkSpyBean
    lateinit var sjablonService: SjablonService

    companion object {
        @JvmStatic
        protected val postgreSqlDb =
            PostgreSQLContainer("postgres:latest").apply {
                withDatabaseName("bidrag-behandling")
                withUsername("cloudsqliamuser")
                withPassword("admin")
                withInitScript("db/init.sql")
                start()
            }

        @Suppress("unused")
        @JvmStatic
        @DynamicPropertySource
        fun postgresqlProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.jpa.database") { "POSTGRESQL" }
            registry.add("spring.datasource.type") { "com.zaxxer.hikari.HikariDataSource" }
            registry.add("spring.flyway.enabled") { true }
            registry.add("spring.flyway.locations") { "classpath:/db/migration" }
            registry.add("spring.datasource.url", postgreSqlDb::getJdbcUrl)
            registry.add("spring.datasource.password", postgreSqlDb::getPassword)
            registry.add("spring.datasource.username", postgreSqlDb::getUsername)
        }
    }
}
