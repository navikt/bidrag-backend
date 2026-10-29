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
import no.nav.bidrag.domene.enums.sak.UkjentPart
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.generer.testdata.person.genererPersonident
import no.nav.bidrag.sak.domain.Bidragssak
import no.nav.bidrag.sak.domain.Rolle
import no.nav.bidrag.sak.domain.Rollehistorikk
import no.nav.bidrag.sak.domain.Tilgang
import no.nav.bidrag.sak.integration.organisasjon.BidragOrganisasjonClient
import no.nav.bidrag.transport.organisasjon.EnhetDto
import no.nav.bidrag.transport.organisasjon.HentEnhetRequest
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
        every { organisasjonClient.hentEnhetForArbeidsfordelingGeografiskTilknytning(any()) } returns
            EnhetDto(nummer = Enhetsnummer(enhet))
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
        val request = slot<HentEnhetRequest>()
        every { organisasjonClient.hentEnhetForArbeidsfordelingGeografiskTilknytning(capture(request)) } returns
            EnhetDto(nummer = Enhetsnummer("4806"))
        val eldreBarn = genererPersonident()
        val sak =
            sakHosEnhet(
                "1701",
                rolle(Rolletype.BIDRAGSMOTTAKER, ukjentPart = UkjentPart.UK),
                rolle(Rolletype.BARN, eldreBarn, fødselsdato = LocalDate.now().minusYears(16)),
                rolle(Rolletype.BARN, barn, fødselsdato = LocalDate.now().minusYears(10)),
            )

        arbeidsfordelingService.utførArbeidsfordeling(sak)

        request.captured.ident shouldBe barn
        request.captured.biidenter.shouldContainExactlyInAnyOrder(barn, eldreBarn)
    }

    @Test
    fun `tar med reell mottaker og feilregistrerte roller i oppslaget`() {
        val reellMottaker = genererPersonident()
        val feilregistrert = genererPersonident()
        val request = slot<HentEnhetRequest>()
        every { organisasjonClient.hentEnhetForArbeidsfordelingGeografiskTilknytning(capture(request)) } returns
            EnhetDto(nummer = Enhetsnummer("2103"))
        val sak =
            sakHosEnhet(
                "1701",
                rolle(Rolletype.BIDRAGSMOTTAKER, bm),
                rolle(Rolletype.BARN, barn),
                rolle(Rolletype.REELMOTTAKER, reellMottaker),
                rolle(Rolletype.FEILREGISTRERT, feilregistrert),
            )

        arbeidsfordelingService.utførArbeidsfordeling(sak)

        request.captured.ident shouldBe bm
        request.captured.biidenter.shouldContainExactlyInAnyOrder(bm, barn, reellMottaker, feilregistrert)
        sak.eierfogd shouldBe "2103"
    }

    @Test
    fun `tar med tidligere parter fra rollehistorikken i oppslaget`() {
        val tidligereRm = genererPersonident()
        val request = slot<HentEnhetRequest>()
        every { organisasjonClient.hentEnhetForArbeidsfordelingGeografiskTilknytning(capture(request)) } returns
            EnhetDto(nummer = Enhetsnummer("2103"))
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

        request.captured.biidenter.shouldContainExactlyInAnyOrder(bm, barn, tidligereRm)
    }

    @Test
    fun `feiler uten å endre eierfogd når ingen part er kjent`() {
        val sak = sakHosEnhet("1701", rolle(Rolletype.BIDRAGSMOTTAKER, ukjentPart = UkjentPart.UK))

        shouldThrow<IllegalStateException> { arbeidsfordelingService.utførArbeidsfordeling(sak) }

        verify(exactly = 0) { organisasjonClient.hentEnhetForArbeidsfordelingGeografiskTilknytning(any()) }
        sak.eierfogd shouldBe "1701"
    }

    @Test
    fun `erstatter eiertilgangen på ny sak i stedet for å avslutte den`() {
        organisasjonGirEnhet("2103")
        val sak = sakHosEnhet("1701", rolle(Rolletype.BIDRAGSMOTTAKER, bm), rolle(Rolletype.BARN, barn))

        arbeidsfordelingService.utførArbeidsfordelingPåNySak(sak)

        sak.eierfogd shouldBe "2103"
        sak.tilganger.single().enhetsnummer shouldBe "2103"
        sak.tilganger.single().tilgangTomDato shouldBe null
    }
}
