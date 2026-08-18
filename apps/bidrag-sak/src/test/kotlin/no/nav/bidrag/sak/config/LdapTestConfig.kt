package no.nav.bidrag.sak.config

import no.nav.bidrag.sak.BidragSakProfiles
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.ldap.autoconfigure.LdapProperties
import org.springframework.boot.ldap.autoconfigure.embedded.EmbeddedLdapProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.ldap.repository.config.EnableLdapRepositories
import org.springframework.ldap.core.support.LdapContextSource

@Configuration
@EnableLdapRepositories(basePackages = ["no.nav.bidrag.sak.security.authentication.ldap"])
@Profile(BidragSakProfiles.TEST)
class LdapTestConfig {
    @Bean
    @ConditionalOnProperty(name = ["spring.ldap.embedded.port", "spring.ldap.base"])
    fun contextSource(
        embeddedProperties: EmbeddedLdapProperties,
        ldapProperties: LdapProperties,
    ): LdapContextSource {
        val contextSource = LdapContextSource()
        contextSource.setBase(ldapProperties.base)
        contextSource.setUrl("ldap://localhost:" + embeddedProperties.port)
        return contextSource
    }
}
