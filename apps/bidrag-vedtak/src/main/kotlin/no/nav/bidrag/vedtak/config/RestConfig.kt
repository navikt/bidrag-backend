package no.nav.bidrag.vedtak.config

import no.nav.bidrag.commons.util.CustomJacksonHttpMessageConverter
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverters
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class RestConfig : WebMvcConfigurer {

    override fun configureMessageConverters(converters: HttpMessageConverters.ServerBuilder) {
        converters.addCustomConverter(
            CustomJacksonHttpMessageConverter(commonObjectmapper),
        )
    }
}
