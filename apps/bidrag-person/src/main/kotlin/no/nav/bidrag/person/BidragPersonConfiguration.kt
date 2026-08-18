package no.nav.bidrag.person

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.deser.YearMonthDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import jakarta.annotation.PostConstruct
import no.nav.bidrag.commons.ExceptionLogger
import no.nav.bidrag.commons.service.KodeverkProvider
import no.nav.bidrag.commons.util.CustomJacksonHttpMessageConverter
import no.nav.bidrag.commons.web.CorrelationIdFilter
import no.nav.bidrag.commons.web.DefaultCorsFilter
import no.nav.bidrag.commons.web.UserMdcFilter
import no.nav.bidrag.commons.web.config.RestTemplateBuilderBean
import no.nav.bidrag.person.BidragPersonConfiguration.Companion.LIVE_PROFILE
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.http.converter.HttpMessageConverters
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Configuration
@OpenAPIDefinition(info = Info(title = "bidrag-person", version = "v1"), security = [SecurityRequirement(name = "bearer-key")])
@SecurityScheme(bearerFormat = "JWT", name = "bearer-key", scheme = "bearer", type = SecuritySchemeType.HTTP)
@Import(
    RestTemplateBuilderBean::class,
)
class BidragPersonConfiguration(@Value("\${KODEVERK_URL}") kodeverkUrl: String) {
    init {
        KodeverkProvider.initialiser(kodeverkUrl)
    }

    @Bean
    fun correlationIdFilter() = CorrelationIdFilter()

    @Bean
    fun defaultCorsFilter() = DefaultCorsFilter()

    @Bean
    fun userMdcFilter() = UserMdcFilter()

    @Bean
    fun exceptionLogger() = ExceptionLogger(BidragPerson::class.java.simpleName)

    companion object {
        const val LIVE_PROFILE = "live"
    }
}

@Profile(LIVE_PROFILE)
@Configuration
class InitKodeverkCache {
    @PostConstruct
    fun initKodeverkCache() {
        KodeverkProvider.initialiserKodeverkCache()
    }
}

@Configuration
class RestConfig : WebMvcConfigurer {

    override fun configureMessageConverters(converters: HttpMessageConverters.ServerBuilder) {
        converters.addCustomConverter(
            CustomJacksonHttpMessageConverter(
                commonObjectmapper
                    .registerModules(
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
