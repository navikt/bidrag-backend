package no.nav.bidrag.organisasjon.config

import no.nav.bidrag.commons.util.CustomJacksonHttpMessageConverter
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.observation.ClientRequestObservationConvention
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention
import org.springframework.http.converter.HttpMessageConverters
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class RestConfig : WebMvcConfigurer {

    override fun configureMessageConverters(converters: HttpMessageConverters.ServerBuilder) {
        converters.addCustomConverter(
            CustomJacksonHttpMessageConverter(
                commonObjectmapper,
            ),
        )
    }

    @Bean
    fun clientRequestObservationConvention(): ClientRequestObservationConvention = DefaultClientRequestObservationConvention()
}
