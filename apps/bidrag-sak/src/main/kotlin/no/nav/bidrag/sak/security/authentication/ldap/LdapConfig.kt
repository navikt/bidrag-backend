package no.nav.bidrag.sak.security.authentication.ldap

import no.nav.bidrag.sak.BidragSakProfiles
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.ldap.repository.config.EnableLdapRepositories
import org.springframework.ldap.core.support.LdapContextSource

@Configuration
@EnableLdapRepositories(basePackages = ["no.nav.bidrag.sak.security.authentication.ldap"])
@Profile(BidragSakProfiles.LIVE, BidragSakProfiles.INTEGRATION_TEST)
class LdapConfig(
    @Value($$"${LDAP_URL}")
    var LDAP_URL: String,
    @Value($$"${LDAP_USERNAME}")
    var LDAP_USERNAME: String,
    @Value($$"${LDAP_PASSWORD}")
    var LDAP_PASSWORD: String,
    @Value($$"${LDAP_SERVICEUSER_BASEDN}")
    var LDAP_SERVICEUSER_BASEDN: String,
) {
    @Bean
    fun contextSource(): LdapContextSource = LdapContextSource().apply {
        setUrl(LDAP_URL)
        setBase(LDAP_SERVICEUSER_BASEDN)
        userDn = LDAP_USERNAME
        password = LDAP_PASSWORD
        setReferral("follow")
    }
}
