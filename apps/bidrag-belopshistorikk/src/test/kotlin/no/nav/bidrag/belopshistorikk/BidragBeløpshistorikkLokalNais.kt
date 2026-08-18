package no.nav.bidrag.belopshistorikk

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Profile

@SpringBootApplication(
    exclude = [
        SecurityAutoConfiguration::class,
        ManagementWebSecurityAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class,
        ServletWebSecurityAutoConfiguration::class,
    ],
)
@EnableJwtTokenValidation(ignore = ["org.springdoc", "org.springframework"])
@ComponentScan(excludeFilters = [ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = [BidragBeløpshistorikk::class])])
@Profile("lokal-nais")
class BidragBeløpshistorikkLokalNais

fun main(args: Array<String>) {
    val app = SpringApplication(BidragBeløpshistorikkLokalNais::class.java)
    app.setAdditionalProfiles("lokal-nais", "lokal-nais-secrets", "nais")
    val ctx = app.run(*args)
    val port = ctx.environment.getProperty("server.port")
    println("##################################")
    println("Server startet på http://localhost:$port")
    println("##################################")
}
