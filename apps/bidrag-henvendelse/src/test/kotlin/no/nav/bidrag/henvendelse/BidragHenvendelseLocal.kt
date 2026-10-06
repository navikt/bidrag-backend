package no.nav.bidrag.henvendelse

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.test.context.ActiveProfiles

/**
 * Kjører appen lokalt. Se README for hvilke miljøvariabler som må settes.
 */
@ActiveProfiles("local")
@SpringBootApplication(
    exclude = [
        SecurityAutoConfiguration::class,
        ManagementWebSecurityAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class,
        ServletWebSecurityAutoConfiguration::class,
    ],
)
@EnableAspectJAutoProxy
@EnableJwtTokenValidation(ignore = ["org.springframework", "org.springdoc"])
class BidragHenvendelseLocal

fun main(args: Array<String>) {
    val app = SpringApplication(BidragHenvendelseLocal::class.java)
    app.setAdditionalProfiles("local", "lokal-nais", "nais", "lokal-nais-secrets")
    app.run(*args)
}
