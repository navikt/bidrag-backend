package no.nav.bidrag.organisasjon

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import no.nav.bidrag.commons.security.api.EnableSecurityConfiguration
import no.nav.bidrag.commons.security.service.SecurityTokenService
import no.nav.bidrag.commons.web.CorrelationIdFilter
import no.nav.bidrag.commons.web.DefaultCorsFilter
import no.nav.bidrag.commons.web.UserMdcFilter
import no.nav.bidrag.commons.web.config.RestOperationsAzure
import no.nav.bidrag.organisasjon.consumer.SkjermingConsumer
import no.nav.bidrag.organisasjon.consumer.ldap.LdapBrukeroppslag
import no.nav.bidrag.organisasjon.consumer.ldap.LdapInnlogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.DefaultUriBuilderFactory
import javax.naming.Context

@Configuration
@OpenAPIDefinition(info = Info(title = "bidrag-organisasjon", version = "v1"), security = [SecurityRequirement(name = "bearer-key")])
@SecurityScheme(bearerFormat = "JWT", name = "bearer-key", scheme = "bearer", type = SecuritySchemeType.HTTP)
@EnableSecurityConfiguration
@EnableAsync
@Import(
    RestOperationsAzure::class,
    CorrelationIdFilter::class,
    DefaultCorsFilter::class,
    UserMdcFilter::class,
)
class BidragOrganisasjonConfiguration {
    @Value($$"${LDAP_URL}")
    private val ldapUrl: String? = null

    @Value($$"${LDAP_USERNAME}")
    private val ldapUsername: String? = null

    @Value($$"${LDAP_DOMAIN}")
    private val ldapDomain: String? = null

    @Value($$"${LDAP_PASSWORD}")
    private val ldapPassword: String? = null

    @Value($$"${LDAP_BASEDN}")
    private val ldapBasedn: String? = null

    @Bean
    fun skjermingConsumer(@Value($$"${SKJERMING_URL}") skjermingBaseUrl: String, securityTokenService: SecurityTokenService): SkjermingConsumer {
        val restTemplate = RestTemplate()
        restTemplate.uriTemplateHandler = DefaultUriBuilderFactory(skjermingBaseUrl)
        restTemplate.interceptors.add(securityTokenService.clientCredentialsTokenInterceptor("skjerming")!!)
        return SkjermingConsumer(restTemplate)
    }

    @Bean
    fun ldapBrukeroppslag(ldapInnlogging: LdapInnlogging): LdapBrukeroppslag {
        val environment =
            mapOf(
                Context.INITIAL_CONTEXT_FACTORY to "com.sun.jndi.ldap.LdapCtxFactory",
                Context.PROVIDER_URL to ldapUrl,
                Context.SECURITY_AUTHENTICATION to "simple",
                Context.SECURITY_CREDENTIALS to ldapPassword,
                Context.SECURITY_PRINCIPAL to "$ldapUsername@$ldapDomain",
            )
        val searchBase = "OU=Users,OU=NAV,OU=BusinessUnits,$ldapBasedn"
        return LdapBrukeroppslag(environment, ldapInnlogging, searchBase)
    }

    @Bean
    fun ldapInnlogging(): LdapInnlogging = LdapInnlogging()
}
