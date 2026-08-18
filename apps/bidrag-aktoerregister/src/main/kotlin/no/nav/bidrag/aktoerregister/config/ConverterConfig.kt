package no.nav.bidrag.aktoerregister.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ConversionServiceFactoryBean
import org.springframework.core.convert.converter.Converter

@Configuration
class ConverterConfig {
    @Bean
    fun conversionService(converters: Set<Converter<*, *>>): ConversionServiceFactoryBean = ConversionServiceFactoryBean().apply {
        this.setConverters(converters)
    }
}
