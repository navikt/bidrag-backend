package no.nav.bidrag.regnskap.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.regnskap.consumer.BidragPersonConsumer
import no.nav.bidrag.regnskap.utils.TestData
import no.nav.bidrag.transport.person.PersonDto
import no.nav.bidrag.transport.person.PersondetaljerDto
import no.nav.bidrag.transport.person.hendelse.Endringsmelding
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class PersonhendelseServiceTest {

    @MockK
    private lateinit var persistenceService: PersistenceService

    @MockK
    private lateinit var bidragPersonConsumer: BidragPersonConsumer

    @InjectMockKs
    private lateinit var personhendelseService: PersonhendelseService

    @BeforeEach
    fun settOpp() {
        every { persistenceService.hentAlleMottakereMedIdent(any()) } returns emptyList()
        every { persistenceService.hentAlleKravhavereMedIdent(any()) } returns emptyList()
        every { persistenceService.hentAlleSkyldnereMedIdent(any()) } returns emptyList()
        every { persistenceService.hentAlleGjelderMedIdent(any()) } returns emptyList()
    }

    @Test
    fun `oppdaterer skyldner på alle tidligere identer når identene i hendelsen ikke gir treff`() {
        val nyIdent = genererFødselsnummer()
        val gammelIdent = genererFødselsnummer()
        val eldreIdent = genererFødselsnummer()
        val førsteOppdrag = TestData.opprettOppdrag(skyldnerIdent = gammelIdent)
        val andreOppdrag = TestData.opprettOppdrag(skyldnerIdent = eldreIdent)
        every { bidragPersonConsumer.hentPerson(any()) } returns person(nyIdent, gammelIdent, eldreIdent, nyIdent)
        every { persistenceService.hentAlleSkyldnereMedIdent(gammelIdent) } returns listOf(førsteOppdrag)
        every { persistenceService.hentAlleSkyldnereMedIdent(eldreIdent) } returns listOf(andreOppdrag)

        personhendelseService.behandlePersonhendelse(hendelse(nyIdent))

        førsteOppdrag.skyldnerIdent shouldBe nyIdent
        andreOppdrag.skyldnerIdent shouldBe nyIdent
        verify(exactly = 1) { bidragPersonConsumer.hentPerson(any()) }
        verify(exactly = 1) { persistenceService.hentAlleSkyldnereMedIdent(gammelIdent) }
        verify(exactly = 1) { persistenceService.hentAlleSkyldnereMedIdent(eldreIdent) }
        verify(exactly = 1) { persistenceService.hentAlleSkyldnereMedIdent(nyIdent) }
        verify(exactly = 1) { persistenceService.hentAlleMottakereMedIdent(gammelIdent) }
        verify(exactly = 1) { persistenceService.hentAlleGjelderMedIdent(gammelIdent) }
    }

    @Test
    fun `søker ikke tidligere skyldnere når skyldner finnes på identen i hendelsen`() {
        val nyIdent = genererFødselsnummer()
        val gammelIdent = genererFødselsnummer()
        val kravhaverOppdrag = TestData.opprettOppdrag(kravhaverIdent = gammelIdent)
        val skyldnerOppdrag = TestData.opprettOppdrag(skyldnerIdent = nyIdent)
        every { bidragPersonConsumer.hentPerson(any()) } returns person(nyIdent, gammelIdent)
        every { persistenceService.hentAlleSkyldnereMedIdent(nyIdent) } returns listOf(skyldnerOppdrag)
        every { persistenceService.hentAlleKravhavereMedIdent(gammelIdent) } returns listOf(kravhaverOppdrag)

        personhendelseService.behandlePersonhendelse(hendelse(nyIdent))

        kravhaverOppdrag.kravhaverIdent shouldBe nyIdent
        verify(exactly = 0) { persistenceService.hentAlleSkyldnereMedIdent(gammelIdent) }
        verify(exactly = 1) { persistenceService.hentAlleKravhavereMedIdent(gammelIdent) }
        verify(exactly = 1) { persistenceService.hentAlleMottakereMedIdent(gammelIdent) }
        verify(exactly = 1) { persistenceService.hentAlleGjelderMedIdent(gammelIdent) }
    }

    @Test
    fun `oppdaterer mottaker og gjelder på tidligere identer når identen i hendelsen ikke gir treff`() {
        val nyIdent = genererFødselsnummer()
        val gammelIdent = genererFødselsnummer()
        val mottakerOppdrag = TestData.opprettOppdrag(mottakerIdent = gammelIdent)
        val gjelderOppdrag = TestData.opprettOppdrag(gjelderIdent = gammelIdent)
        every { bidragPersonConsumer.hentPerson(any()) } returns person(nyIdent, gammelIdent)
        every { persistenceService.hentAlleMottakereMedIdent(gammelIdent) } returns listOf(mottakerOppdrag)
        every { persistenceService.hentAlleGjelderMedIdent(gammelIdent) } returns listOf(gjelderOppdrag)

        personhendelseService.behandlePersonhendelse(hendelse(nyIdent))

        mottakerOppdrag.mottakerIdent shouldBe nyIdent
        gjelderOppdrag.gjelderIdent shouldBe nyIdent
        verify(exactly = 1) { persistenceService.hentAlleMottakereMedIdent(gammelIdent) }
        verify(exactly = 1) { persistenceService.hentAlleGjelderMedIdent(gammelIdent) }
    }

    @Test
    fun `søker ikke tidligere identer når alle roller finnes på identen i hendelsen`() {
        val nyIdent = genererFødselsnummer()
        val gammelIdent = genererFødselsnummer()
        every { bidragPersonConsumer.hentPerson(any()) } returns person(nyIdent, gammelIdent)
        every { persistenceService.hentAlleMottakereMedIdent(nyIdent) } returns listOf(TestData.opprettOppdrag(mottakerIdent = nyIdent))
        every { persistenceService.hentAlleKravhavereMedIdent(nyIdent) } returns listOf(TestData.opprettOppdrag(kravhaverIdent = nyIdent))
        every { persistenceService.hentAlleSkyldnereMedIdent(nyIdent) } returns listOf(TestData.opprettOppdrag(skyldnerIdent = nyIdent))
        every { persistenceService.hentAlleGjelderMedIdent(nyIdent) } returns listOf(TestData.opprettOppdrag(gjelderIdent = nyIdent))

        personhendelseService.behandlePersonhendelse(hendelse(nyIdent))

        verify(exactly = 0) { persistenceService.hentAlleMottakereMedIdent(gammelIdent) }
        verify(exactly = 0) { persistenceService.hentAlleKravhavereMedIdent(gammelIdent) }
        verify(exactly = 0) { persistenceService.hentAlleSkyldnereMedIdent(gammelIdent) }
        verify(exactly = 0) { persistenceService.hentAlleGjelderMedIdent(gammelIdent) }
    }

    @Test
    fun `oppdaterer fortsatt alle roller når identen i hendelsen gir treff`() {
        val nyIdent = genererFødselsnummer()
        val gammelIdent = genererFødselsnummer()
        val oppdrag = TestData.opprettOppdrag(
            kravhaverIdent = gammelIdent,
            skyldnerIdent = gammelIdent,
            mottakerIdent = gammelIdent,
            gjelderIdent = gammelIdent,
        )
        every { bidragPersonConsumer.hentPerson(any()) } returns person(nyIdent, gammelIdent)
        every { persistenceService.hentAlleMottakereMedIdent(gammelIdent) } returns listOf(oppdrag)
        every { persistenceService.hentAlleKravhavereMedIdent(gammelIdent) } returns listOf(oppdrag)
        every { persistenceService.hentAlleSkyldnereMedIdent(gammelIdent) } returns listOf(oppdrag)
        every { persistenceService.hentAlleGjelderMedIdent(gammelIdent) } returns listOf(oppdrag)

        personhendelseService.behandlePersonhendelse(hendelse(gammelIdent))

        oppdrag.mottakerIdent shouldBe nyIdent
        oppdrag.kravhaverIdent shouldBe nyIdent
        oppdrag.skyldnerIdent shouldBe nyIdent
        oppdrag.gjelderIdent shouldBe nyIdent
        verify(exactly = 1) { persistenceService.hentAlleKravhavereMedIdent(gammelIdent) }
        verify(exactly = 1) { persistenceService.hentAlleSkyldnereMedIdent(gammelIdent) }
    }

    @Test
    fun `oppdaterer alle identer i hendelsen selv om den første gir treff`() {
        val nyIdent = genererFødselsnummer()
        val førsteIdent = genererFødselsnummer()
        val andreIdent = genererFødselsnummer()
        val førsteOppdrag = TestData.opprettOppdrag(mottakerIdent = førsteIdent)
        val andreOppdrag = TestData.opprettOppdrag(mottakerIdent = andreIdent)
        every { bidragPersonConsumer.hentPerson(any()) } returns person(nyIdent)
        every { persistenceService.hentAlleMottakereMedIdent(førsteIdent) } returns listOf(førsteOppdrag)
        every { persistenceService.hentAlleMottakereMedIdent(andreIdent) } returns listOf(andreOppdrag)

        personhendelseService.behandlePersonhendelse(hendelse(nyIdent, setOf(førsteIdent, andreIdent)))

        førsteOppdrag.mottakerIdent shouldBe nyIdent
        andreOppdrag.mottakerIdent shouldBe nyIdent
    }

    @Test
    fun `hopper fortsatt over hendelsen når bidrag-person ikke finner personen`() {
        val ident = genererFødselsnummer()
        every { bidragPersonConsumer.hentPerson(any()) } returns null

        personhendelseService.behandlePersonhendelse(hendelse(ident))

        verify(exactly = 0) { persistenceService.hentAlleMottakereMedIdent(any()) }
        verify(exactly = 0) { persistenceService.hentAlleSkyldnereMedIdent(any()) }
        verify(exactly = 0) { persistenceService.hentAlleKravhavereMedIdent(any()) }
        verify(exactly = 0) { persistenceService.hentAlleGjelderMedIdent(any()) }
    }

    private fun person(nyIdent: String, vararg tidligereIdenter: String) = PersondetaljerDto(
        person = PersonDto(Personident(nyIdent)),
        adresse = null,
        kontonummer = null,
        dødsbo = null,
        språk = null,
        tidligereIdenter = tidligereIdenter.map(::Personident),
    )

    private fun hendelse(ident: String, personidenter: Set<String> = setOf(ident)) = Endringsmelding(
        aktørid = "aktørid",
        personidenter = personidenter,
        endringer = listOf(Endringsmelding.Endring(identendring = Endringsmelding.Identendring(identifikasjonsnummer = ident))),
    )
}
