package no.nav.bidrag.bbm.model

import no.nav.bidrag.domene.enums.behandling.Behandlingstype
import no.nav.bidrag.domene.enums.behandling.Behandlingstype.FORHOLDSMESSIG_FORDELING
import no.nav.bidrag.domene.enums.behandling.Behandlingstype.FORHOLDSMESSIG_FORDELING_KLAGE
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype

fun Behandlingstype.erForholdsmessigFordeling() = listOf(
    FORHOLDSMESSIG_FORDELING,
    FORHOLDSMESSIG_FORDELING_KLAGE,
).contains(this)

fun konverterTilStønadstype(kode: String) = when (kode) {
    "BB", "OB" -> Stønadstype.BIDRAG
    "18" -> Stønadstype.BIDRAG18AAR
    "FO" -> Stønadstype.FORSKUDD
    "EB" -> Stønadstype.EKTEFELLEBIDRAG
    else -> null
}

fun Stønadstype.konverterTilBisyskode() = when (this) {
    Stønadstype.BIDRAG -> listOf("BB", "OB")
    Stønadstype.BIDRAG18AAR -> listOf("18")
    Stønadstype.FORSKUDD -> listOf("FO")
    Stønadstype.EKTEFELLEBIDRAG -> listOf("EB")
    else -> emptyList()
}

fun Engangsbeløptype.konverterTilBisyskode() = when (this) {
    Engangsbeløptype.SÆRBIDRAG -> "SB"
    Engangsbeløptype.TILBAKEKREVING -> "TB"
    Engangsbeløptype.GEBYR_MOTTAKER, Engangsbeløptype.GEBYR_SKYLDNER -> "GE"
    else -> null
}
