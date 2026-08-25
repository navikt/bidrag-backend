package no.nav.bidrag.reisekostnad.skedulering

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import jakarta.persistence.EntityManager
import no.nav.bidrag.reisekostnad.BidragReisekostnadApiTestapplikasjon
import no.nav.bidrag.reisekostnad.MELLOMSTEIN_IDENT
import no.nav.bidrag.reisekostnad.MIDDELSTEIN_IDENT
import no.nav.bidrag.reisekostnad.STORSTEIN_IDENT
import no.nav.bidrag.reisekostnad.Testperson
import no.nav.bidrag.reisekostnad.database.dao.BarnDao
import no.nav.bidrag.reisekostnad.database.dao.ForelderDao
import no.nav.bidrag.reisekostnad.database.dao.ForespørselDao
import no.nav.bidrag.reisekostnad.database.dao.OppgavebestillingDao
import no.nav.bidrag.reisekostnad.database.datamodell.Barn
import no.nav.bidrag.reisekostnad.database.datamodell.Forelder
import no.nav.bidrag.reisekostnad.database.datamodell.Forespørsel
import no.nav.bidrag.reisekostnad.integrasjon.brukernotifikasjon.Brukernotifikasjonkonsument
import no.nav.bidrag.reisekostnad.konfigurasjon.Profil
import no.nav.bidrag.reisekostnad.registrerAllePersonstubber
import no.nav.bidrag.reisekostnad.tjeneste.Databasetjeneste
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionTemplate
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import org.wiremock.spring.InjectWireMock
import java.time.LocalDate
import java.time.LocalDateTime

@ActiveProfiles(value = [Profil.TEST, Profil.HENDELSE])
@EnableMockOAuth2Server
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@EnableWireMock(
    ConfigureWireMock(port = 0), // , filesUnderDirectory = ["src/test/resources/mappings"])
)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [BidragReisekostnadApiTestapplikasjon::class],
)
@ExtendWith(MockKExtension::class)
@DisplayName("DatabehandlerTest")
class DatabehandlerTest {

    @MockK
    lateinit var brukernotifikasjonkonsument: Brukernotifikasjonkonsument

    @Autowired
    lateinit var forespørselDao: ForespørselDao

    @Autowired
    lateinit var databasetjeneste: Databasetjeneste

    @Autowired
    lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    lateinit var forelderDao: ForelderDao

    @Autowired
    lateinit var entityManager: EntityManager

    @Autowired
    lateinit var barnDao: BarnDao

    @Autowired
    lateinit var databehandler: Databehandler

    @Autowired
    lateinit var oppgavebestillingDao: OppgavebestillingDao

    @InjectWireMock
    lateinit var wireMockServer: WireMockServer

    @BeforeEach
    fun sletteTestdata() {
        WireMock.resetAllRequests()
        oppgavebestillingDao.deleteAll()
        barnDao.deleteAll()
        forespørselDao.deleteAll()
        forelderDao.deleteAll()
        registrerAllePersonstubber(wireMockServer)
    }

    protected var testpersonGråtass = Forelder.builder().personident(Testperson.testpersonGråtass.ident).build()
    protected var testpersonStreng = Forelder.builder().personident(Testperson.testpersonStreng.ident).build()
    protected var testpersonBarn16 = Barn.builder().personident(Testperson.testpersonBarn16.ident)
        .fødselsdato(Testperson.testpersonBarn16.fødselsdato).build()
    protected var testpersonBarn10 = Barn.builder().personident(Testperson.testpersonBarn10.ident)
        .fødselsdato(Testperson.testpersonBarn10.fødselsdato).build()
    protected var testpersonBarn11 =
        Barn.builder().personident("42124124").fødselsdato(LocalDate.now().minusYears(11)).build()
    protected var testpersonBarn12 =
        Barn.builder().personident("335533133355555").fødselsdato(LocalDate.now().minusYears(12)).build()
    protected var testpersonBarn13 =
        Barn.builder().personident(STORSTEIN_IDENT).fødselsdato(LocalDate.now().minusYears(13)).build()
    protected var testpersonBarn15 =
        Barn.builder().personident(MELLOMSTEIN_IDENT).fødselsdato(LocalDate.now().minusYears(15)).build()
    protected var testpersonBarn15Nr2 =
        Barn.builder().personident("5515155").fødselsdato(LocalDate.now().minusYears(15).minusDays(7)).build()
    protected var testpersonBarn15Nr3 =
        Barn.builder().personident(MIDDELSTEIN_IDENT).fødselsdato(LocalDate.now().minusYears(15).minusDays(1))
            .build()

    fun opppretteForespørsel(kreverSamtykke: Boolean = false): Forespørsel = oppretteForespørsel(testpersonGråtass, testpersonStreng, mutableSetOf(testpersonBarn10, testpersonBarn16), kreverSamtykke)

    fun oppretteForespørsel(
        hovedpart: Forelder = testpersonGråtass,
        motpart: Forelder = testpersonStreng,
        barn: Set<Barn> = mutableSetOf(testpersonBarn10, testpersonBarn16),
        kreverSamtykke: Boolean = false,
    ): Forespørsel = Forespørsel.builder()
        .opprettet(LocalDateTime.now())
        .hovedpart(hovedpart)
        .motpart(motpart)
        .barn(barn)
        .kreverSamtykke(kreverSamtykke)
        .samtykkefrist(LocalDate.now().plusDays(4))
        .build()
}
