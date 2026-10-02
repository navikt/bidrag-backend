package no.nav.bidrag.sak.service

import io.kotest.assertions.throwables.shouldThrowMessage
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.bidrag.commons.util.IdentConsumer
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.ident.ReellMottaker
import no.nav.bidrag.domene.land.Landkode
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.generer.testdata.person.genererPersonident
import no.nav.bidrag.sak.domain.Rolle
import no.nav.bidrag.sak.integration.kodeverk.CachedKodeverkService
import no.nav.bidrag.sak.integration.person.BidragPersonClient
import no.nav.bidrag.transport.person.PersonDto
import no.nav.bidrag.transport.sak.OpprettSakRequest
import no.nav.bidrag.transport.sak.ReellMottakerDto
import no.nav.bidrag.transport.sak.RolleDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ValideringsgrunnlagServiceTest {
    private val identConsumer: IdentConsumer = mockk()
    private val bidragPersonClient: BidragPersonClient = mockk()
    private val cachedKodeverkService: CachedKodeverkService = mockk()
    private val service = ValideringsgrunnlagService(identConsumer, bidragPersonClient, cachedKodeverkService)

    @BeforeEach
    fun setup() {
        every { identConsumer.hentPersonInformasjon(any()) } answers { PersonDto(ident = firstArg(), fødselsdato = null) }
        every { bidragPersonClient.hentAlleIdenter(any()) } answers { setOf(firstArg()) }
        every { bidragPersonClient.hentFødselsdatoer(any()) } returns emptyMap()
        every { cachedKodeverkService.hentLandkoder() } returns mapOf(Landkode("NOR") to "Norge")
    }

    @Test
    fun `person som ikke finnes mangler i grunnlaget`() {
        val bm = genererPersonident()
        every { identConsumer.hentPersonInformasjon(bm) } returns null

        val grunnlag = service.hentForOpprettelse(opprett(rolle(Rolletype.BIDRAGSMOTTAKER, bm)))

        grunnlag.finnes(bm) shouldBe false
    }

    @Test
    fun `feil fra personoppslaget går videre`() {
        val bm = genererPersonident()
        every { identConsumer.hentPersonInformasjon(bm) } throws IllegalStateException("bidrag-person er nede")

        shouldThrowMessage("bidrag-person er nede") {
            service.hentForOpprettelse(opprett(rolle(Rolletype.BIDRAGSMOTTAKER, bm)))
        }
    }

    @Test
    fun `bruker fødselsdato fra fødselsdato-oppslaget når personinfo mangler den`() {
        val barn = genererPersonident()
        val fødselsdato = LocalDate.now().minusYears(18)
        every { bidragPersonClient.hentFødselsdatoer(listOf(barn)) } returns mapOf(barn to fødselsdato)

        val grunnlag = service.hentForOpprettelse(opprett(rolle(Rolletype.BARN, barn)))

        grunnlag.fødselsdato(barn) shouldBe fødselsdato
    }

    @Test
    fun `bruker fødselsdato fra personinfo først`() {
        val barn = genererPersonident()
        val fraPersoninfo = LocalDate.now().minusYears(10)
        every { identConsumer.hentPersonInformasjon(barn) } returns PersonDto(ident = barn, fødselsdato = fraPersoninfo)
        every { bidragPersonClient.hentFødselsdatoer(listOf(barn)) } returns mapOf(barn to LocalDate.now().minusYears(20))

        val grunnlag = service.hentForOpprettelse(opprett(rolle(Rolletype.BARN, barn)))

        grunnlag.fødselsdato(barn) shouldBe fraPersoninfo
    }

    @Test
    fun `feil fra fødselsdato-oppslaget går videre`() {
        val barn = genererPersonident()
        every { bidragPersonClient.hentFødselsdatoer(listOf(barn)) } throws IllegalStateException("PDL utilgjengelig")

        shouldThrowMessage("PDL utilgjengelig") {
            service.hentForOpprettelse(opprett(rolle(Rolletype.BARN, barn)))
        }
    }

    @Test
    fun `henter fødselsdatoer for alle roller og RM i ett kall`() {
        val barn = genererPersonident()
        val bm = genererPersonident()
        val rm = genererPersonident()
        val fødselsdatoer = mapOf<Personident, LocalDate?>(barn to null, bm to null, rm to null)
        every { bidragPersonClient.hentFødselsdatoer(listOf(barn, bm, rm)) } returns fødselsdatoer
        val barnMedRm = RolleDto(
            type = Rolletype.BARN,
            fødselsnummer = barn,
            reellMottaker = ReellMottakerDto(ident = ReellMottaker(rm.verdi), verge = false),
        )

        val grunnlag = service.hentForOpprettelse(opprett(barnMedRm, rolle(Rolletype.BIDRAGSMOTTAKER, bm)))

        grunnlag.fødselsdatoer shouldBe fødselsdatoer
        verify(exactly = 1) { bidragPersonClient.hentFødselsdatoer(any()) }
    }

    @Test
    fun `feil fra identoppslaget går videre`() {
        val bm = genererPersonident()
        every { bidragPersonClient.hentAlleIdenter(bm.verdi) } throws IllegalStateException("bidrag-person er nede")

        shouldThrowMessage("bidrag-person er nede") {
            service.hentForOpprettelse(opprett(rolle(Rolletype.BIDRAGSMOTTAKER, bm)))
        }
    }

    @Test
    fun `slår ikke opp blanke identer`() {
        service.hentForOpprettelse(opprett(rolle(Rolletype.BIDRAGSMOTTAKER, Personident(""))))

        verify(exactly = 0) { identConsumer.hentPersonInformasjon(any()) }
        verify(exactly = 0) { bidragPersonClient.hentAlleIdenter(any()) }
        verify(exactly = 0) { bidragPersonClient.hentFødselsdatoer(any()) }
    }

    @Test
    fun `henter ikke landkoder når land ikke er satt`() {
        val grunnlag = service.hentForOpprettelse(opprett(rolle(Rolletype.BIDRAGSMOTTAKER, genererPersonident())))

        grunnlag.landkoder shouldBe emptySet()
        verify(exactly = 0) { cachedKodeverkService.hentLandkoder() }
    }

    @Test
    fun `henter landkoder når land er satt`() {
        val request = OpprettSakRequest(eierfogd = Enhetsnummer("1701"), land = Landkode("SWE"), roller = emptySet())

        service.hentForOpprettelse(request).landkoder shouldBe setOf(Landkode("NOR"))
    }

    @Test
    fun `henter identer for både lagrede og forespurte roller ved endring`() {
        val lagret = genererPersonident()
        val forespurt = genererPersonident()
        val historisk = genererPersonident()
        every { bidragPersonClient.hentAlleIdenter(lagret.verdi) } returns setOf(lagret.verdi, historisk.verdi)

        val grunnlag = service.hentForEndring(
            listOf(Rolle(fødselsnummer = lagret.verdi, rolleType = Rolletype.BIDRAGSMOTTAKER)),
            listOf(rolle(Rolletype.BIDRAGSPLIKTIG, forespurt)),
            null,
        )

        grunnlag.identer shouldBe mapOf(
            forespurt.verdi to setOf(forespurt.verdi),
            lagret.verdi to setOf(lagret.verdi, historisk.verdi),
        )
        grunnlag.personer.keys shouldBe setOf(forespurt.verdi)
    }

    @Test
    fun `slår opp samme person én gang`() {
        val ident = genererPersonident()

        val grunnlag =
            service.hentForOpprettelse(opprett(rolle(Rolletype.BIDRAGSMOTTAKER, ident), rolle(Rolletype.BARN, ident)))

        grunnlag.finnes(ident) shouldBe true
        verify(exactly = 1) { identConsumer.hentPersonInformasjon(ident) }
        verify(exactly = 1) { bidragPersonClient.hentAlleIdenter(ident.verdi) }
        verify(exactly = 1) { bidragPersonClient.hentFødselsdatoer(listOf(ident)) }
    }

    private fun rolle(type: Rolletype, fnr: Personident) = RolleDto(type = type, fødselsnummer = fnr)

    private fun opprett(vararg roller: RolleDto) = OpprettSakRequest(eierfogd = Enhetsnummer("1701"), roller = roller.toSet())
}
