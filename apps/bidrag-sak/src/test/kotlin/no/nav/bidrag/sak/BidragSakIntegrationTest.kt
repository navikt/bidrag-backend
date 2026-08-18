package no.nav.bidrag.sak

import no.nav.bidrag.commons.security.service.ClientConfigurationWellknownProperties
import no.nav.bidrag.sak.security.authentication.ldap.annotation.EnableBasicAndOidcAuthentication
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Import

@SpringBootApplication
@EnableBasicAndOidcAuthentication(
    ignore = ["springfox.documentation.swagger.web.ApiResourceController", "org.springframework"],
)
@ComponentScan(
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            value = [BidragSak::class],
        ), ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = [BidragSakLocal::class]),
    ],
)
@Import(ClientConfigurationWellknownProperties::class)
class BidragSakIntegrationTest

fun main(args: Array<String>) {
    val profile = if (args.isEmpty()) BidragSakProfiles.INTEGRATION_TEST else args[0]
    val app = SpringApplication(BidragSakIntegrationTest::class.java)
    app.setAdditionalProfiles(profile)
    app.run(*args)
}
