package no.nav.bidrag.dokument

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.test.context.ActiveProfiles

const val TEST_PROFILE: kotlin.String = "test"
const val OIDC_TOKEN_TEST: kotlin.String = "oidc-token-test"

@SpringBootApplication(
    exclude = [
        SecurityAutoConfiguration::class,
        ManagementWebSecurityAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class,
        ServletWebSecurityAutoConfiguration::class,
    ],
)
@EnableJwtTokenValidation(ignore = ["springfox.documentation.swagger.web.ApiResourceController"])
@ActiveProfiles(TEST_PROFILE)
@ComponentScan(
    excludeFilters = [
        ComponentScan.Filter(
            type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
            value = arrayOf(BidragDokument::class),
        ),
    ],
)
class BidragDokumentTest

fun main(args: Array<String>) {
    val app = SpringApplication(BidragDokumentLocal::class.java)
    app.setAdditionalProfiles(if (args.isEmpty()) TEST_PROFILE else args[0])
    app.run(*args)
}
