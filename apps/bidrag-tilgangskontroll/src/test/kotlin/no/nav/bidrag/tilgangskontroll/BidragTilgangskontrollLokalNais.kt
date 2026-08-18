package no.nav.bidrag.tilgangskontroll

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration

@SpringBootApplication(
    exclude = [
        SecurityAutoConfiguration::class,
        ManagementWebSecurityAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class,
        ServletWebSecurityAutoConfiguration::class,
    ],
)
@EnableJwtTokenValidation(ignore = ["org.springframework", "org.springdoc"])
class BidragTilgangskontrollLokalNais

fun main(args: Array<String>) {
    val app = SpringApplication(BidragTilgangskontrollLokalNais::class.java)
    app.setAdditionalProfiles("lokal-nais", "lokal-nais-secrets", "lokal", "nais")
    app.run(*args)
}
