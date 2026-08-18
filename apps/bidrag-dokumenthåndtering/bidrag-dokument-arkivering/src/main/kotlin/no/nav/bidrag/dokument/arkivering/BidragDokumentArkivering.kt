package no.nav.bidrag.dokument.arkivering

import no.nav.bidrag.dokument.arkivering.BidragDokumentArkivering.Companion.PROFILE_LIVE
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration
import org.springframework.retry.annotation.EnableRetry

@EnableJwtTokenValidation(ignore = ["org.springframework", "org.springdoc"])
@EnableRetry
@SpringBootApplication(
    exclude = [
        SecurityAutoConfiguration::class,
        ManagementWebSecurityAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class,
        ServletWebSecurityAutoConfiguration::class,
    ],
)
class BidragDokumentArkivering {
    companion object {
        const val PROFILE_INTEGRATION_TEST = "integration-test"
        const val PROFILE_LIVE = "live"
        const val PROFILE_LOCAL = "local"
        const val PROFILE_TEST = "test"
        const val PROFILE_SECURED_TEST = "secured-test"
    }
}

fun main(args: Array<String>) {
    val profile = if (args.size < 1) PROFILE_LIVE else args[0]
    val app = SpringApplication(BidragDokumentArkivering::class.java)
    app.setAdditionalProfiles(profile)
    app.run(profile)
}
