package no.nav.bidrag.tilgangskontroll.tjeneste

import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.bidrag.domene.enums.behandling.Behandlingstema
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.tilgangskontroll.konfigurasjon.UnleashFeatures
import no.nav.bidrag.tilgangskontroll.konsumer.MicrosoftGraphConsumer
import no.nav.bidrag.tilgangskontroll.konsumer.SakPipKonsumer
import no.nav.bidrag.tilgangskontroll.konsumer.TilgangsmaskinConsumer
import no.nav.bidrag.tilgangskontroll.model.graph.BrukerGrupperResponse
import no.nav.bidrag.tilgangskontroll.model.graph.Søknadsgruppe
import no.nav.bidrag.tilgangskontroll.model.kodeverk.Informasjonstilgang
import no.nav.bidrag.tilgangskontroll.model.tilgangsmaskin.TilgangsmaskinBulkResponse
import no.nav.bidrag.tilgangskontroll.model.tilgangsmaskin.TilgangsmaskinResultat
import no.nav.bidrag.tilgangskontroll.model.tilgangsmaskin.TilgangsmaskinResultatDetaljer
import no.nav.bidrag.transport.sak.BidragssakPipDto
import org.junit.experimental.runners.Enclosed
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.runner.RunWith

@RunWith(Enclosed::class)
@ExtendWith(MockKExtension::class)
class TilgangskontrollServiceTest {
    @MockK
    private lateinit var sakPipKonsumer: SakPipKonsumer

    @MockK
    private lateinit var tilgangsmaskinConsumer: TilgangsmaskinConsumer

    @MockK
    private lateinit var microsoftGraphConsumer: MicrosoftGraphConsumer

    @MockK
    private lateinit var kodeverkService: KodeverkService

    private lateinit var tilgangskontrollService: TilgangskontrollService

    @BeforeEach
    internal fun oppsett() {
        tilgangskontrollService =
            TilgangskontrollService(
                sakPipKonsumer,
                tilgangsmaskinConsumer,
                microsoftGraphConsumer,
                kodeverkService,
                listOf("Z999999"),
            )
        clearAllMocks()
        mockkStatic(TokenUtils::class)
        mockkObject(UnleashFeatures.TILGANG_TIL_AVSLUTTET_SAK)
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(TokenUtils::class)
        unmockkObject(UnleashFeatures.TILGANG_TIL_AVSLUTTET_SAK)
    }

    @Nested
    inner class SjekkTilgangOpprettSakUtenBmV2 {
        @Test
        fun `skal gi tilgang nar ident er i listen`() {
            every { TokenUtils.hentSaksbehandlerIdent() } returns "Z999999"

            val resultat = tilgangskontrollService.sjekkTilgangOpprettSakUtenBm()

            resultat.harTilgang shouldBe true
        }

        @Test
        fun `skal nekte tilgang nar ident ikke er i listen`() {
            every { TokenUtils.hentSaksbehandlerIdent() } returns "Z111111"

            val resultat = tilgangskontrollService.sjekkTilgangOpprettSakUtenBm()

            resultat.harTilgang shouldBe false
        }
    }

    @Nested
    inner class SjekkLesetilgangSakV2 {
        @Test
        fun `skal nekte tilgang til avsluttet sak nar feature toggle er av`() {
            val sakPip = mockk<BidragssakPipDto>()
            every { sakPip.avsluttet } returns true
            every { sakPip.saksnummer } returns Saksnummer("123456")
            every { UnleashFeatures.TILGANG_TIL_AVSLUTTET_SAK.isEnabled } returns false

            val resultat = tilgangskontrollService.sjekkLesetilgangSakV2(sakPip)

            resultat.harTilgang shouldBe false
        }

        @Test
        fun `skal gi tilgang til avsluttet sak nar feature toggle er pa`() {
            val sakPip = mockk<BidragssakPipDto>()
            every { sakPip.avsluttet } returns true
            every { sakPip.saksnummer } returns Saksnummer("123456")
            every { UnleashFeatures.TILGANG_TIL_AVSLUTTET_SAK.isEnabled } returns true

            val resultat = tilgangskontrollService.sjekkLesetilgangSakV2(sakPip)

            resultat.harTilgang shouldBe true
        }

        @Test
        fun `skal gi tilgang nar saken ikke er avsluttet`() {
            val sakPip = mockk<BidragssakPipDto>()
            every { sakPip.avsluttet } returns false
            every { sakPip.saksnummer } returns Saksnummer("123456")

            val resultat = tilgangskontrollService.sjekkLesetilgangSakV2(sakPip)

            resultat.harTilgang shouldBe true
        }
    }

    @Nested
    inner class SjekkTilgangSakV2 {
        @Test
        fun `skal gi tilgang nar badek sak og roller gir tilgang`() {
            val saksnr = "123456"
            val sakPip = mockk<BidragssakPipDto>()
            every { sakPip.avsluttet } returns false
            every { sakPip.saksnummer } returns Saksnummer(saksnr)
            every { sakPip.roller } returns listOf("rolle1")

            every { sakPipKonsumer.hentPipMetadata(saksnr) } returns sakPip
            every { TokenUtils.erApplikasjonsbruker() } returns false
            every { TokenUtils.hentSaksbehandlerIdent() } returns "Z999999"

            val tmResponse = mockk<TilgangsmaskinBulkResponse>()
            every { tmResponse.resultater } returns emptyList()
            every { tilgangsmaskinConsumer.evaluerKomplettRegelsettForFlereBrukere(any()) } returns tmResponse

            val resultat = tilgangskontrollService.sjekkTilgangSak(saksnr)

            resultat.harTilgang shouldBe true
        }
    }

    @Nested
    inner class SjekkTilgangPersonV2 {
        @Test
        fun `skal gi tilgang for ident nar roller gir tilgang`() {
            every { TokenUtils.erApplikasjonsbruker() } returns false
            every { TokenUtils.hentSaksbehandlerIdent() } returns "Z999999"

            val tmResponse = mockk<TilgangsmaskinBulkResponse>()
            every { tmResponse.resultater } returns emptyList()
            every { tilgangsmaskinConsumer.evaluerKomplettRegelsettForFlereBrukere(any()) } returns tmResponse

            val resultat = tilgangskontrollService.sjekkTilgangPerson(Personident("12345678910"))

            resultat.harTilgang shouldBe true
        }
    }

    @Nested
    inner class SjekkTilgangTemaV2 {
        @Test
        fun `skal sjekke tilgang til tema via graph consumer og gi tilgang når det er applikasjonsbruker`() {
            every { TokenUtils.erApplikasjonsbruker() } returns true
            every { TokenUtils.hentSaksbehandlerIdent() } returns "Z999999"

            val grupperResponse =
                BrukerGrupperResponse(
                    listOf(
                        mockk {
                            every { navn } returns "0000-GA-TEMA_BID"
                        },
                    ),
                )
            every { microsoftGraphConsumer.hentGrupperForBruker("Z999999") } returns grupperResponse

            val resultat = tilgangskontrollService.sjekkTilgangTema("BID")

            resultat.harTilgang shouldBe true
        }

        @Test
        fun `skal nekte tilgang til tema hvis bruker ikke har riktig gruppe`() {
            every { TokenUtils.erApplikasjonsbruker() } returns false
            every { TokenUtils.hentSaksbehandlerIdent() } returns "Z999999"

            val grupperResponse =
                BrukerGrupperResponse(
                    listOf(
                        mockk {
                            every { navn } returns "0000-GA-TEMA_FAR"
                        },
                    ),
                )
            every { microsoftGraphConsumer.hentGrupperForBruker("Z999999") } returns grupperResponse

            val resultat = tilgangskontrollService.sjekkTilgangTema("BID")

            resultat.harTilgang shouldBe false
        }
    }

    @Nested
    inner class SjekkTilgangAlleRollerV2 {
        @Test
        fun `skal tillate hvis det er applikasjonsbruker og ingen saksbehandlerIdent`() {
            every { TokenUtils.erApplikasjonsbruker() } returns true

            val resultat = tilgangskontrollService.sjekkTilgangAlleRollerV2(emptyList())

            resultat.harTilgang shouldBe true
        }

        @Test
        fun `skal evaluere mot tilgangsmaskin når man sjekker roller`() {
            every { TokenUtils.erApplikasjonsbruker() } returns false
            every { TokenUtils.hentSaksbehandlerIdent() } returns "Z999999"

            val tmResponse = mockk<TilgangsmaskinBulkResponse>()
            val tmResultat = mockk<TilgangsmaskinResultat>()
            val tmDetaljer = mockk<TilgangsmaskinResultatDetaljer>()
            every { tmResultat.status } returns 403
            every { tmResultat.detaljer } returns tmDetaljer
            every { tmDetaljer.begrunnelse } returns "Avslag"
            every { tmDetaljer.navIdent } returns "Z999999"
            every { tmResponse.resultater } returns listOf(tmResultat)

            every { tilgangsmaskinConsumer.evaluerKomplettRegelsettForFlereBrukere(listOf("rolle1")) } returns tmResponse

            val resultat = tilgangskontrollService.sjekkTilgangAlleRollerV2(listOf("rolle1"))

            resultat.harTilgang shouldBe false
        }
    }

    @Nested
    inner class SjekkTilgangSøknadsgruppe {
        @Test
        fun `skal nekte tilgang hvis navIdent mangler`() {
            every { TokenUtils.hentSaksbehandlerIdent() } returns null

            val resultat = tilgangskontrollService.sjekkTilgangSøknadsgruppe(Søknadsgruppe.BARNEBORTFØRING, null)

            resultat.harTilgang shouldBe false
        }

        @Test
        fun `skal gi tilgang hvis bruker har riktig enhetsgruppe for søknadsgruppe`() {
            every { TokenUtils.hentSaksbehandlerIdent() } returns "Z999999"

            val grupperResponse =
                BrukerGrupperResponse(
                    listOf(
                        mockk {
                            every { navn } returns "0000-GA-ENHET_2103"
                        },
                    ),
                )
            every { microsoftGraphConsumer.hentGrupperForBruker("Z999999") } returns grupperResponse

            val resultat = tilgangskontrollService.sjekkTilgangSøknadsgruppe(Søknadsgruppe.BARNEBORTFØRING, null)

            resultat.harTilgang shouldBe true
        }
    }

    @Nested
    inner class LesetilgangTilBehandlingstema {
        private val navident = "Z999999"
        private val bisysGruppe = "0000-GA-BISYS"
        private val leseGruppe = "0000-GA-LESE"
        private val bidragGruppe = "0000-GA-Bisys-Bidrag"

        private fun settOppAdgrupper(vararg grupper: String) {
            every { TokenUtils.hentSaksbehandlerIdent() } returns navident
            every { microsoftGraphConsumer.hentGrupperForBruker(navident) } returns
                BrukerGrupperResponse(grupper.map { mockk { every { navn } returns it } })
            every { kodeverkService.hentAdgruppe(Informasjonstilgang.BISYS) } returns bisysGruppe
            every { kodeverkService.hentAdgruppe(Informasjonstilgang.LESE) } returns leseGruppe
            every { kodeverkService.hentAdgruppe(Behandlingstema.BIDRAG) } returns bidragGruppe
        }

        @Test
        fun `skal gi tilgang når bruker har BISYS, LESE og riktig behandlingstema-gruppe`() {
            settOppAdgrupper(bisysGruppe, leseGruppe, bidragGruppe)

            val resultat = tilgangskontrollService.sjekkLesetilgangTilBehandlingstema(listOf(Behandlingstema.BIDRAG))

            resultat.harTilgang shouldBe true
        }

        @Test
        fun `skal nekte tilgang når bruker mangler BISYS`() {
            settOppAdgrupper(leseGruppe, bidragGruppe)

            val resultat = tilgangskontrollService.sjekkLesetilgangTilBehandlingstema(listOf(Behandlingstema.BIDRAG))

            resultat.harTilgang shouldBe false
        }

        @Test
        fun `skal nekte tilgang når bruker mangler LESE`() {
            settOppAdgrupper(bisysGruppe, bidragGruppe)

            val resultat = tilgangskontrollService.sjekkLesetilgangTilBehandlingstema(listOf(Behandlingstema.BIDRAG))

            resultat.harTilgang shouldBe false
        }

        @Test
        fun `skal nekte tilgang når bruker mangler AD-gruppe for behandlingstema`() {
            settOppAdgrupper(bisysGruppe, leseGruppe)

            val resultat = tilgangskontrollService.sjekkLesetilgangTilBehandlingstema(listOf(Behandlingstema.BIDRAG))

            resultat.harTilgang shouldBe false
        }
    }

    @Nested
    inner class BehandlingstilgangTilBehandlingstema {
        private val navident = "Z999999"
        private val bisysGruppe = "0000-GA-BISYS"
        private val behandleGruppe = "0000-GA-BEHANDLE"
        private val bidragGruppe = "0000-GA-Bisys-Bidrag"

        private fun settOppAdgrupper(vararg grupper: String) {
            every { TokenUtils.hentSaksbehandlerIdent() } returns navident
            every { microsoftGraphConsumer.hentGrupperForBruker(navident) } returns
                BrukerGrupperResponse(grupper.map { mockk { every { navn } returns it } })
            every { kodeverkService.hentAdgruppe(Informasjonstilgang.BISYS) } returns bisysGruppe
            every { kodeverkService.hentAdgruppe(Informasjonstilgang.BEHANDLE) } returns behandleGruppe
            every { kodeverkService.hentAdgruppe(Behandlingstema.BIDRAG) } returns bidragGruppe
        }

        @Test
        fun `skal gi tilgang når bruker har BISYS, BEHANDLE og riktig behandlingstema-gruppe`() {
            settOppAdgrupper(bisysGruppe, behandleGruppe, bidragGruppe)

            val resultat = tilgangskontrollService.sjekkSkrivetilgangTilBehandlingstema(listOf(Behandlingstema.BIDRAG))

            resultat.harTilgang shouldBe true
        }

        @Test
        fun `skal nekte tilgang når bruker mangler BISYS`() {
            settOppAdgrupper(behandleGruppe, bidragGruppe)

            val resultat = tilgangskontrollService.sjekkSkrivetilgangTilBehandlingstema(listOf(Behandlingstema.BIDRAG))

            resultat.harTilgang shouldBe false
        }

        @Test
        fun `skal nekte tilgang når bruker mangler BEHANDLE`() {
            settOppAdgrupper(bisysGruppe, bidragGruppe)

            val resultat = tilgangskontrollService.sjekkSkrivetilgangTilBehandlingstema(listOf(Behandlingstema.BIDRAG))

            resultat.harTilgang shouldBe false
        }

        @Test
        fun `skal nekte tilgang når bruker mangler AD-gruppe for behandlingstema`() {
            settOppAdgrupper(bisysGruppe, behandleGruppe)

            val resultat = tilgangskontrollService.sjekkSkrivetilgangTilBehandlingstema(listOf(Behandlingstema.BIDRAG))

            resultat.harTilgang shouldBe false
        }
    }

    @Nested
    inner class HentBrukertilganger {
        @Test
        fun `skal hente riktige boolean flagg basert på ad grupper`() {
            every { TokenUtils.hentSaksbehandlerIdent() } returns "Z999999"

            val grupperResponse =
                BrukerGrupperResponse(
                    listOf(
                        mockk { every { navn } returns "0000-GA-BISYS" },
                        mockk { every { navn } returns "0000-GA-LESE" },
                    ),
                )
            every { microsoftGraphConsumer.hentGrupperForBruker("Z999999") } returns grupperResponse

            every { kodeverkService.hentAdgruppe(Informasjonstilgang.BISYS) } returns "0000-GA-BISYS"
            every { kodeverkService.hentAdgruppe(Informasjonstilgang.UTLAND) } returns "0000-GA-UTLAND"
            every { kodeverkService.hentAdgruppe(Informasjonstilgang.LESE) } returns "0000-GA-LESE"
            every { kodeverkService.hentAdgruppe(Informasjonstilgang.BEHANDLE) } returns "0000-GA-BEHANDLE"
            every { kodeverkService.hentAdgruppe(Behandlingstema.FARSSKAP) } returns "0000-GA-FARSSKAP"
            every { kodeverkService.hentAdgruppe(Informasjonstilgang.ADMINISTRASJON) } returns "0000-GA-ADMINISTRASJON"
            every { kodeverkService.hentAdgruppe(any<Behandlingstema>()) } returns "ANNET"

            val tilganger = tilgangskontrollService.hentBrukertilganger()

            tilganger.bisysTilgang shouldBe true
            tilganger.utlandTilgang shouldBe false
            tilganger.leseSakTilgang shouldBe true
        }
    }
}
