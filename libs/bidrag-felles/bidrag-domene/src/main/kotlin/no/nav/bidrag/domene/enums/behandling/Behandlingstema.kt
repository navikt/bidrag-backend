package no.nav.bidrag.domene.enums.behandling

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype

// Enum verdier av tabellen t_kode_sokn_gr i bisys
// Dette er enum som er 1-1 med bisys sine søknad gruppe kode. Brukes i blant annet bidrag-behandling og ved opprettelse av oppgaver i bidrag-arbeidsflyt
@Schema(enumAsRef = true)
enum class Behandlingstema(
    val bisysKode: String,
    val bisysDekode: String,
) {
    AVSKRIVNING("AV", "Avskrivning direkte betalt"),
    BIDRAG("BI", "Bidrag"),
    BIDRAG_PLUSS_TILLEGGSBIDRAG("BT", "Bidrag, tilleggsbidrag"),
    DIREKTE_OPPGJØR("DO", "Direkte oppgjør"),
    EKTEFELLEBIDRAG("EB", "Ektefellebidrag"),
    ETTERGIVELSE("EG", "Ettergivelse"),
    ERSTATNING("ER", "Erstatning"),
    FARSSKAP("FA", "Farskap"),
    KUNNSKAP_OM_BIOLOGISK_FAR("FB", "Kunnskap om biologisk far"),
    FORSKUDD("FO", "Forskudd"),
    GEBYR("GB", "Gebyr"),
    INNKREVING("IK", "Innkreving"),
    MORSSKAP("MO", "Morskap"),
    MOTREGNING("MR", "Motregning"),
    OPPFOSTRINGSBIDRAG("OB", "Oppfostringsbidrag"),
    REFUSJON_BIDRAG("RB", "Refusjon bidrag"),
    SAKSOMKOSTNINGER("SO", "Saksomkostninger"),
    SÆRBIDRAG("ST", "Særbidrag"),
    TILLEGGSBIDRAG("TB", "Tilleggsbidrag"),
    TILBAKEKREVING_ETTERGIVELSE("TE", "Tilbakekreving,ettergivelse"),
    TILBAKEKREVING("TK", "Tilbakekreving"),
    TILBAKEKREVING_BIDRAG("M1", "Tilbakekreving bidrag"),
    BIDRAG_18_ÅR_PLUSS_TILLEGGSBIDRAG("T1", "18 år, tilleggsbidrag"),
    BIDRAG_18_ÅR("18", "18 år"),
    REISEKOSTNADER("RK", "Reisekostnader"),

    ;

    companion object {
        fun fraKode(kode: String): Behandlingstema? =
            try {
                enumValues<Behandlingstema>().find { res -> res.bisysKode == kode } ?: Behandlingstema.valueOf(kode)
            } catch (e: Exception) {
                null
            }
    }
}

fun Stønadstype.tilBehandlingstema(): Behandlingstema =
    when (this) {
        Stønadstype.BIDRAG -> Behandlingstema.BIDRAG
        Stønadstype.BIDRAG18AAR -> Behandlingstema.BIDRAG_18_ÅR
        Stønadstype.MOTREGNING -> Behandlingstema.MOTREGNING
        Stønadstype.EKTEFELLEBIDRAG -> Behandlingstema.EKTEFELLEBIDRAG
        Stønadstype.OPPFOSTRINGSBIDRAG -> Behandlingstema.OPPFOSTRINGSBIDRAG
        Stønadstype.FORSKUDD -> Behandlingstema.FORSKUDD
    }

fun Behandlingstema.tilStønadstype(): Stønadstype? =
    when (this) {
        Behandlingstema.BIDRAG,
        Behandlingstema.BIDRAG_PLUSS_TILLEGGSBIDRAG,
        -> Stønadstype.BIDRAG

        Behandlingstema.BIDRAG_18_ÅR, Behandlingstema.BIDRAG_18_ÅR_PLUSS_TILLEGGSBIDRAG -> Stønadstype.BIDRAG18AAR

        Behandlingstema.EKTEFELLEBIDRAG -> Stønadstype.EKTEFELLEBIDRAG

        Behandlingstema.OPPFOSTRINGSBIDRAG -> Stønadstype.OPPFOSTRINGSBIDRAG

        Behandlingstema.MOTREGNING -> Stønadstype.MOTREGNING

        Behandlingstema.FORSKUDD -> Stønadstype.FORSKUDD

        else -> null
    }

fun Behandlingstema.tilBeskrivelse(medInnkreving: Boolean = false): String? =
    when (this) {
        Behandlingstema.BIDRAG_18_ÅR -> if (medInnkreving) "18 år, innkreving" else "18 år"
        Behandlingstema.SÆRBIDRAG -> if (medInnkreving) "Særtilskudd,innkreving" else "Særtilskudd"
        Behandlingstema.BIDRAG_18_ÅR_PLUSS_TILLEGGSBIDRAG -> if (medInnkreving) "18 år, tilleggsb,innkreving" else "18 år, tilleggsb."
        Behandlingstema.TILLEGGSBIDRAG -> if (medInnkreving) "Tilleggsbidrag,innkreving" else "Tilleggsbidrag"
        Behandlingstema.BIDRAG -> if (medInnkreving) "Bidrag,innkreving" else "Bidrag"
        Behandlingstema.BIDRAG_PLUSS_TILLEGGSBIDRAG -> if (medInnkreving) "Bidrag,tilleggsb,innkreving" else "Bidrag,tilleggsb"
        Behandlingstema.EKTEFELLEBIDRAG -> if (medInnkreving) "Ektefellebidrag m/innkr" else "Ektefellebidrag u/innkr"
        Behandlingstema.DIREKTE_OPPGJØR -> "Direkte oppgjør"
        Behandlingstema.AVSKRIVNING -> "Avskrivning direkte betalt"
        Behandlingstema.ETTERGIVELSE -> "Ettergivelse"
        Behandlingstema.ERSTATNING -> "Erstatning"
        Behandlingstema.FARSSKAP -> "Farskap"
        Behandlingstema.KUNNSKAP_OM_BIOLOGISK_FAR -> "Kunnskap om biologisk far"
        Behandlingstema.FORSKUDD -> "Forskudd"
        Behandlingstema.GEBYR -> "Gebyr"
        Behandlingstema.INNKREVING -> "Innkreving"
        Behandlingstema.MORSSKAP -> "Morskap"
        Behandlingstema.MOTREGNING -> "Motregning"
        Behandlingstema.OPPFOSTRINGSBIDRAG -> "Oppfostringsbidrag,innkreving"
        Behandlingstema.REFUSJON_BIDRAG -> "Refusjon bidrag"
        Behandlingstema.REISEKOSTNADER -> "Reisekostnader"
        Behandlingstema.SAKSOMKOSTNINGER -> "Saksomkostninger"
        Behandlingstema.TILBAKEKREVING_ETTERGIVELSE -> "Tilbakekr,ettergivelse"
        Behandlingstema.TILBAKEKREVING -> "Tilbakekreving"
        Behandlingstema.TILBAKEKREVING_BIDRAG -> "Tilbakekr,bidrag"
    }

fun tilBeskrivelseBehandlingstema(
    stønadstype: Stønadstype? = null,
    engangsbeløptype: Engangsbeløptype? = null,
    behandlingstema: Behandlingstema? = null,
): String? =
    when (stønadstype) {
        Stønadstype.FORSKUDD -> {
            "Bidragsforskudd"
        }

        Stønadstype.BIDRAG -> {
            "Barnebidrag"
        }

        Stønadstype.BIDRAG18AAR -> {
            "Barnebidrag 18 år"
        }

        Stønadstype.EKTEFELLEBIDRAG -> {
            "Ektefellebidrag"
        }

        Stønadstype.OPPFOSTRINGSBIDRAG -> {
            "Oppfostringbidrag"
        }

        Stønadstype.MOTREGNING -> {
            "Motregning"
        }

        else -> {
            when (engangsbeløptype) {
                Engangsbeløptype.SAERTILSKUDD, Engangsbeløptype.SÆRTILSKUDD, Engangsbeløptype.SÆRBIDRAG -> {
                    "Særbidrag"
                }

                Engangsbeløptype.DIREKTE_OPPGJOR, Engangsbeløptype.DIREKTE_OPPGJØR -> {
                    "Direkte oppgjør"
                }

                Engangsbeløptype.ETTERGIVELSE -> {
                    "Ettergivelse"
                }

                Engangsbeløptype.ETTERGIVELSE_TILBAKEKREVING -> {
                    "Ettergivelse tilbakekreving"
                }

                Engangsbeløptype.GEBYR_MOTTAKER, Engangsbeløptype.GEBYR_SKYLDNER -> {
                    "Gebyr"
                }

                Engangsbeløptype.TILBAKEKREVING -> {
                    "Tilbakekreving"
                }

                else -> {
                    behandlingstema
                        ?.name
                        ?.lowercase()
                        ?.replace("_", " ")
                        ?.replaceFirstChar { it.uppercase() }
                }
            }
        }
    }
