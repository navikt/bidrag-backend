package no.nav.bidrag.commons.web.config

import no.nav.bidrag.commons.util.CustomJacksonHttpMessageConverter
import no.nav.bidrag.commons.web.interceptor.BearerTokenClientInterceptor
import no.nav.bidrag.commons.web.interceptor.ServiceUserAuthTokenInterceptor
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Scope
import org.springframework.http.converter.ByteArrayHttpMessageConverter
import org.springframework.http.converter.FormHttpMessageConverter
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.ResourceHttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter
import org.springframework.http.converter.xml.MarshallingHttpMessageConverter
import org.springframework.web.client.RestTemplate

@Suppress("SpringFacetCodeInspection")
@Configuration
@Import(RestTemplateBuilderBean::class, BearerTokenClientInterceptor::class, ServiceUserAuthTokenInterceptor::class)
class RestOperationsAzure {
    @Bean("azure")
    @Scope("prototype")
    fun restOperationsJwtBearer(
        restTemplateBuilder: RestTemplateBuilder,
        bearerTokenClientInterceptor: BearerTokenClientInterceptor,
    ): RestTemplate =
        restTemplateBuilder
            .additionalInterceptors(bearerTokenClientInterceptor)
            .messageConverters(createMessageConverters())
            .build()

    @Bean("azureService")
    @Scope("prototype")
    fun restOperationsServiceJwtBearer(
        restTemplateBuilder: RestTemplateBuilder,
        bearerTokenClientInterceptor: ServiceUserAuthTokenInterceptor,
    ): RestTemplate =
        restTemplateBuilder
            .additionalInterceptors(bearerTokenClientInterceptor)
            .messageConverters(createMessageConverters())
            .build()

    /**
     * Creates a complete list of message converters that replaces Spring defaults.
     * Order matters — more specific converters (ByteArray, String) must come before
     * the JSON converter to avoid [CustomJacksonHttpMessageConverter] intercepting
     * binary/string responses it cannot handle.
     *
     * [ByteArrayHttpMessageConverter] is configured to also accept [MediaType.APPLICATION_PDF]
     * so that PDF responses can be consumed as raw bytes.
     */
    private fun createMessageConverters(): List<HttpMessageConverter<*>> =
        listOf(
            ByteArrayHttpMessageConverter(),
            StringHttpMessageConverter(),
            ResourceHttpMessageConverter(false),
            AllEncompassingFormHttpMessageConverter(),
            CustomJacksonHttpMessageConverter(commonObjectmapper),
            Jaxb2RootElementHttpMessageConverter(),
            MarshallingHttpMessageConverter(),
            FormHttpMessageConverter(),
        )
}
