package no.nav.bidrag.grunnlag.consumer.valutakurser.api

import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.Valutakurs
import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.norgesbank.Frekvens
import no.nav.bidrag.grunnlag.consumer.valutakurs.exception.NorgesBankValutakursMappingException
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException

fun SdmxSimplified.tilValutakurs(valuta: String, frekvens: Frekvens, kursDato: LocalDate): Valutakurs {
    val struktur = data.structure
    val serie = data.dataSets.singleOrNull()?.series?.entries?.singleOrNull()
        ?: throw NorgesBankValutakursMappingException.UventetAntall("Forventet én valutakursserie.")
    val indekser = serie.key.split(":")
    fun dimensjonsverdi(id: String): String {
        val posisjon = struktur.dimensions.series.indexOfFirst { it.id == id }
        return struktur.dimensions.series.getOrNull(posisjon)?.values
            ?.getOrNull(indekser.getOrNull(posisjon)?.toIntOrNull() ?: -1)?.id
            ?: throw NorgesBankValutakursMappingException.ManglerFelt("Mangler gyldig $id.")
    }
    fun attributtverdi(id: String): String {
        val posisjon = struktur.attributes.series.indexOfFirst { it.id == id }
        return struktur.attributes.series.getOrNull(posisjon)?.values
            ?.getOrNull(serie.value?.attributes?.getOrNull(posisjon) ?: -1)?.id
            ?: throw NorgesBankValutakursMappingException.ManglerFelt("Mangler gyldig $id.")
    }

    if (dimensjonsverdi("BASE_CUR") != valuta || dimensjonsverdi("QUOTE_CUR") != "NOK" || dimensjonsverdi("FREQ") != frekvens.verdi) {
        throw NorgesBankValutakursMappingException.UgyldigData("Valuta eller frekvens samsvarer ikke med forespørselen.")
    }
    if (attributtverdi("CALCULATED") != "false" || attributtverdi("COLLECTION") != (if (frekvens == Frekvens.MÅNEDLIG) "A" else "C")) {
        throw NorgesBankValutakursMappingException.UgyldigData("Forventet observert valutakurs med riktig innsamlingstidspunkt.")
    }
    val multiplikator = attributtverdi("UNIT_MULT").toIntOrNull()
    if (multiplikator == null || multiplikator !in -12..12) {
        throw NorgesBankValutakursMappingException.UgyldigData("Ugyldig UNIT_MULT.")
    }
    val observasjon = serie.value?.observations?.entries?.singleOrNull()
        ?: throw NorgesBankValutakursMappingException.UventetAntall("Forventet én kursobservasjon.")
    val periode = struktur.dimensions.observation.singleOrNull { it.id == "TIME_PERIOD" }?.values
        ?.getOrNull(observasjon.key.toIntOrNull() ?: -1)?.id
        ?: throw NorgesBankValutakursMappingException.ManglerFelt("Mangler gyldig observasjonsperiode.")
    val observasjonsdato = try {
        if (frekvens == Frekvens.MÅNEDLIG) YearMonth.parse(periode).atEndOfMonth() else LocalDate.parse(periode)
    } catch (e: DateTimeParseException) {
        throw NorgesBankValutakursMappingException.UgyldigData("Ugyldig observasjonsperiode.", e)
    }
    if (if (frekvens == Frekvens.MÅNEDLIG) YearMonth.from(observasjonsdato) != YearMonth.from(kursDato) else observasjonsdato != kursDato) {
        throw NorgesBankValutakursMappingException.UgyldigData("Observasjonsperioden samsvarer ikke med forespørselen.")
    }
    val kurs = observasjon.value?.singleOrNull()?.toBigDecimalOrNull()
        ?: throw NorgesBankValutakursMappingException.UgyldigData("Mangler gyldig valutakurs.")
    if (kurs.signum() <= 0) throw NorgesBankValutakursMappingException.UgyldigData("Ugyldig valutakurs.")
    val normalisert = kurs.scaleByPowerOfTen(-multiplikator).stripTrailingZeros()
    return Valutakurs(valuta, if (normalisert.scale() < 0) normalisert.setScale(0) else normalisert, observasjonsdato)
}
