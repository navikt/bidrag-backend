package no.nav.bidrag.sak.security.authentication.ldap

import org.springframework.data.ldap.repository.LdapRepository
import org.springframework.stereotype.Repository

@Repository
interface LdapUserRepository : LdapRepository<LdapUser> {
    fun findByUsername(username: String): LdapUser
}
