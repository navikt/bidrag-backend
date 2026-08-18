package no.nav.bidrag.sak

import no.nav.bidrag.sak.config.DbContainerInitializer
import no.nav.bidrag.sak.security.authentication.ldap.annotation.EnableBasicAndOidcAuthentication
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType

@SpringBootApplication(
    exclude = [
        SecurityAutoConfiguration::class,
        ManagementWebSecurityAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class,
        ServletWebSecurityAutoConfiguration::class,
    ],
)
@EnableBasicAndOidcAuthentication(ignore = ["org.springdoc", "org.springframework"])
class BidragSakLocal

fun main(args: Array<String>) {
    val profile = if (args.isEmpty()) BidragSakProfiles.TEST else args[0]
    val app = SpringApplication(BidragSakLocal::class.java)
    app.addInitializers(DbContainerInitializer())
    app.setAdditionalProfiles(profile)
    app.run(*args)
}
