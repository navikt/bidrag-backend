package no.nav.bidrag.dokument.produksjon

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration

private val log = KotlinLogging.logger {}

val SIKKER_LOGG = KotlinLogging.logger("secureLogger")

@SpringBootApplication(
    exclude = [
        SecurityAutoConfiguration::class,
        ManagementWebSecurityAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class,
        ServletWebSecurityAutoConfiguration::class,
    ],
)
class App

fun main(args: Array<String>) {
    try {
        runApplication<App>(*args)
    } catch (e: Exception) {
        log.error(e) { "Feil ved oppstart av applikasjon" }
    } catch (e: Error) {
        log.error(e) { "Feil ved oppstart av applikasjon" }
    }
}
