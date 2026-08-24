@file:Suppress("ktlint:standard:kdoc")

package no.nav.bidrag.dokument.bestilling.model

import no.nav.bidrag.domene.enums.diverse.Språk
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.util.visningsnavn

fun Inntektsrapportering.tilLegacyKode() =
    when (this) {
        Inntektsrapportering.KONTANTSTØTTE -> "KONT"
        Inntektsrapportering.LIGNINGSINNTEKT -> "LIGS"
        Inntektsrapportering.KAPITALINNTEKT -> "KAPS"
        Inntektsrapportering.KAPITALINNTEKT_EGNE_OPPLYSNINGER -> "KIEO"
        Inntektsrapportering.SKJØNN_MANGLER_DOKUMENTASJON -> "MDOK"
        Inntektsrapportering.SKJØNN_MANGLENDE_BRUK_AV_EVNE -> "EVNE"
        Inntektsrapportering.SYKEPENGER -> "SP"
        Inntektsrapportering.PENSJON -> "PE"
        Inntektsrapportering.INNTEKTSOPPLYSNINGER_FRA_ARBEIDSGIVER -> "AG"
        Inntektsrapportering.SMÅBARNSTILLEGG -> "ESBT"
        Inntektsrapportering.PERSONINNTEKT_EGNE_OPPLYSNINGER -> "PIEO"
        Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT -> "SAK"
        Inntektsrapportering.UTVIDET_BARNETRYGD -> "UBAT"
        Inntektsrapportering.OVERGANGSSTØNAD -> "EFOS"
        else -> this.legacyKode ?: name
    }

val visningsnavnSomKreverÅrstall = listOf(Inntektsrapportering.LIGNINGSINNTEKT)

fun Inntektsrapportering.visningsnavnBruker(
    språk: Språk,
    årstall: Int?,
) = if (visningsnavnSomKreverÅrstall.contains(this)) {
    "${visningsnavn.bruker[språk]} ${årstall ?: ""}".trim()
} else {
    visningsnavn.bruker[språk] ?: ""
}

/**
 * IF
 * (BI_perForskVtak_resKd(SYS_TableRow) = 'AHI'
 * OR BI_perForskVtak_resKd(SYS_TableRow) = 'AMD'
 * OR BI_perForskVtak_resKd(SYS_TableRow) = 'AUT'
 * OR BI_perForskVtak_resKd(SYS_TableRow) = 'AIR'
 * OR BI_perForskVtak_resKd (SYS_TableRow) = 'AIO'
 * OR BI_perForskVtak_resKd(SYS_TableRow) = 'ASA'
 * OR BI_perForskVtak_resKd(SYS_TableRow) = 'ABA'
 * OR BI_perForskVtak_resKd(SYS_TableRow) = 'AFT'
 * OR BI_perForskVtak_resKd(SYS_TableRow) = 'AFU'
 * OR BI_perForskVtak_resKd (SYS_TableRow) = 'ABI'
 * OR BI_perForskVtak_resKd (SYS_TableRow) = 'ABE'
 * OR BI_perForskVtak_resKd (SYS_TableRow) = 'AUY'
 * OR BI_perForskVtak_resKd (SYS_TableRow) = 'A'
 * OR BI_perForskVtak_resKd(SYS_TableRow) = 'OHI'
 * OR BI_perForskVtak_resKd(SYS_TableRow) = 'OMD'
 * OR BI_perForskVtak_resKd(SYS_TableRow) = 'OUT'
 * OR BI_perForskVtak_resKd(SYS_TableRow) = 'OIR'
 * OR BI_perForskVtak_resKd (SYS_TableRow) = 'OIO'
 * OR BI_perForskVtak_resKd(SYS_TableRow) = 'OSA'
 * OR BI_perForskVtak_resKd(SYS_TableRow) = 'OBA'
 * OR BI_perForskVtak_resKd(SYS_TableRow) = 'OFT'
 * OR BI_perForskVtak_resKd(SYS_TableRow) = 'OFU'
 * OR BI_perForskVtak_resKd (SYS_TableRow) = 'OBI'
 * OR BI_perForskVtak_resKd (SYS_TableRow) = 'OBE'
 * OR BI_perForskVtak_resKd (SYS_TableRow) = 'OUY'
 * OR BI_perForskVtak_resKd (SYS_TableRow) = 'OH'
 * OR BI_perForskVtak_resKd(SYS_TableRow) ='50'
 * OR BI_perForskVtak_resKd(SYS_TableRow) ='75')
 * THEN
 * INCLUDE
 * ENDIF
 **/
