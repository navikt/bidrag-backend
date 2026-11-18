package no.nav.bidrag.oppgave.config

import no.nav.bidrag.texas.NaisTokenProperties
import no.nav.bidrag.texas.NaisTokenService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.validation.annotation.Validated

@Configuration
@EnableConfigurationProperties(NaisTokenProperties::class)
@Validated
class NaisTokenConfig {
    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun tokenService(naisTokenProperties: NaisTokenProperties): NaisTokenService {
        val authenticatedUserTokenSupplier: () -> String = {
            when (val authentication = SecurityContextHolder.getContext().authentication) {
                is JwtAuthenticationToken -> authentication.token.tokenValue
                else -> throw IllegalStateException("Unknown Authenticated user ${authentication?.javaClass?.name}")
            }
        }
        log.info("Setting up texas with oboEndpoint=${naisTokenProperties.exchange.endpoint} and m2mEndpoint=${naisTokenProperties.endpoint}")
        return NaisTokenService(
            naisTokenProperties = naisTokenProperties,
            tokenSupplier = authenticatedUserTokenSupplier,
        )
    }
}
