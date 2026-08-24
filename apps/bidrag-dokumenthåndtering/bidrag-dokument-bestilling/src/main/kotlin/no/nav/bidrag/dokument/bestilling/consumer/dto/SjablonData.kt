package no.nav.bidrag.dokument.bestilling.consumer.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.bidrag.transport.dokumentmaler.PeriodeFraTom
import java.math.BigDecimal
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class SjablonData(
    val typeSjablon: String,
    val datoFom: LocalDate,
    val datoTom: LocalDate,
    val verdi: BigDecimal,
)

typealias SjablongerDto = List<SjablonData>

fun SjablongerDto.hentSisteSjablong(type: SjablongType): SjablonData? = find { it.typeSjablon == type.kode && it.datoTom.isAfter(LocalDate.now()) }

fun SjablongerDto.hentSjablongForTomDato(
    type: SjablongType,
    tomDato: LocalDate?,
): SjablonData? {
    if (tomDato == null) return hentSisteSjablong(type)
    return sortedBy { it.datoTom }.find { it.typeSjablon == type.kode && (it.datoTom.isAfter(tomDato) || it.datoTom == tomDato) }
}

fun SjablongerDto.hentNestePeriodeForSjabloner(
    typer: List<SjablongType>,
    fraDato: LocalDate,
): PeriodeFraTom? {
    val typerKode = typer.map { it.kode }
    return sortedBy { it.datoTom }.find { typerKode.contains(it.typeSjablon) && it.datoTom >= fraDato }?.let {
        val fraDatoPeriode = if (it.datoFom < fraDato) fraDato else it.datoFom
        PeriodeFraTom(fraDatoPeriode, it.datoTom)
    }
}

fun SjablongerDto.hentPerioderForSjabloner(
    typer: List<SjablongType>,
    fraDato: LocalDate,
    tomDato: LocalDate?,
): List<PeriodeFraTom> {
    val perioder = mutableListOf<PeriodeFraTom>()
    val periodeTomDato = tomDato ?: LocalDate.parse("9999-12-31")
    var periode = hentNestePeriodeForSjabloner(typer, fraDato) ?: return perioder
    perioder.add(periode)
    while (periodeTomDato > periode.tomDato) {
        periode = hentNestePeriodeForSjabloner(typer, periode.tomDato!!.plusDays(1)) ?: return perioder
        perioder.add(periode)
    }
    return perioder
}

enum class SjablongType(
    val kode: String,
) {
    BELØP_ORDINÆR_BARNETRYGD("0001"),
    BELØP_ORDINÆRT_SMÅBARNSTILLEGG("0002"),
    BOUTGIFTER_BIDRAGSBARN("0003"),
    FORDEL_SKATTEKLASSE_2("0004"),
    FORSKUDDSSATS("0005"),
    INNSLAG_KAPITALINNTEKT("0006"),
    INNTEKTSINTERVALL_TILLEGGSBIDRAG("0007"),
    MAKS_PROSENT_AV_INNTEKT_BIDRAGSPLIKTIG("0008"),
    MULTIPLIKATOR_HØY_INNTEKT_BIDRAGSPLIKTIG("0009"),
    MULTIPLIKATOR_INNTEKTSINNSLAG_BIDRAGSBARN("0010"),
    MULTIPLIKATOR_MAKS_BIDRAG("0011"),
    MULTIPLIKATOR_MAKS_INNTEKT_BIDRAGSBARN("0012"),
    MULTIPLIKATOR_MAKS_INNTGRENSE_FORSKUDD_MOTTAKER("0013"),
    NEDRE_INNTEKTSGRENSE_GEBYR("0014"),
    PROSENTSATS_SKATT_ALMINNELIG_INNTEKT("0015"),
    PROSENTSATS_TILLEGGSBIDRAG("0016"),
    PROSENTSATS_TRYGDEAVGIFT("0017"),
    SKATTEPROSENT_BARNETILLEGG("0018"),
    UNDERHOLD_BARN_EGEN_HUS_BIDRAGSPLIKTIG("0019"),
    PROSENTGRENSE_FOR_ENDRING_AV_BIDRAG_10PROSENT_REGEL("0020"),
    BARNETILLEGG_FRA_FORSVARET_FØRSTE_BARN("0021"),
    BARNETILLEGG_FRA_FORSVARET_ØVRIGE_BARN("0022"),
    MINSTEFRADRAG("0023"),
    GJENNOMSNITT_ANTALL_VIRKEDAGER_PR_MÅNED("0024"),
    MINSTEFRADRAG_PROSENT_INNTEKT("0025"),
    DAGLIG_SATS_FOR_BARNETILLEGG("0026"),
    PERSONFRADRAG_KLASSE_1("0027"),
    PERSONFRADRAG_KLASSE_2("0028"),
    KONTANTSTSTØTTE("0029"),
    ØVRE_INNTEKTSGRENSE_FOR_IKKE_I_SKATTEPOSISJON("0030"),
    NEDRE_INNTEKTSGRENSE_FOR_FULL_SKATTEPOSISJON("0031"),
    EKSTRA_SMÅBARNSTILLEGG("0032"),
    ØVRE_INNTEKTSGRENSE_FOR_FULLT_FORSKUDD("0033"),
    ØVRE_INNTEKTSGRENSE_FOR_75PROSENT_FORSKUDD_ENSLIG("0034"),
    ØVRE_INNTEKTSGRENSE_FOR_75PROSENT_FORSKUDD_GIFT_SAMB("0035"),
    INNTEKTSINTERVALL_FORSKUDD("0036"),
    ØVRE_GRENSE_FOR_SÆRTILSKUDD("0037"),
    FORSKUDDSSATS_75PROSENT("0038"),
    FORDEL__SÆRFRADRAG("0039"),
    PROSENTSATS_ALMINNELIG_INNTEKT("0040"),
    BELØP_FASTSETTELSESGEBYR("0100"),
    BELØP_FORHØYET_BARNETRYGD("0041"),
    UTVIDET_BARNETRYGD_TIL_BIDRAGSKALKULATOR("0042"),
}
