package no.nav.bidrag.samhandler.config

import org.springframework.boot.security.autoconfigure.SecurityProperties
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.web.client.RestOperations
import org.springframework.web.client.RestTemplate

@TestConfiguration
class SamhandlerTestConfig {
    @Bean("azure")
    fun azureRestOperations(): RestOperations = RestTemplate()

    @Bean
    fun securityProperties() = SecurityProperties()
}
