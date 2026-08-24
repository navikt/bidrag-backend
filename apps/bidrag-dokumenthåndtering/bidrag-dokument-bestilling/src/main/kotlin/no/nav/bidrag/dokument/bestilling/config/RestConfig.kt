package no.nav.bidrag.dokument.bestilling.config

import no.nav.bidrag.commons.security.api.EnableSecurityConfiguration
import no.nav.bidrag.commons.util.CustomJacksonHttpMessageConverter
import no.nav.bidrag.commons.web.config.RestOperationsAzure
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention
import org.springframework.http.converter.HttpMessageConverters
import org.springframework.retry.annotation.EnableRetry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@EnableSecurityConfiguration
@EnableRetry
@Import(RestOperationsAzure::class)
class RestConfig : WebMvcConfigurer {
    override fun configureMessageConverters(converters: HttpMessageConverters.ServerBuilder) {
        converters.addCustomConverter(
            CustomJacksonHttpMessageConverter(
                commonObjectmapper,
            ),
        )
    }

    @Bean
    fun clientRequestObservationConvention() = DefaultClientRequestObservationConvention()
}
