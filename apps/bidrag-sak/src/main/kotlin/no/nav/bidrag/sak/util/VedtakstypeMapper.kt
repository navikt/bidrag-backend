package no.nav.bidrag.sak.util

import no.nav.bidrag.domene.enums.vedtak.Vedtakstype

// Mapper sokn_type-kode fra T_BLANKETT til Vedtakstype, basert på bestemVedtakType() i bisys.
fun String.tilVedtakstype(): Vedtakstype? = when (this) {
    "FA" -> Vedtakstype.FASTSETTELSE
    "IG", "PA" -> Vedtakstype.INNKREVING
    "IR" -> Vedtakstype.INDEKSREGULERING
    "KB", "KL", "KM" -> Vedtakstype.KLAGE
    "RB", "RF", "FF", "FK" -> Vedtakstype.REVURDERING
    "OH" -> Vedtakstype.OPPHØR
    "OA" -> Vedtakstype.ALDERSOPPHØR
    "OF", "AJ" -> Vedtakstype.ALDERSJUSTERING
    "EN" -> Vedtakstype.ENDRING
    else -> null
}
