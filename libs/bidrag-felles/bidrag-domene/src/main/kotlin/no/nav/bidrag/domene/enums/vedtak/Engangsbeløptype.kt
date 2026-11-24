package no.nav.bidrag.domene.enums.vedtak

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class Engangsbeløptype {
    @Deprecated("DIREKTE_OPPGJOR skal ikke brukes", ReplaceWith("DIREKTE_OPPGJØR"))
    DIREKTE_OPPGJOR,
    DIREKTE_OPPGJØR,
    ETTERGIVELSE,
    ETTERGIVELSE_TILBAKEKREVING,
    GEBYR_MOTTAKER,
    GEBYR_SKYLDNER,
    INNKREVING_GJELD,

    TILBAKEKREVING,
    TILBAKEKREVING_BIDRAG,

    @Deprecated("SAERTILSKUDD skal ikke brukes", ReplaceWith("SÆRBIDRAG"))
    SAERTILSKUDD,

    @Deprecated("SÆRTILSKUDD skal ikke brukes", ReplaceWith("SÆRBIDRAG"))
    SÆRTILSKUDD,
    SÆRBIDRAG,
}

fun Engangsbeløptype.konverterFraDeprekerteVerdier() = when (this) {
    Engangsbeløptype.SÆRBIDRAG, Engangsbeløptype.SÆRTILSKUDD, Engangsbeløptype.SAERTILSKUDD -> Engangsbeløptype.SÆRBIDRAG
    Engangsbeløptype.DIREKTE_OPPGJØR, Engangsbeløptype.DIREKTE_OPPGJOR -> Engangsbeløptype.DIREKTE_OPPGJØR
    else -> this
}
