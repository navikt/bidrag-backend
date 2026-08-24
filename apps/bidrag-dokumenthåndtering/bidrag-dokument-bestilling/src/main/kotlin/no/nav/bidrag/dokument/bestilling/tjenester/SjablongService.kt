package no.nav.bidrag.dokument.bestilling.tjenester

import no.nav.bidrag.commons.service.AppContext
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.dokument.bestilling.consumer.SjablonConsumer
import no.nav.bidrag.dokument.bestilling.consumer.dto.SjablonData
import no.nav.bidrag.dokument.bestilling.consumer.dto.SjablongType
import no.nav.bidrag.dokument.bestilling.consumer.dto.SjablongerDto
import no.nav.bidrag.dokument.bestilling.consumer.dto.hentPerioderForSjabloner
import no.nav.bidrag.dokument.bestilling.consumer.dto.hentSisteSjablong
import no.nav.bidrag.dokument.bestilling.consumer.dto.hentSjablongForTomDato
import no.nav.bidrag.domene.enums.person.Sivilstandskode
import no.nav.bidrag.transport.dokumentmaler.BeløpFraTil
import no.nav.bidrag.transport.dokumentmaler.ForskuddInntektgrensePeriode
import no.nav.bidrag.transport.dokumentmaler.PeriodeFraTom
import no.nav.bidrag.transport.dokumentmaler.SjablonDetaljer
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
class SjablongService(
    val sjablonConsumer: SjablonConsumer,
) {
    fun hentSjablonDetaljer(): SjablonDetaljer {
        val sjablonger = sjablonConsumer.hentSjablonger()!!
        return SjablonDetaljer(
            multiplikatorInntekstgrenseForskudd = sjablonger.hentSisteSjablong(SjablongType.MULTIPLIKATOR_MAKS_INNTGRENSE_FORSKUDD_MOTTAKER)?.verdi ?: BigDecimal(0),
            fastsettelseGebyr = sjablonger.hentSisteSjablong(SjablongType.BELØP_FASTSETTELSESGEBYR)?.verdi ?: BigDecimal(0),
            forskuddInntektIntervall = sjablonger.hentSisteSjablong(SjablongType.INNTEKTSINTERVALL_FORSKUDD)?.verdi ?: BigDecimal(0),
            forskuddSats = sjablonger.hentSisteSjablong(SjablongType.FORSKUDDSSATS)?.verdi ?: BigDecimal(0),
            multiplikatorMaksBidrag = sjablonger.hentSisteSjablong(SjablongType.MULTIPLIKATOR_MAKS_BIDRAG)?.verdi ?: BigDecimal(0),
            multiplikatorHøyInntektBp = sjablonger.hentSisteSjablong(SjablongType.MULTIPLIKATOR_HØY_INNTEKT_BIDRAGSPLIKTIG)?.verdi ?: BigDecimal(0),
            inntektsintervallTillegsbidrag = sjablonger.hentSisteSjablong(SjablongType.INNTEKTSINTERVALL_TILLEGGSBIDRAG)?.verdi ?: BigDecimal(0),
            multiplikatorInnteksinslagBarn = sjablonger.hentSisteSjablong(SjablongType.MULTIPLIKATOR_INNTEKTSINNSLAG_BIDRAGSBARN)?.verdi ?: BigDecimal(0),
            multiplikatorMaksInntekBarn = sjablonger.hentSisteSjablong(SjablongType.MULTIPLIKATOR_MAKS_INNTEKT_BIDRAGSBARN)?.verdi ?: BigDecimal(0),
            nedreInntekstgrenseGebyr = sjablonger.hentSisteSjablong(SjablongType.NEDRE_INNTEKTSGRENSE_GEBYR)?.verdi ?: BigDecimal(0),
            maksProsentAvInntektBp = sjablonger.hentSisteSjablong(SjablongType.MAKS_PROSENT_AV_INNTEKT_BIDRAGSPLIKTIG)?.verdi ?: BigDecimal(0),
            prosentsatsTilleggsbidrag = sjablonger.hentSisteSjablong(SjablongType.PROSENTSATS_TILLEGGSBIDRAG)?.verdi ?: BigDecimal(0),
            forskuddInntektgrensePerioder = hentSjablonGrunnlagForskuddSjablongForPeriode(PeriodeFraTom(LocalDate.now(), null)),
        )
    }

    fun hentForsuddsatsForPeriode(periodeTomDato: LocalDate?): SjablonData {
        val sjablonger = sjablonConsumer.hentSjablonger()!!
        return sjablonger.hentSjablongForTomDato(SjablongType.FORSKUDDSSATS, periodeTomDato)!!
    }

    fun hentInntektGrenseForPeriode(periodeTomDato: LocalDate?): BigDecimal {
        val sjablonger = sjablonConsumer.hentSjablonger()!!
        val forskuddSats = sjablonger.hentSjablongForTomDato(SjablongType.FORSKUDDSSATS, periodeTomDato)!!
        val multiplikatorInnteksinslagBarn = sjablonger.hentSjablongForTomDato(SjablongType.MULTIPLIKATOR_INNTEKTSINNSLAG_BIDRAGSBARN, periodeTomDato)!!
        return forskuddSats.verdi * multiplikatorInnteksinslagBarn.verdi
    }

    fun hentMaksInntektForPeriode(periodeTomDato: LocalDate?): BigDecimal {
        val sjablonger = sjablonConsumer.hentSjablonger()!!
        val forskuddSats = sjablonger.hentSjablongForTomDato(SjablongType.FORSKUDDSSATS, periodeTomDato)!!
        val multiplikatorMaksInntektMottaker = sjablonger.hentSjablongForTomDato(SjablongType.MULTIPLIKATOR_MAKS_INNTGRENSE_FORSKUDD_MOTTAKER, periodeTomDato)!!
        return forskuddSats.verdi * multiplikatorMaksInntektMottaker.verdi
    }

    fun hentForskuddInntektgrensePerioder(
        fraDato: LocalDate,
        tomDato: LocalDate? = null,
    ): List<ForskuddInntektgrensePeriode> {
        val sjablonger = sjablonConsumer.hentSjablonger() ?: return emptyList()

        val sjablongTyper =
            listOf(
                SjablongType.INNTEKTSINTERVALL_FORSKUDD,
                SjablongType.ØVRE_INNTEKTSGRENSE_FOR_FULLT_FORSKUDD,
                SjablongType.ØVRE_INNTEKTSGRENSE_FOR_75PROSENT_FORSKUDD_ENSLIG,
                SjablongType.ØVRE_INNTEKTSGRENSE_FOR_75PROSENT_FORSKUDD_GIFT_SAMB,
                SjablongType.MULTIPLIKATOR_MAKS_INNTGRENSE_FORSKUDD_MOTTAKER,
                SjablongType.FORSKUDDSSATS,
            )
        return sjablonger.hentPerioderForSjabloner(sjablongTyper, fraDato, tomDato).flatMap { periodeFraTom ->
            hentSjablonGrunnlagForskuddSjablongForPeriode(periodeFraTom)
        }
    }

    fun hentSjablonGrunnlagForskuddSjablongForPeriode(periodeFraTom: PeriodeFraTom): List<ForskuddInntektgrensePeriode> {
        val sjablonger = sjablonConsumer.hentSjablonger()!!
        val periodeFra = periodeFraTom.fraDato
        val periodTomDato = periodeFraTom.tomDato
        val grenseFullForskudd = sjablonger.hentSjablongForTomDato(SjablongType.ØVRE_INNTEKTSGRENSE_FOR_FULLT_FORSKUDD, periodTomDato)!!
        val forskudd75ProsentEnslig = sjablonger.hentSjablongForTomDato(SjablongType.ØVRE_INNTEKTSGRENSE_FOR_75PROSENT_FORSKUDD_ENSLIG, periodTomDato)!!
        val forskudd75ProsentGift = sjablonger.hentSjablongForTomDato(SjablongType.ØVRE_INNTEKTSGRENSE_FOR_75PROSENT_FORSKUDD_GIFT_SAMB, periodTomDato)!!
        val inntekstIntervallForskudd = sjablonger.hentSjablongForTomDato(SjablongType.INNTEKTSINTERVALL_FORSKUDD, periodTomDato)!!
        val multiplikatorForskudd = sjablonger.hentSjablongForTomDato(SjablongType.MULTIPLIKATOR_MAKS_INNTGRENSE_FORSKUDD_MOTTAKER, periodTomDato)!!
        val forskuddSats = sjablonger.hentSjablongForTomDato(SjablongType.FORSKUDDSSATS, periodTomDato)!!

        val forskuddMaksVerdi = forskuddSats.verdi * multiplikatorForskudd.verdi
        val forskuddEnslig1Barn =
            ForskuddInntektgrensePeriode(
                fomDato = periodeFra,
                tomDato = periodTomDato,
                antallBarn = 1,
                forsorgerType = Sivilstandskode.ENSLIG,
                beløp75Prosent = BeløpFraTil(grenseFullForskudd.verdi + BigDecimal.ONE, forskudd75ProsentEnslig.verdi),
                beløp50Prosent = BeløpFraTil(forskudd75ProsentEnslig.verdi + BigDecimal.ONE, forskuddMaksVerdi),
            )
        val forskuddGift1Barn =
            ForskuddInntektgrensePeriode(
                fomDato = periodeFra,
                tomDato = periodTomDato,
                antallBarn = 1,
                forsorgerType = Sivilstandskode.GIFT_SAMBOER,
                beløp75Prosent = BeløpFraTil(grenseFullForskudd.verdi + BigDecimal.ONE, forskudd75ProsentGift.verdi),
                beløp50Prosent = BeløpFraTil(forskudd75ProsentGift.verdi + BigDecimal.ONE, forskuddMaksVerdi),
            )
        return listOf(
            forskuddEnslig1Barn,
            forskuddEnslig1Barn.copy(
                antallBarn = 2,
                beløp75Prosent = BeløpFraTil(grenseFullForskudd.verdi + BigDecimal.ONE, (forskudd75ProsentEnslig.verdi + inntekstIntervallForskudd.verdi).coerceAtMost(forskuddMaksVerdi)),
                beløp50Prosent = BeløpFraTil((forskudd75ProsentEnslig.verdi + inntekstIntervallForskudd.verdi + BigDecimal.ONE).coerceAtMost(forskuddMaksVerdi), forskuddMaksVerdi),
            ),
            forskuddEnslig1Barn.copy(
                antallBarn = 3,
                beløp75Prosent = BeløpFraTil(grenseFullForskudd.verdi + BigDecimal.ONE, forskuddMaksVerdi),
                beløp50Prosent = BeløpFraTil(forskuddMaksVerdi, forskuddMaksVerdi),
            ),
            forskuddEnslig1Barn.copy(
                antallBarn = 4,
                beløp75Prosent = BeløpFraTil(grenseFullForskudd.verdi + BigDecimal.ONE, forskuddMaksVerdi),
                beløp50Prosent = BeløpFraTil(forskuddMaksVerdi, forskuddMaksVerdi),
            ),
            forskuddGift1Barn,
            forskuddGift1Barn.copy(
                antallBarn = 2,
                beløp75Prosent = BeløpFraTil(grenseFullForskudd.verdi + BigDecimal.ONE, forskudd75ProsentGift.verdi + inntekstIntervallForskudd.verdi),
                beløp50Prosent = BeløpFraTil(forskudd75ProsentGift.verdi + inntekstIntervallForskudd.verdi + BigDecimal.ONE, forskuddMaksVerdi),
            ),
            forskuddGift1Barn.copy(
                antallBarn = 3,
                beløp75Prosent = BeløpFraTil(grenseFullForskudd.verdi + BigDecimal.ONE, forskudd75ProsentGift.verdi + inntekstIntervallForskudd.verdi * BigDecimal(2)),
                beløp50Prosent = BeløpFraTil(forskudd75ProsentGift.verdi + inntekstIntervallForskudd.verdi * BigDecimal(2) + BigDecimal.ONE, forskuddMaksVerdi),
            ),
            forskuddGift1Barn.copy(
                antallBarn = 4,
                beløp75Prosent = BeløpFraTil(grenseFullForskudd.verdi + BigDecimal.ONE, forskuddMaksVerdi),
                beløp50Prosent = BeløpFraTil(forskuddMaksVerdi, forskuddMaksVerdi),
            ),
        )
    }
}

fun hentSjablonListe(): SjablongerDto? =
    try {
        AppContext.getBean(SjablonConsumer::class.java).hentSjablonger()
    } catch (e: Exception) {
        secureLogger.debug(e) { "Feil ved henting av sjabloner" }
        null
    }

fun hentInnslagKapitalinntekt(periodeTomDato: LocalDate?): BigDecimal {
    val sjablonger = hentSjablonListe()!!
    return sjablonger.hentSjablongForTomDato(SjablongType.INNSLAG_KAPITALINNTEKT, periodeTomDato)?.verdi ?: BigDecimal(10000)
}
