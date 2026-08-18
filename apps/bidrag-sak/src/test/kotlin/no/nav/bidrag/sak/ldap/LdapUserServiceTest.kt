package no.nav.bidrag.sak.ldap

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.bidrag.sak.security.authentication.ldap.LdapUser
import no.nav.bidrag.sak.security.authentication.ldap.LdapUserRepository
import no.nav.bidrag.sak.security.authentication.ldap.LdapUserService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.ldap.AuthenticationException
import org.springframework.ldap.core.support.LdapContextSource
import org.springframework.ldap.test.DummyDirContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import javax.naming.CompositeName

@ExtendWith(SpringExtension::class)
class LdapUserServiceTest {
    private val ldapContextSource: LdapContextSource = mockk()

    private val ldapUserRepository: LdapUserRepository = mockk()

    private var ldapUserService = LdapUserService(ldapContextSource, ldapUserRepository)

    @Test
    fun whenAuthenticateWithCorrectCredentials_thenSuccessfulLogin() {
        val groups = listOf(PIP_AD_GROUP)
        val userPrincipalName = "$USER1@TEST.LOCAL"
        every { ldapUserRepository.findByUsername(USER1) }
            .returns(LdapUser(CompositeName("Name"), USER1, userPrincipalName, MEMBEROF, "sd"))
        every { ldapContextSource.getContext(userPrincipalName, USER1_PWD) }
            .returns(DummyDirContext())
        val isValid = ldapUserService.authenticate(USER1, USER1_PWD, groups)
        isValid shouldBe true
    }

    @Test
    fun whenAuthenticateWithWrongCredentials_thenLoginFails() {
        val groups = listOf(PIP_AD_GROUP)
        val userPrincipalName = "$USER1@TEST.LOCAL"
        every { ldapUserRepository.findByUsername(USER1) }
            .returns(LdapUser(CompositeName("Name"), USER1, userPrincipalName, MEMBEROF, "sd"))
        every { ldapContextSource.getContext(userPrincipalName, USER1_PWD) }
            .throws(AuthenticationException())
        val isValid = ldapUserService.authenticate(USER1, USER1_PWD, groups)
        isValid shouldBe false
    }

    @Test
    fun whenGroupMembershipIsIncorrect_thenLoginFails() {
        val groups = listOf(PIP_AD_GROUP)
        val userPrincipalName = "$USER1@TEST.LOCAL"
        val memberOf = listOf("stateTroopers")
        every { ldapUserRepository.findByUsername(USER1) }
            .returns(LdapUser(CompositeName("Name"), USER1, userPrincipalName, memberOf, "sd"))
        every { ldapContextSource.getContext(userPrincipalName, USER1_PWD) }
            .returns(DummyDirContext())
        val isValid = ldapUserService.authenticate(USER1, USER1_PWD, groups)
        isValid shouldBe false
    }

    companion object {
        private const val USER1 = "USER1"
        private const val USER1_PWD = "a"
        private const val PIP_AD_GROUP = "0000-GA-PIP_BIDRAGSAK"
        private val MEMBEROF = listOf("cn=$PIP_AD_GROUP")
    }
}
