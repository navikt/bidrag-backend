package no.nav.bidrag.bbm.config

import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties
import org.springframework.boot.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    entityManagerFactoryRef = "bisysEntityManagerFactory",
    transactionManagerRef = "bisysTransactionManager",
    basePackages = ["no.nav.bidrag.bbm.persistence.bisys"],
)
class BisysDatabaseConfiguration {
    @Bean
    @ConfigurationProperties("spring.datasource.bisys")
    fun bisysDataSourceProperties(): DataSourceProperties = DataSourceProperties()

    @Bean
    fun bisysDataSource(): DataSource = bisysDataSourceProperties()
        .initializeDataSourceBuilder()
        .build()

    @Bean
    fun bisysJdbcTemplate(
        @Qualifier("bisysDataSource") dataSource: DataSource,
    ): JdbcTemplate = JdbcTemplate(dataSource)

    @Bean
    fun bisysEntityManagerFactory(
        builder: EntityManagerFactoryBuilder,
        @Qualifier("bisysDataSource") dataSource: DataSource,
    ): LocalContainerEntityManagerFactoryBean = builder
        .dataSource(dataSource)
        .persistenceUnit("bisys")
        .packages("no.nav.bidrag.bbm.persistence.bisys")
        .build()

    @Bean
    fun bisysTransactionManager(
        @Qualifier("bisysEntityManagerFactory") entityManagerFactory: EntityManagerFactory,
    ): PlatformTransactionManager = JpaTransactionManager(entityManagerFactory)
}
