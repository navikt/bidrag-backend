package no.nav.bidrag.sak

import no.nav.bidrag.sak.security.authentication.ldap.annotation.EnableBasicAndOidcAuthentication
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration

@EnableBasicAndOidcAuthentication(ignore = ["org.springdoc", "org.springframework"])
@SpringBootApplication(
    exclude = [
        SecurityAutoConfiguration::class,
        ManagementWebSecurityAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class,
        ServletWebSecurityAutoConfiguration::class,
    ],
)
class BidragSak

fun main(args: Array<String>) {
    val profile = if (args.isEmpty()) BidragSakProfiles.LIVE else args[0]
    val app = SpringApplication(BidragSak::class.java)
    app.setAdditionalProfiles(profile)
    app.run(*args)
}
