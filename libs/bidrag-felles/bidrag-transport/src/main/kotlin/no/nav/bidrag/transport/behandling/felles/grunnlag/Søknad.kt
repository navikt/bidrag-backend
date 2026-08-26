package no.nav.bidrag.transport.behandling.felles.grunnlag

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.behandling.Behandlingstema
import no.nav.bidrag.domene.enums.behandling.Behandlingstype
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.vedtak.BeregnTil
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.enums.vedtak.VirkningstidspunktÅrsakstype
import java.time.LocalDate
import java.time.YearMonth

data class SøknadGrunnlag(
    val mottattDato: LocalDate,
    val søktFraDato: LocalDate,
    val søktAv: SøktAvType,
    @Schema(
        description =
            "Dato når klage ble mottatt. Kan være ulik mottattDato hvis behandlingen gjelder særbidrag." +
                "I særbidrag så er det mottatt dato på originale søknad som gjelder",
    )
    val klageMottattDato: LocalDate? = null,
    @Schema(
        description =
            "Opprinnelig vedtakstype hvis behandlingen gjelder klage." +
                " Dette er relevant for beregning av blant annet for særbidrag" +
                " hvor resultatet kan være lavere enn forskuddssats hvis vedtakstype er ENDRING",
    )
    val opprinneligVedtakstype: Vedtakstype? = null,
    val begrensetRevurdering: Boolean = false,
    val egetTiltak: Boolean = false,
    val paragraf35c: Boolean = false,
    val privatAvtale: Boolean = false,
    val innkrevingsgrunnlag: Boolean = false,
    val behandlingstype: Behandlingstype? = null,
    val behandlingstema: Behandlingstema? = null,
    // Søknadsid og saksnummer er nyttig ved FF hvor vedtak kan inneholde flere søknader
    val søknadsid: Long? = null,
    val saksnummer: String? = null,
    val behandlerenhet: String? = null,
) : GrunnlagInnhold

data class VirkningstidspunktGrunnlag(
    val virkningstidspunkt: LocalDate,
    val opprinneligVirkningstidspunkt: LocalDate? = null,
    val opphørsdato: LocalDate? = null,
    val beregnTilDato: YearMonth? = null,
    val årsak: VirkningstidspunktÅrsakstype? = null,
    val avslag: Resultatkode? = null,
    val beregnTil: BeregnTil? = null,
) : GrunnlagInnhold

data class BehandlingDetaljerGrunnlag(
    val opprettetForholdsmessigFordeling: Boolean,
    val fatteVedtakRevurderingsbarn: FatteVedtakRevurderingsbarn? = null,
) : GrunnlagInnhold

data class FatteVedtakRevurderingsbarn(
    val foreslåttFatteVedtak: Boolean = false,
    // Tilfeller hvor BP har full evne i alle perioder.
    // Da skal ikke saksbehandler få valg om å fatte vedtak for revurderingsbarn fordi det ikke gjøres noe beregning for revurderingsbarn
    @get:JsonAlias("kunneFatteVedtak")
    val bleFFTrukket: Boolean = true,
    val manueltOverstyrtForslagBegrunnelse: String? = null,
) {
    @get:JsonIgnore
    val kunneFatteVedtak get() = !bleFFTrukket

    @get:JsonIgnore
    val skalFatteVedtakForRevurderingsbarn get() =
        if (foreslåttFatteVedtak) {
            manueltOverstyrtForslagBegrunnelse == null
        } else {
            manueltOverstyrtForslagBegrunnelse !=
                null
        }
}
