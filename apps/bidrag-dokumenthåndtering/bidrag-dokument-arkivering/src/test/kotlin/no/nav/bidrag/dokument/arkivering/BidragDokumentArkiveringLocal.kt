package no.nav.bidrag.dokument.arkivering

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration

@EnableJwtTokenValidation(ignore = ["org.springframework", "org.springdoc"])
@SpringBootApplication(
    exclude = [
        SecurityAutoConfiguration::class,
        ManagementWebSecurityAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class,
        ServletWebSecurityAutoConfiguration::class,
    ],
)
class BidragDokumentArkiveringLocal

fun main(args: Array<String>) {
    val profile = if (args.size < 1) BidragDokumentArkivering.PROFILE_LOCAL else args[0]
    val app = SpringApplication(BidragDokumentArkiveringLocal::class.java)
    app.setAdditionalProfiles("nais", "local", "lokal-nais-secrets", "live")
    app.run(*args)
}
