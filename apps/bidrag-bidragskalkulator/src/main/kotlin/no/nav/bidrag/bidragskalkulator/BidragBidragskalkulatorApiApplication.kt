package no.nav.bidrag.bidragskalkulator

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration

@SpringBootApplication(
    exclude = [
        SecurityAutoConfiguration::class,
        ManagementWebSecurityAutoConfiguration::class,
        ServletWebSecurityAutoConfiguration::class,
    ],
)
@EnableJwtTokenValidation(ignore = ["org.springdoc", "org.springframework", "/actuator/**"])
class BidragBidragskalkulatorApiApplication

fun main(args: Array<String>) {
    runApplication<BidragBidragskalkulatorApiApplication>(*args)
}
