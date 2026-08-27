package no.nav.bidrag.transport.dokument.forsendelse

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype

data class HentDokumentValgRequest(
    @Schema(enumAsRef = true) val soknadType: SoknadType? = null,
    @Schema(enumAsRef = true) val vedtakType: Vedtakstype? = null,
    @Schema(enumAsRef = true) val behandlingType: BehandlingType? = null,
    @Schema(enumAsRef = true) val soknadFra: SøktAvType? = null,
    val erFattetBeregnet: Boolean? = null,
    val erVedtakIkkeTilbakekreving: Boolean? = false,
    val vedtakId: String? = null,
    val behandlingId: String? = null,
    val soknadId: String? = null,
    val enhet: String? = null,
    val inneholderAldersjustering: Boolean? = false,
    val erOrkestrertVedtak: Boolean? = false,
    @Schema(enumAsRef = true) val stonadType: Stønadstype? = null,
    @Schema(enumAsRef = true) val engangsBelopType: Engangsbeløptype? = null,
    val forsendelseId: Long? = null,
) {
    fun erKlage() = vedtakType == Vedtakstype.KLAGE || soknadType == Vedtakstype.KLAGE.name

    val behandlingtypeKonvertert =
        when (behandlingType) {
            Engangsbeløptype.SAERTILSKUDD.name,
            Engangsbeløptype.SÆRTILSKUDD.name,
            Engangsbeløptype.SÆRBIDRAG.name,
            -> Engangsbeløptype.SÆRBIDRAG.name

            Engangsbeløptype.DIREKTE_OPPGJØR.name, Engangsbeløptype.DIREKTE_OPPGJOR.name -> Engangsbeløptype.DIREKTE_OPPGJØR.name

            else -> behandlingType
        }
}
