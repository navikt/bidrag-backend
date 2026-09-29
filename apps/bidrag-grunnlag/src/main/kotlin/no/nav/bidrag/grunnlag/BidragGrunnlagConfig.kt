package no.nav.bidrag.grunnlag

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import no.nav.bidrag.commons.cache.EnableUserCache
import no.nav.bidrag.commons.security.api.EnableSecurityConfiguration
import no.nav.bidrag.commons.service.organisasjon.EnableSaksbehandlernavnProvider
import no.nav.bidrag.commons.web.CorrelationIdFilter
import no.nav.bidrag.commons.web.DefaultCorsFilter
import no.nav.bidrag.commons.web.UserMdcFilter
import no.nav.bidrag.commons.web.config.RestOperationsAzure
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.retry.annotation.EnableRetry
import org.springframework.web.client.RestTemplate
import java.time.Duration

const val LIVE_PROFILE = "live"

@Configuration
@OpenAPIDefinition(
    info = Info(title = "bidrag-grunnlag", version = "v1"),
    security = [SecurityRequirement(name = "bearer-key")],
)
@EnableJwtTokenValidation
@SecurityScheme(
    bearerFormat = "JWT",
    name = "bearer-key",
    scheme = "bearer",
    type = SecuritySchemeType.HTTP,
)
@EnableUserCache
@EnableRetry
@EnableSecurityConfiguration
@EnableSaksbehandlernavnProvider
@Import(CorrelationIdFilter::class, UserMdcFilter::class, DefaultCorsFilter::class, RestOperationsAzure::class)
class BidragGrunnlagConfig {

    @Bean
    fun lockProvider(jdbcTemplate: JdbcTemplate): LockProvider = JdbcTemplateLockProvider(
        JdbcTemplateLockProvider.Configuration.builder()
            .withJdbcTemplate(jdbcTemplate)
            .usingDbTime()
            .build(),
    )

    @Bean
    fun clientRequestObservationConvention() = DefaultClientRequestObservationConvention()

    @Bean
    @Primary
    fun restTemplate(): RestTemplate = RestTemplate()

    @Bean("ecbRestTemplate")
    fun ecbRestTemplate(): RestTemplate = RestTemplate(
        SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(Duration.ofSeconds(5))
            setReadTimeout(Duration.ofSeconds(10))
        },
    )

    @Bean("norgesBankRestTemplate")
    fun norgesBankRestTemplate(): RestTemplate = RestTemplate(
        SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(Duration.ofSeconds(5))
            setReadTimeout(Duration.ofSeconds(10))
        },
    )
}
