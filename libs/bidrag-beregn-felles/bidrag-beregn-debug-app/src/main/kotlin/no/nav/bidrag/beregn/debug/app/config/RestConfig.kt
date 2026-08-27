package no.nav.bidrag.beregn.debug.app.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.deser.YearMonthDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.bidrag.beregn.barnebidrag.service.orkestrering.HentLøpendeBidragService
import no.nav.bidrag.beregn.vedtak.Vedtaksfiltrering
import no.nav.bidrag.commons.security.api.EnableSecurityConfiguration
import no.nav.bidrag.commons.service.AppContext
import no.nav.bidrag.commons.service.organisasjon.EnableSaksbehandlernavnProvider
import no.nav.bidrag.commons.unleash.EnableUnleashFeatures
import no.nav.bidrag.commons.util.CustomJacksonHttpMessageConverter
import no.nav.bidrag.commons.util.IdentConsumer
import no.nav.bidrag.commons.web.config.RestOperationsAzure
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention
import org.springframework.http.converter.HttpMessageConverters
import org.springframework.retry.annotation.EnableRetry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Configuration
@EnableSecurityConfiguration
@EnableSaksbehandlernavnProvider
@EnableRetry
@Import(RestOperationsAzure::class, AppContext::class, Vedtaksfiltrering::class, IdentConsumer::class, HentLøpendeBidragService::class)
@EnableUnleashFeatures
class RestConfig : WebMvcConfigurer {
    @Bean
    fun clientRequestObservationConvention() = DefaultClientRequestObservationConvention()

    override fun configureMessageConverters(converters: HttpMessageConverters.ServerBuilder) {
        converters.addCustomConverter(
            CustomJacksonHttpMessageConverter(),
        )
    }
}
