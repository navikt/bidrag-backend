package no.nav.bidrag.admin.configuration

import no.nav.bidrag.commons.security.api.EnableSecurityConfiguration
import no.nav.bidrag.commons.util.CustomJacksonHttpMessageConverter
import no.nav.bidrag.commons.web.config.RestOperationsAzure
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.converter.HttpMessageConverters
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@EnableSecurityConfiguration
@Import(RestOperationsAzure::class)
class RestConfiguration : WebMvcConfigurer {
    override fun configureMessageConverters(converters: HttpMessageConverters.ServerBuilder) {
        converters.addCustomConverter(CustomJacksonHttpMessageConverter())
    }
}
