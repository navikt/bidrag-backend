package no.nav.bidrag.person

import no.nav.bidrag.commons.security.api.EnableSecurityConfiguration
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration

@EnableSecurityConfiguration
@SpringBootApplication(
    exclude = [
        SecurityAutoConfiguration::class,
        ManagementWebSecurityAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class,
        ServletWebSecurityAutoConfiguration::class,
    ],
)
@EnableJwtTokenValidation(ignore = ["org.springframework", "org.springdoc"])
class BidragPerson {
    companion object {
        val SECURE_LOGGER: Logger = LoggerFactory.getLogger("secureLogger")
    }
}

fun main(args: Array<String>) {
    val profile = if (args.isEmpty()) BidragPersonConfiguration.LIVE_PROFILE else args[0]
    val app = SpringApplication(BidragPerson::class.java)
    app.setAdditionalProfiles(profile)
    app.run(*args)
}
