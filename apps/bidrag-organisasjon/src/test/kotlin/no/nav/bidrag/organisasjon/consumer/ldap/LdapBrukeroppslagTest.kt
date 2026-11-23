package no.nav.bidrag.organisasjon.consumer.ldap

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import java.util.Hashtable
import javax.naming.LimitExceededException
import javax.naming.NamingEnumeration
import javax.naming.NamingException
import javax.naming.directory.BasicAttributes
import javax.naming.directory.SearchResult
import javax.naming.ldap.LdapContext

@ExtendWith(MockKExtension::class)
internal class LdapBrukeroppslagTest {
    private lateinit var baseSearch: String
    private lateinit var sbIdent: String

    private lateinit var ldap: LdapBrukeroppslag
    private val regularLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger
    private val secureLogger = LoggerFactory.getLogger("secureLogger") as Logger
    private val regularAppender = ListAppender<ILoggingEvent>()
    private val secureAppender = ListAppender<ILoggingEvent>()
    private var originalRegularLevel: Level? = null
    private var originalSecureLevel: Level? = null
    private var originalSecureAdditivity = false

    @RelaxedMockK
    private lateinit var ldapInnlogging: LdapInnlogging

    @RelaxedMockK
    private lateinit var environment: Hashtable<String, Any?>

    @RelaxedMockK
    private lateinit var context: LdapContext

    @BeforeEach
    fun init() {
        baseSearch = "ou=ServiceAccounts,dc=test,dc=local"
        ldap = LdapBrukeroppslag(environment, ldapInnlogging, baseSearch)
        sbIdent = "L999999"
        every { ldapInnlogging.lagLdapContext(environment) } returns context
        originalRegularLevel = regularLogger.level
        originalSecureLevel = secureLogger.level
        originalSecureAdditivity = secureLogger.isAdditive
        regularAppender.start()
        secureAppender.start()
        regularLogger.level = Level.WARN
        secureLogger.level = Level.WARN
        secureLogger.isAdditive = false
        regularLogger.addAppender(regularAppender)
        secureLogger.addAppender(secureAppender)
    }

    @AfterEach
    fun ryddOppLogging() {
        regularLogger.detachAppender(regularAppender)
        secureLogger.detachAppender(secureAppender)
        regularLogger.level = originalRegularLevel
        secureLogger.level = originalSecureLevel
        secureLogger.isAdditive = originalSecureAdditivity
        regularAppender.stop()
        secureAppender.stop()
    }

    @Test
    fun `Skal liste ut brukernavn når det er i resultatet`() {
        val attributes = BasicAttributes()
        attributes.put("displayName", KJENT_NAVN)
        attributes.put("cn", sbIdent)
        val resultat = SearchResult("CN=L999999,OU=ApplAccounts", null, attributes)
        val heleResultatet = SearchMock(listOf(resultat))
        every {
            context.search(
                eq(baseSearch),
                eq("(cn=L999999)"),
                any(),
            )
        } returns heleResultatet
        val saksbehandlerNavn = ldap.hentBrukernavn(sbIdent)
        ldap.getDisplayName(resultat) shouldBe KJENT_NAVN
        saksbehandlerNavn shouldBe KJENT_NAVN
    }

    @Test
    fun `Skal gi brukernavn ukjent og lage warning når søket gir ingen treff`() {
        val heleResultatet = SearchMock(emptyList())
        every {
            context.search(
                eq(baseSearch),
                eq("(cn=L999999)"),
                any(),
            )
        } returns heleResultatet
        val saksbehandlerNavn = ldap.hentBrukernavn(sbIdent)
        saksbehandlerNavn shouldBe null
        regularAppender.list.single().formattedMessage shouldBe "Navn på saksbehandler ikke funnet i LDAP"
        secureAppender.list.single().formattedMessage.contains(sbIdent) shouldBe true
    }

    @Test
    fun `Skal gi brukernavn ukjent og lage warning når søket gir to treff`() {
        every {
            context.search(eq(baseSearch), eq("(cn=L999999)"), any())
        } throws LimitExceededException("This is a test")
        val saksbehandlerNavn = ldap.hentBrukernavn(sbIdent)
        saksbehandlerNavn shouldBe null
        regularAppender.list.single().throwableProxy shouldBe null
        regularAppender.list.single().formattedMessage.contains(sbIdent) shouldBe false
        secureAppender.list.single().throwableProxy shouldNotBe null
        secureAppender.list.single().formattedMessage.contains(sbIdent) shouldBe true
    }

    @Test
    fun `Skal gi brukernavn ukjent og lage warning når søket gir NamingException`() {
        every {
            context.search(eq(baseSearch), eq("(cn=L999999)"), any())
        } throws NamingException("This is a test")
        val saksbehandlerNavn = ldap.hentBrukernavn(sbIdent)
        saksbehandlerNavn shouldBe null
        regularAppender.list.single().throwableProxy shouldBe null
        regularAppender.list.single().formattedMessage.contains(sbIdent) shouldBe false
        secureAppender.list.single().throwableProxy shouldNotBe null
        secureAppender.list.single().formattedMessage.contains(sbIdent) shouldBe true
    }

    @Test
    fun `Skal gi brukernavn ukjent og lage warning når svaret mangler forventet attributt`() {
        val attributes = BasicAttributes()
        attributes.put("cn", sbIdent)
        val resultat = SearchResult("CN=L999999,OU=ApplAccounts", null, attributes)
        val heleResultatet = SearchMock(listOf(resultat))
        every {
            context.search(eq(baseSearch), eq("(cn=L999999)"), any())
        } returns heleResultatet
        val saksbehandlerNavn = ldap.hentBrukernavn(sbIdent)
        saksbehandlerNavn shouldBe null
        regularAppender.list.single().formattedMessage.contains("attribute == null") shouldBe true
    }

    @Test
    fun `Skal gi brukernavn ukjent og lage warning når det søkes med spesialtegn`() {
        sbIdent = "L999999) or (cn=A*"
        val saksbehandlerNavn = ldap.hentBrukernavn(sbIdent)
        saksbehandlerNavn shouldBe null
        regularAppender.list.single().formattedMessage.contains(sbIdent) shouldBe false
        secureAppender.list.single().formattedMessage.contains(sbIdent) shouldBe true
    }

    private class SearchMock(private val resultList: List<SearchResult>) : NamingEnumeration<SearchResult> {
        private var index = 0

        override fun next(): SearchResult = throw IllegalArgumentException("Test---not implemented")

        override fun hasMore(): Boolean = throw IllegalArgumentException("Test---not implemented")

        override fun close() {}

        override fun hasMoreElements(): Boolean = index < resultList.size

        override fun nextElement(): SearchResult = resultList[index++]
    }

    companion object {
        private val KJENT_NAVN = "Lars Saksbehandler"
    }
}
