package no.nav.bidrag.organisasjon.consumer.ldap

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
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
    private lateinit var logCaptor: CapturingSlot<ILoggingEvent>
    private lateinit var sbIdent: String

    private lateinit var ldap: LdapBrukeroppslag

    @RelaxedMockK
    private lateinit var appenderMock: Appender<ILoggingEvent>

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
        mockLogAppender()
        logCaptor = slot()
    }

    private fun mockLogAppender() {
        val logger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger
        every { appenderMock.name } returns "MOCK"
        every { appenderMock.isStarted } returns true
        logger.addAppender(appenderMock)
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
        verify { appenderMock.doAppend(capture(logCaptor)) }
        val loggingEvent = logCaptor.captured
        saksbehandlerNavn shouldBe null
        loggingEvent shouldNotBe null
        loggingEvent.formattedMessage.contains("Navn på saksbehandler for ident $sbIdent ikke funnet (2)") shouldBe true
    }

    @Test
    fun `Skal gi brukernavn ukjent og lage warning når søket gir to treff`() {
        every {
            context.search(eq(baseSearch), eq("(cn=L999999)"), any())
        } throws LimitExceededException("This is a test")
        val saksbehandlerNavn = ldap.hentBrukernavn(sbIdent)
        verify { appenderMock.doAppend(capture(logCaptor)) }
        val loggingEvent = logCaptor.captured
        saksbehandlerNavn shouldBe null
        loggingEvent shouldNotBe null
        loggingEvent.formattedMessage.contains(
            "Navn på saksbehandler for ident $sbIdent ikke funnet (LimitExceededException)",
        ) shouldBe true
    }

    @Test
    fun `Skal gi brukernavn ukjent og lage warning når søket gir NamingException`() {
        every {
            context.search(eq(baseSearch), eq("(cn=L999999)"), any())
        } throws NamingException("This is a test")
        val saksbehandlerNavn = ldap.hentBrukernavn(sbIdent)
        verify { appenderMock.doAppend(capture(logCaptor)) }
        val loggingEvent = logCaptor.captured
        saksbehandlerNavn shouldBe null
        loggingEvent shouldNotBe null
        loggingEvent.formattedMessage.contains("Navn på saksbehandler for ident $sbIdent ikke funnet (NamingException)") shouldBe true
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
        verify { appenderMock.doAppend(capture(logCaptor)) }
        val loggingEvent = logCaptor.captured
        saksbehandlerNavn shouldBe null
        loggingEvent shouldNotBe null
        loggingEvent.formattedMessage.contains("Navn på saksbehandler ikke funnet (attribute == null)") shouldBe true
    }

    @Test
    fun `Skal gi brukernavn ukjent og lage warning når det søkes med spesialtegn`() {
        sbIdent = "L999999) or (cn=A*"
        val saksbehandlerNavn = ldap.hentBrukernavn(sbIdent)
        verify { appenderMock.doAppend(capture(logCaptor)) }
        val loggingEvent = logCaptor.captured
        saksbehandlerNavn shouldBe null
        loggingEvent shouldNotBe null
        loggingEvent.formattedMessage.contains("Navn på saksbehandler for ident $sbIdent ikke funnet (1)") shouldBe true
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
