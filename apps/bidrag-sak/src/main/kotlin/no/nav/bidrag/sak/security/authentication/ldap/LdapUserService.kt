package no.nav.bidrag.sak.security.authentication.ldap

import org.springframework.ldap.NamingException
import org.springframework.ldap.core.support.LdapContextSource
import org.springframework.stereotype.Service

@Service
class LdapUserService(
    val ldapContextSource: LdapContextSource,
    val ldapUserRepository: LdapUserRepository,
) {
    fun authenticate(
        username: String,
        userPassword: String,
        groups: List<String>,
    ): Boolean {
        val user = ldapUserRepository.findByUsername(username)
        for (group in groups) {
            if (user.memberOfString.indexOf(group) > 0) {
                val userPrincipalName = user.userPrincipalName
                return try {
                    // getContext returnerer aldri null (se spring-ldap-core sin @NullMarked-kontrakt).
                    // Ved mislykket autentisering kastes det i stedet en NamingException.
                    ldapContextSource.getContext(userPrincipalName, userPassword)
                    true
                } catch (ex: NamingException) {
                    false
                }
            }
        }
        return false
    }
}
