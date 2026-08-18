package no.nav.bidrag.statistikk

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import no.nav.bidrag.commons.ExceptionLogger
import no.nav.bidrag.commons.security.api.EnableSecurityConfiguration
import no.nav.bidrag.commons.web.CorrelationIdFilter
import no.nav.bidrag.commons.web.DefaultCorsFilter
import no.nav.bidrag.commons.web.UserMdcFilter
import no.nav.bidrag.commons.web.config.RestOperationsAzure
import no.nav.bidrag.statistikk.hendelse.KafkaVedtakHendelseListener
import no.nav.bidrag.statistikk.service.BehandleHendelseService
import no.nav.bidrag.statistikk.service.JsonMapperService
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention

const val LIVE_PROFILE = "live"
const val LOKAL_NAIS_PROFILE = "lokal-nais"

@Configuration
@EnableSecurityConfiguration
@OpenAPIDefinition(
    info = Info(title = "bidrag-statistikk", version = "v1"),
    security = [SecurityRequirement(name = "bearer-key")],
)
@EnableJwtTokenValidation(ignore = ["org.springframework", "org.springdoc"])
@SecurityScheme(
    bearerFormat = "JWT",
    name = "bearer-key",
    scheme = "bearer",
    type = SecuritySchemeType.HTTP,
)
@EnableAspectJAutoProxy
@Import(
    CorrelationIdFilter::class,
    UserMdcFilter::class,
    DefaultCorsFilter::class,
    RestOperationsAzure::class,
)
class BidragStatistikkConfig {

    @Bean
    fun exceptionLogger(): ExceptionLogger = ExceptionLogger(BidragStatistikk::class.java.simpleName)

    @Bean
    fun clientRequestObservationConvention() = DefaultClientRequestObservationConvention()
}

@Configuration
@EnableSecurityConfiguration
@Import(RestOperationsAzure::class)
class RestConfig

val LOGGER = LoggerFactory.getLogger(KafkaConfig::class.java)

@Configuration
@Profile(LIVE_PROFILE)
class KafkaConfig {
    @Bean
    fun vedtakHendelseListener(jsonMapperService: JsonMapperService, behandeHendelseService: BehandleHendelseService) = KafkaVedtakHendelseListener(jsonMapperService, behandeHendelseService)
}
