package no.nav.bidrag.sak.config

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.deser.YearMonthDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.security.SecuritySchemes
import no.nav.bidrag.commons.ExceptionLogger
import no.nav.bidrag.commons.logging.audit.AuditAdvice
import no.nav.bidrag.commons.security.service.ClientConfigurationWellknownProperties
import no.nav.bidrag.commons.service.AppContext
import no.nav.bidrag.commons.util.CustomJacksonHttpMessageConverter
import no.nav.bidrag.commons.util.IdentConsumer
import no.nav.bidrag.commons.web.CorrelationIdFilter
import no.nav.bidrag.commons.web.DefaultCorsFilter
import no.nav.bidrag.commons.web.MdcFilter
import no.nav.bidrag.commons.web.UserMdcFilter
import no.nav.bidrag.commons.web.config.RestOperationsAzure
import no.nav.bidrag.commons.web.config.RestTemplateBuilderBean
import no.nav.bidrag.sak.BidragSak
import no.nav.bidrag.transport.felles.commonObjectmapper
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.client.observation.ClientRequestObservationConvention
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention
import org.springframework.http.converter.HttpMessageConverters
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Configuration
@SecuritySchemes(
    SecurityScheme(bearerFormat = "JWT", name = "bearer-key", scheme = "bearer", type = SecuritySchemeType.HTTP),
    SecurityScheme(name = "basic-auth", scheme = "basic", type = SecuritySchemeType.HTTP),
)
@OpenAPIDefinition(info = Info(title = "bidrag-sak", version = "v1"), security = [SecurityRequirement(name = "bearer-key")])
@EnableOAuth2Client(cacheEnabled = true)
@ConfigurationPropertiesScan
@Import(
    RestTemplateBuilderBean::class,
    RestOperationsAzure::class,
    MdcFilter::class,
    UserMdcFilter::class,
    AuditAdvice::class,
    DefaultCorsFilter::class,
    ClientConfigurationWellknownProperties::class,
    IdentConsumer::class,
    AppContext::class,
)
class BidragSakConfig {
    @Bean
    fun kotlinModule(): KotlinModule = KotlinModule.Builder().build()

    @Bean
    fun exceptionLogger(): ExceptionLogger = ExceptionLogger(BidragSak::class.java.simpleName)

    @Bean
    fun bidragSakCorrelationIdFilter(): CorrelationIdFilter = CorrelationIdFilter()

    @Bean
    fun bidragCorsFilter(): DefaultCorsFilter = DefaultCorsFilter()

    @Bean
    fun clientRequestObservationConvention(): ClientRequestObservationConvention = DefaultClientRequestObservationConvention()
}

@Configuration
class RestConfig : WebMvcConfigurer {
    override fun configureMessageConverters(converters: HttpMessageConverters.ServerBuilder) {
        converters.addCustomConverter(
            CustomJacksonHttpMessageConverter(
                commonObjectmapper.registerModules(
                    KotlinModule.Builder().build(),
                    JavaTimeModule()
                        .addDeserializer(
                            YearMonth::class.java,
                            // Denne trengs for å parse år over 9999 riktig.
                            YearMonthDeserializer(DateTimeFormatter.ofPattern("u-MM")),
                        ).addSerializer(
                            LocalDate::class.java,
                            // Denne trengs for å skrive ut år over 9999 riktig.
                            LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        ),
                ),
            ),
        )
    }
}
