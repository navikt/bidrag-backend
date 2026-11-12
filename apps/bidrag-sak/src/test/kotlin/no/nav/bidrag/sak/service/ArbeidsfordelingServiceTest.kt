package no.nav.bidrag.sak.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.sak.Arbeidsfordeling
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.enums.sak.UkjentPart
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.ident.ReellMottaker
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.generer.testdata.person.genererPersonident
import no.nav.bidrag.sak.domain.Bidragssak
import no.nav.bidrag.sak.domain.Rolle
import no.nav.bidrag.sak.domain.Rollehistorikk
import no.nav.bidrag.sak.domain.Tilgang
import no.nav.bidrag.sak.integration.organisasjon.BidragOrganisasjonClient
import no.nav.bidrag.transport.organisasjon.ArbeidsfordelingSakRequest
import no.nav.bidrag.transport.organisasjon.ArbeidsfordelingSakResponse
import no.nav.bidrag.transport.organisasjon.EnhetDto
import no.nav.bidrag.transport.sak.ReellMottakerDto
import no.nav.bidrag.transport.sak.ReellMottakerType
import no.nav.bidrag.transport.sak.RolleDto
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class ArbeidsfordelingServiceTest {
    private val organisasjonClient: BidragOrganisasjonClient = mockk()
    private val arbeidsfordelingService = ArbeidsfordelingService(organisasjonClient)

    private val bm = genererPersonident()
    private val barn = genererPersonident(LocalDate.now().minusYears(10))

    private fun sakHosEnhet(enhet: String, vararg roller: Rolle) = Bidragssak(saksnummer = "2500001", eierfogd = enhet).also { sak ->
        sak.tilganger.add(Tilgang(enhetsnummer = enhet, bidragssak = sak))
        sak.roller.addAll(roller.onEach { it.bidragssak = sak })
    }

    private fun rolle(
        type: Rolletype,
        ident: Personident? = null,
        ukjentPart: UkjentPart? = null,
        fødselsdato: LocalDate? = null,
    ) = Rolle(fødselsnummer = ident?.verdi, rolleType = type, ukjentPart = ukjentPart, fødselsdato = fødselsdato)

    private fun organisasjonGirEnhet(enhet: String) {
        every { organisasjonClient.hentArbeidsfordelingForSak(any()) } returns
            ArbeidsfordelingSakResponse(EnhetDto(nummer = Enhetsnummer(enhet)), false)
    }

    @Test
    fun `flytter saken til enheten arbeidsfordelingen gir`() {
        organisasjonGirEnhet("4806")
        val sak = sakHosEnhet("1701", rolle(Rolletype.BIDRAGSMOTTAKER, bm), rolle(Rolletype.BARN, barn))

        arbeidsfordelingService.utførArbeidsfordeling(sak)

        sak.eierfogd shouldBe "4806"
        sak.tilganger.single { it.enhetsnummer == "1701" }.tilgangTomDato shouldNotBe null
        sak.tilganger.single { it.enhetsnummer == "4806" }.tilgangTomDato shouldBe null
    }

    @Test
    fun `flytter saken ut av skjermet enhet når arbeidsfordelingen gir en vanlig enhet`() {
        organisasjonGirEnhet("4806")
        val sak = sakHosEnhet("2103", rolle(Rolletype.BIDRAGSMOTTAKER, bm), rolle(Rolletype.BARN, barn))

        arbeidsfordelingService.utførArbeidsfordeling(sak)

        sak.eierfogd shouldBe "4806"
    }

    @Test
    fun `beholder eiertilgangen når arbeidsfordelingen gir samme enhet`() {
        organisasjonGirEnhet("1701")
        val sak = sakHosEnhet("1701", rolle(Rolletype.BIDRAGSMOTTAKER, bm), rolle(Rolletype.BARN, barn))

        arbeidsfordelingService.utførArbeidsfordeling(sak)

        sak.eierfogd shouldBe "1701"
        sak.tilganger.single().tilgangTomDato shouldBe null
    }

    @Test
    fun `bruker yngste barn som hovedident når bidragsmottaker er ukjent`() {
        val request = slot<ArbeidsfordelingSakRequest>()
        every { organisasjonClient.hentArbeidsfordelingForSak(capture(request)) } returns
            ArbeidsfordelingSakResponse(EnhetDto(nummer = Enhetsnummer("4806")), false)
        val eldreBarn = genererPersonident()
        val sak =
            sakHosEnhet(
                "1701",
                rolle(Rolletype.BIDRAGSMOTTAKER, ukjentPart = UkjentPart.UK),
                rolle(Rolletype.BARN, eldreBarn, fødselsdato = LocalDate.now().minusYears(16)),
                rolle(Rolletype.BARN, barn, fødselsdato = LocalDate.now().minusYears(10)),
            )

        arbeidsfordelingService.utførArbeidsfordeling(sak)

        request.captured.hovedident shouldBe barn
        request.captured.parter.shouldContainExactlyInAnyOrder(barn, eldreBarn)
    }

    @Test
    fun `tar med reell mottaker og feilregistrerte roller i oppslaget`() {
        val reellMottaker = genererPersonident()
        val feilregistrert = genererPersonident()
        val request = slot<ArbeidsfordelingSakRequest>()
        every { organisasjonClient.hentArbeidsfordelingForSak(capture(request)) } returns
            ArbeidsfordelingSakResponse(EnhetDto(nummer = Enhetsnummer("2103")), true)
        val sak =
            sakHosEnhet(
                "1701",
                rolle(Rolletype.BIDRAGSMOTTAKER, bm),
                rolle(Rolletype.BARN, barn),
                rolle(Rolletype.REELMOTTAKER, reellMottaker),
                rolle(Rolletype.FEILREGISTRERT, feilregistrert),
            )

        arbeidsfordelingService.utførArbeidsfordeling(sak)

        request.captured.hovedident shouldBe bm
        request.captured.parter.shouldContainExactlyInAnyOrder(bm, barn, reellMottaker, feilregistrert)
        sak.eierfogd shouldBe "2103"
        sak.ansatt shouldBe false
        sak.inhabilitet shouldBe false
    }

    @Test
    fun `tar med tidligere parter fra rollehistorikken i oppslaget`() {
        val tidligereRm = genererPersonident()
        val request = slot<ArbeidsfordelingSakRequest>()
        every { organisasjonClient.hentArbeidsfordelingForSak(capture(request)) } returns
            ArbeidsfordelingSakResponse(EnhetDto(nummer = Enhetsnummer("2103")), true)
        val barnerolle = rolle(Rolletype.BARN, barn)
        val sak = sakHosEnhet("1701", rolle(Rolletype.BIDRAGSMOTTAKER, bm), barnerolle)
        barnerolle.rollehistorikk.add(
            Rollehistorikk(
                saksnummer = sak.saksnummer,
                rolleFødselsnummer = barn.verdi,
                rmRolleFødselsnummer = tidligereRm.verdi,
                opprettetAv = "Z999999",
                rolle = barnerolle,
            ),
        )

        arbeidsfordelingService.utførArbeidsfordeling(sak)

        request.captured.parter.shouldContainExactlyInAnyOrder(bm, barn, tidligereRm)
    }

    @Test
    fun `feiler uten å endre eierfogd når ingen part er kjent`() {
        val sak = sakHosEnhet("1701", rolle(Rolletype.BIDRAGSMOTTAKER, ukjentPart = UkjentPart.UK))

        shouldThrow<IllegalStateException> { arbeidsfordelingService.utførArbeidsfordeling(sak) }

        verify(exactly = 0) { organisasjonClient.hentArbeidsfordelingForSak(any()) }
        sak.eierfogd shouldBe "1701"
    }

    @Test
    fun `bestemmer enhet for ny sak med reell mottaker før saken bygges`() {
        val request = slot<ArbeidsfordelingSakRequest>()
        every { organisasjonClient.hentArbeidsfordelingForSak(capture(request)) } returns
            ArbeidsfordelingSakResponse(EnhetDto(Enhetsnummer("2103")), true)
        val mottaker = genererPersonident()
        val roller = setOf(
            RolleDto(type = Rolletype.BIDRAGSMOTTAKER),
            RolleDto(
                fødselsnummer = barn,
                type = Rolletype.BARN,
                reellMottaker = ReellMottakerDto(ReellMottaker(mottaker.verdi), true, ReellMottakerType.PERSON),
            ),
        )

        val enhet = arbeidsfordelingService.hentEnhetForNySak(
            roller,
            mapOf(barn to LocalDate.of(2016, 1, 1)),
            Arbeidsfordeling.SETTEKONTOR,
            Sakskategori.NASJONAL,
        )

        enhet.nummer shouldBe Enhetsnummer("2103")
        request.captured.hovedident shouldBe barn
        request.captured.parter.shouldContainExactlyInAnyOrder(barn, mottaker)
        request.captured.arbeidsfordeling shouldBe Arbeidsfordeling.SETTEKONTOR
    }
}
