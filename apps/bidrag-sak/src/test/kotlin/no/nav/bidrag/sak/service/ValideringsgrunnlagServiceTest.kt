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
import no.nav.bidrag.transport.person.PersonDto
import no.nav.bidrag.transport.sak.OpprettSakRequest
import no.nav.bidrag.transport.sak.ReellMottakerDto
import no.nav.bidrag.transport.sak.RolleDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ValideringsgrunnlagServiceTest {
    private val identConsumer: IdentConsumer = mockk()
    private val cachedKodeverkService: CachedKodeverkService = mockk()
    private val service = ValideringsgrunnlagService(identConsumer, cachedKodeverkService)

    @BeforeEach
    fun setup() {
        every { identConsumer.hentPersonInformasjon(any()) } answers { PersonDto(ident = firstArg(), fødselsdato = null) }
        every { identConsumer.hentAlleIdenter(any()) } returns emptyList()
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
    fun `tar ikke med RM som ikke finnes i fødselsdatoene`() {
        val barn = genererPersonident()
        val rm = genererPersonident()
        every { identConsumer.hentPersonInformasjon(rm) } returns null
        val barnMedRm = RolleDto(
            type = Rolletype.BARN,
            fødselsnummer = barn,
            reellMottaker = ReellMottakerDto(ident = ReellMottaker(rm.verdi), verge = false),
        )

        val grunnlag = service.hentForOpprettelse(opprett(barnMedRm))

        grunnlag.fødselsdatoer shouldBe mapOf<Personident, LocalDate?>(barn to null)
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
    fun `henter fødselsdato fra personinformasjonen`() {
        val barn = genererPersonident()
        val fødselsdato = LocalDate.now().minusYears(10)
        every { identConsumer.hentPersonInformasjon(barn) } returns PersonDto(ident = barn, fødselsdato = fødselsdato)

        val grunnlag = service.hentForOpprettelse(opprett(rolle(Rolletype.BARN, barn)))

        grunnlag.fødselsdato(barn) shouldBe fødselsdato
        grunnlag.fødselsdatoer shouldBe mapOf(barn to fødselsdato)
    }

    @Test
    fun `henter fødselsdatoer for alle roller og RM`() {
        val barn = genererPersonident()
        val bm = genererPersonident()
        val rm = genererPersonident()
        val fødselsdatoRm = LocalDate.now().minusYears(40)
        every { identConsumer.hentPersonInformasjon(rm) } returns PersonDto(ident = rm, fødselsdato = fødselsdatoRm)
        val fødselsdatoer = mapOf<Personident, LocalDate?>(barn to null, bm to null, rm to fødselsdatoRm)
        val barnMedRm = RolleDto(
            type = Rolletype.BARN,
            fødselsnummer = barn,
            reellMottaker = ReellMottakerDto(ident = ReellMottaker(rm.verdi), verge = false),
        )

        val grunnlag = service.hentForOpprettelse(opprett(barnMedRm, rolle(Rolletype.BIDRAGSMOTTAKER, bm)))

        grunnlag.fødselsdatoer shouldBe fødselsdatoer
        grunnlag.finnes(rm) shouldBe false
    }

    @Test
    fun `slår ikke opp blanke identer`() {
        service.hentForOpprettelse(opprett(rolle(Rolletype.BIDRAGSMOTTAKER, Personident(""))))

        verify(exactly = 0) { identConsumer.hentPersonInformasjon(any()) }
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
    fun `slår opp samme person én gang`() {
        val ident = genererPersonident()

        val grunnlag =
            service.hentForOpprettelse(opprett(rolle(Rolletype.BIDRAGSMOTTAKER, ident), rolle(Rolletype.BARN, ident)))

        grunnlag.finnes(ident) shouldBe true
        verify(exactly = 1) { identConsumer.hentPersonInformasjon(ident) }
    }

    @Test
    fun `henter identer for lagrede og forespurte roller én gang per ident`() {
        val gammelIdent = genererPersonident()
        val nyIdent = genererPersonident()
        val bm = genererPersonident()
        every { identConsumer.hentAlleIdenter(nyIdent.verdi) } returns listOf(nyIdent.verdi, gammelIdent.verdi)
        val lagredeRoller = listOf(
            Rolle(fødselsnummer = gammelIdent.verdi, rolleType = Rolletype.BARN),
            Rolle(fødselsnummer = bm.verdi, rolleType = Rolletype.BIDRAGSMOTTAKER),
        )

        val grunnlag = service.hentForEndring(lagredeRoller, setOf(rolle(Rolletype.BARN, nyIdent), rolle(Rolletype.BIDRAGSMOTTAKER, bm)), land = null)

        grunnlag.sammePerson(gammelIdent.verdi, nyIdent.verdi) shouldBe true
        grunnlag.sammePerson(bm.verdi, nyIdent.verdi) shouldBe false
        listOf(gammelIdent, nyIdent, bm).forEach { verify(exactly = 1) { identConsumer.hentAlleIdenter(it.verdi) } }
    }

    private fun rolle(type: Rolletype, fnr: Personident) = RolleDto(type = type, fødselsnummer = fnr)

    private fun opprett(vararg roller: RolleDto) = OpprettSakRequest(eierfogd = Enhetsnummer("1701"), roller = roller.toSet())
}
