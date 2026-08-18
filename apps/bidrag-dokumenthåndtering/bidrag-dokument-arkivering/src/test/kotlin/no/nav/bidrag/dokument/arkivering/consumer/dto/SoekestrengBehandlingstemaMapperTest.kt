package no.nav.bidrag.dokument.arkivering.consumer.dto

import no.nav.bidrag.dokument.arkivering.dto.Behandlingstema
import no.nav.bidrag.dokument.arkivering.dto.Soekestreng.Companion.toBehandlingstema
import no.nav.bidrag.dokument.arkivering.dto.Tema
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension

@DisplayName("SoekestrengBehandlingstemaMapper")
@ExtendWith(MockitoExtension::class)
class SoekestrengBehandlingstemaMapperTest {
    @DisplayName("Skal mappe til kode for barnebortføring")
    @Test
    fun skalMappeTilKodeForBarnebortføring() {
        val innhold = "Utbetalingsvedtak settes på hold pga barnebortføring."
        val behandlingstema = toBehandlingstema(innhold, Tema.BID.toString())
        Assertions
            .assertThat(behandlingstema)
            .withFailMessage(
                "Feil behandlingstema for barnebortføring: forventet <%s>, fikk <%s>",
                Behandlingstema.BARNEBORTFOERING.kode,
                behandlingstema,
            ).isEqualTo(Behandlingstema.BARNEBORTFOERING.kode)
    }

    @DisplayName("Skal mappe til kode for oppfostringsbidrag")
    @Test
    fun skalMappeTilKodeForOppfostringsbidrag() {
        val innhold = "Søknad om oppfostringsbidrag."
        val behandlingstema = toBehandlingstema(innhold, Tema.BID.toString())
        Assertions
            .assertThat(behandlingstema)
            .withFailMessage(
                "Feil behandlingstema for oppfostringsbidrag: forventet <%s>, fikk <%s>",
                Behandlingstema.OPPFOSTRINGSBIDRAG.kode,
                behandlingstema,
            ).isEqualTo(Behandlingstema.OPPFOSTRINGSBIDRAG.kode)
    }

    @DisplayName("Skal mappe til kode for ektefellebidrag")
    @Test
    fun skalMappeTilKodeForEktefelle() {
        val innhold = "Ektefelle trenger kontanter..."
        val behandlingstema = toBehandlingstema(innhold, Tema.BID.toString())
        Assertions
            .assertThat(behandlingstema)
            .withFailMessage(
                "Feil behandlingstema for ektefellebidrag: forventet <%s>, fikk <%s>",
                Behandlingstema.EKTEFELLE.kode,
                behandlingstema,
            ).isEqualTo(Behandlingstema.EKTEFELLE.kode)
    }

    @DisplayName("Skal mappe til kode for foreldrepenger")
    @Test
    fun skalMappeTilKodeForForeldrepenger() {
        val innhold = "Lommepenger og foreldrepenger..."
        val behandlingstema = toBehandlingstema(innhold, Tema.BID.toString())
        Assertions
            .assertThat(behandlingstema)
            .withFailMessage(
                "Feil behandlingstema for foreldrepenger: forventet <%s>, fikk <%s>",
                Behandlingstema.FORELDREPENGER.kode,
                behandlingstema,
            ).isEqualTo(Behandlingstema.FORELDREPENGER.kode)
    }

    @DisplayName("Skal mappe til kode for engangsstønad")
    @Test
    fun skalMappeTilKodeForEngangsstoenad() {
        val innhold = "Løst og fast om engangsstønad..."
        val behandlingstema = toBehandlingstema(innhold, Tema.BID.toString())
        Assertions
            .assertThat(behandlingstema)
            .withFailMessage(
                "Feil behandlingstema for engangsstønad: forventet <%s>, fikk <%s>",
                Behandlingstema.ENGANGSSTOENAD.kode,
                behandlingstema,
            ).isEqualTo(Behandlingstema.ENGANGSSTOENAD.kode)
    }

    @DisplayName("Skal mappe til kode for bidrag inklusiv farskap")
    @Test
    fun skalMappeTilKodeForBidragInklusivFarskap() {
        val innhold = "Hva skjera bagera?"
        val behandlingstema = toBehandlingstema(innhold, Tema.FAR.toString())
        Assertions
            .assertThat(behandlingstema)
            .withFailMessage(
                "Feil behandlingstema for bidrag inklusiv farskap: forventet <%s>, fikk <%s>",
                Behandlingstema.BIDRAG_INKLUSIV_FARSKAP.kode,
                behandlingstema,
            ).isEqualTo(Behandlingstema.BIDRAG_INKLUSIV_FARSKAP.kode)
    }

    @DisplayName("Skal mappe til kode for bidrag utland eksklusiv farskap")
    @Test
    fun skalMappeTilKodeForBidragUtlandEksklusivFarskap() {
        val innhold = "Ich bin ein Berliner - utland.."
        val behandlingstema = toBehandlingstema(innhold, Tema.BID.toString())
        Assertions
            .assertThat(behandlingstema)
            .withFailMessage(
                "Feil behandlingstema for bidrag utland eksklusiv farskap: forventet <%s>, fikk <%s>",
                Behandlingstema.BIDRAG_UTLAND_EKSKLUSIV_FARSKAP.kode,
                behandlingstema,
            ).isEqualTo(Behandlingstema.BIDRAG_UTLAND_EKSKLUSIV_FARSKAP.kode)
    }

    @DisplayName("Skal mappe til kode for bidrag  eksklusiv farskap")
    @Test
    fun skalMappeTilKodeForBidragEksklusivFarskap() {
        val innhold = "Dette handler ikke om farskap.."
        val behandlingstema = toBehandlingstema(innhold, Tema.BID.toString())
        Assertions
            .assertThat(behandlingstema)
            .withFailMessage(
                "Feil behandlingstema for bidrag eksklusiv farskap: forventet <%s>, fikk <%s>",
                Behandlingstema.BIDRAG_EKSKLUSIV_FARSKAP.kode,
                behandlingstema,
            ).isEqualTo(Behandlingstema.BIDRAG_EKSKLUSIV_FARSKAP.kode)
    }
}
