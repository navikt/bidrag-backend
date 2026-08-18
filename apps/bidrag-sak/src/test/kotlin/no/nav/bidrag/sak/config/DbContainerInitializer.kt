package no.nav.bidrag.sak.config

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.containers.Db2Container

class DbContainerInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        db2.start()
        TestPropertyValues
            .of(
                "spring.datasource.url=" + db2.jdbcUrl,
                "spring.datasource.username=" + db2.username,
                "spring.datasource.password=" + db2.password,
            ).applyTo(applicationContext.environment)
    }

    companion object {
        // Lazy because we only want it to be initialized when accessed
        private val db2: Db2Container by lazy {
            Db2Container("icr.io/db2_community/db2:latest")
                .withDatabaseName("BI464Q0")
                .withUsername("db2")
                .withPassword("test")
                .withInitScript("db/migration/V1__Schema.sql")
                .acceptLicense()
        }
    }
}
