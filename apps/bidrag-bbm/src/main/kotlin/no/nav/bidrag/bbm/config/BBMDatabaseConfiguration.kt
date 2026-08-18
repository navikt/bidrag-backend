package no.nav.bidrag.bbm.config

import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties
import org.springframework.boot.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
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
    entityManagerFactoryRef = "bbmEntityManagerFactory",
    transactionManagerRef = "bbmTransactionManager",
    basePackages = ["no.nav.bidrag.bbm.persistence.bbm"],
)
class BBMDatabaseConfiguration {
    @Bean
    @ConfigurationProperties("spring.datasource.bbm")
    fun bbmDataSourceProperties(): DataSourceProperties = DataSourceProperties()

    @Bean
    @Primary
    fun bbmDataSource(): DataSource = bbmDataSourceProperties()
        .initializeDataSourceBuilder()
        .build()

    @Bean
    fun bbmJdbcTemplate(
        @Qualifier("bbmDataSource") dataSource: DataSource,
    ): JdbcTemplate = JdbcTemplate(dataSource)

    @Primary
    @Bean
    fun bbmEntityManagerFactory(
        builder: EntityManagerFactoryBuilder,
        @Qualifier("bbmDataSource") dataSource: DataSource,
    ): LocalContainerEntityManagerFactoryBean = builder
        .dataSource(dataSource)
        .persistenceUnit("bbm")
        .packages("no.nav.bidrag.bbm.persistence.bbm")
        .build()

    @Primary
    @Bean
    fun bbmTransactionManager(
        @Qualifier("bbmEntityManagerFactory") entityManagerFactory: EntityManagerFactory,
    ): PlatformTransactionManager = JpaTransactionManager(entityManagerFactory)
}
