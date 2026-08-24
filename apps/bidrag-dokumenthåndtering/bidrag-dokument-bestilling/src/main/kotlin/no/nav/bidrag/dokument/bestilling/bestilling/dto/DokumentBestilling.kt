package no.nav.bidrag.dokument.bestilling.bestilling.dto

import no.nav.bidrag.dokument.bestilling.model.tilLegacyKode
import no.nav.bidrag.dokument.bestilling.model.visningsnavnBruker
import no.nav.bidrag.dokument.bestilling.tjenester.sammenstillDeMedSammeVerdi
import no.nav.bidrag.dokument.bestilling.tjenester.sammenstillDeMedSammeVerdiAndelUnderhold
import no.nav.bidrag.dokument.bestilling.tjenester.sammenstillDeMedSammeVerdiInntekter
import no.nav.bidrag.dokument.bestilling.tjenester.sammenstillDeMedSammeVerdiUnderhold
import no.nav.bidrag.domene.enums.diverse.Språk
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.transport.dokumentmaler.InntektPeriode
import no.nav.bidrag.transport.dokumentmaler.VedtakBarn
import java.math.BigDecimal

data class DokumentBestillingResult(
    val dokumentReferanse: String,
    val journalpostId: String? = null,
    val innhold: ByteArray? = null,
    val bestillingSystem: String,
) {
    override fun toString(): String = "dokumentReferanse='$dokumentReferanse', journalpostId=$journalpostId, innhold=${innhold?.size}, bestillingSystem='$bestillingSystem'"
}

val InntektPeriode.opprinneligPeriode get() = inntektOpprinneligPerioder.minByOrNull { it.fom }
val InntektPeriode.inntektPeriode get() = inntektPerioder.minByOrNull { it.fom }

val InntektPeriode.beskrivelse
    get() =
        when {
            typer.isNotEmpty() -> typer.first().visningsnavnBruker(Språk.NB, beløpÅr ?: opprinneligPeriode?.fom?.year)
            periodeTotalinntekt == true -> "Personens beregningsgrunnlag i perioden"
            nettoKapitalInntekt == true -> "Netto positive kapitalinntekter"
            else -> ""
        }
val InntektPeriode.beløpKode
    get() =
        when {
            typer.isNotEmpty() -> typer.first().tilLegacyKode()
            periodeTotalinntekt == true -> "XINN"
            nettoKapitalInntekt == true -> "XKAP"
            else -> ""
        }

val VedtakBarn.samværsperioder get() = stønadsendringer.flatMap { it.vedtakPerioder.map { it.samvær } }.filterNotNull().sammenstillDeMedSammeVerdi()
val VedtakBarn.underholdskostnadperioder get() = stønadsendringer.flatMap { it.vedtakPerioder.map { it.underhold } }.filterNotNull().sammenstillDeMedSammeVerdiUnderhold()
val VedtakBarn.andelUnderholdPerioder get() = stønadsendringer.flatMap { it.vedtakPerioder.map { it.andelUnderhold } }.filterNotNull().sammenstillDeMedSammeVerdiAndelUnderhold()
val VedtakBarn.inntektsperioder get() =
    stønadsendringer
        .flatMap { it.vedtakPerioder.flatMap { it.inntekter } }
        .sammenstillDeMedSammeVerdiInntekter()
        .filter { it.rolle != Rolletype.BARN || it.beløp > BigDecimal.ZERO }
