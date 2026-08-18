package no.nav.bidrag.belopshistorikk

import no.nav.bidrag.commons.util.CustomJacksonHttpMessageConverter
import no.nav.bidrag.commons.web.CorrelationIdFilter
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.http.converter.HttpMessageConverters
import org.springframework.web.client.RestTemplate
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class RestTemplateConfiguration : WebMvcConfigurer {
    @Bean
    @Scope("prototype")
    fun restTemplate(): RestTemplate {
        val httpHeaderRestTemplate = HttpHeaderRestTemplate()
        httpHeaderRestTemplate.addHeaderGenerator(
            CorrelationIdFilter.CORRELATION_ID_HEADER,
        ) { CorrelationIdFilter.fetchCorrelationIdForThread() }
        return httpHeaderRestTemplate
    }

    override fun configureMessageConverters(converters: HttpMessageConverters.ServerBuilder) {
        converters.addCustomConverter(CustomJacksonHttpMessageConverter(commonObjectmapper))
    }
}
