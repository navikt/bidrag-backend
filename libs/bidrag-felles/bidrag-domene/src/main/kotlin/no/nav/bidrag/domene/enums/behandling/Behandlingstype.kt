package no.nav.bidrag.domene.enums.behandling

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype

typealias BisysSøknadstype = Behandlingstype

// Enum verdier av kodetabellen t_kode_sokn_type
// Ved konvertering til Vedtakstype så blir noen detaljer borte (feks om det er begrenset revurdering eller revurdering).
// Dette er enum som er 1-1 med bisys sine søknadstyper. Brukes i blant annet bidrag-behandling
@Schema(enumAsRef = true)
enum class Behandlingstype(
    val bisysKode: String,
    val bisysDekode: String,
    val erKlageEllerOmgjøring: Boolean,
) {
    ENDRING("EN", "Endring", false),
    EGET_TILTAK("ET", "Eget tiltak", false),
    SØKNAD("FA", "Søknad", false),
    INNKREVINGSGRUNNLAG("IG", "Innkr.grunnlag", false),
    FORHOLDSMESSIG_FORDELING("FF", "Forholdsmessig fordeling", false),
    FORHOLDSMESSIG_FORDELING_KLAGE("FK", "Forholdsmessig fordeling klage", true),
    ALDERSJUSTERING("AJ", "Aldersjustering", false),
    INDEKSREGULERING("IR", "Indeksregulering", false),
    KLAGE_BEGRENSET_SATS("KB", "Klage begr sats", true),
    KLAGE("KL", "Klage", true),
    FØLGER_KLAGE("KM", "Følger klage", true),
    KORRIGERING("KR", "Korrigering", false),
    KONVERTERING("KV", "Konvertering", false),
    OPPHØR("OH", "Opphør", false),
    PRIVAT_AVTALE("PA", "Privat avtale", false),
    BEGRENSET_REVURDERING("RB", "Begr.revurd.", false),
    REVURDERING("RF", "Revurdering", false),
    OPPJUSTERT_FORSKUDD("OF", "Oppjust forsk", false),
    OMGJØRING("OM", "Omgjøring eget tiltak", true),
    OMGJØRING_BEGRENSET_SATS("OB", "Omgjøring begr sats", true),
    PARAGRAF_35_C("35", "§ 35c", true),
    PARAGRAF_35_C_BEGRENSET_SATS("3B", "§ 35c begrenset sats", true),
    MÅNEDLIG_PÅLOP("MP", "Månedlig påløp", false),
    ;

    fun tilVedtakstype() =
        when (this) {
            INDEKSREGULERING -> Vedtakstype.INDEKSREGULERING
            ALDERSJUSTERING -> Vedtakstype.ALDERSJUSTERING
            OPPHØR -> Vedtakstype.OPPHØR
            SØKNAD -> Vedtakstype.FASTSETTELSE
            INNKREVINGSGRUNNLAG, PRIVAT_AVTALE -> Vedtakstype.INNKREVING
            KLAGE_BEGRENSET_SATS, KLAGE, FØLGER_KLAGE -> Vedtakstype.KLAGE
            REVURDERING, FORHOLDSMESSIG_FORDELING, FORHOLDSMESSIG_FORDELING_KLAGE -> Vedtakstype.REVURDERING
            else -> Vedtakstype.ENDRING
        }

    fun erBegrensetRevurdering() =
        listOf(
            PARAGRAF_35_C_BEGRENSET_SATS,
            OMGJØRING_BEGRENSET_SATS,
            KLAGE_BEGRENSET_SATS,
            REVURDERING,
            BEGRENSET_REVURDERING,
        ).contains(this)

    fun erForholdsmessigFordeling() =
        listOf(
            FORHOLDSMESSIG_FORDELING,
            FORHOLDSMESSIG_FORDELING_KLAGE,
        ).contains(this)

    companion object {
        fun fraKode(kode: String): Behandlingstype? =
            try {
                enumValues<Behandlingstype>().find { res -> res.bisysKode == kode } ?: Behandlingstype.valueOf(kode)
            } catch (e: Exception) {
                null
            }
    }
}
