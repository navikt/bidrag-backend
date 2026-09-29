package no.nav.bidrag.grunnlag.consumer.valutakurser.api

import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.Valutakurs
import no.nav.bidrag.grunnlag.consumer.valutakurs.exception.ValutakursTransformationException
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException

fun SdmxData.toExchangeRates(): List<Valutakurs> {
    val dimensjoner = structure.dimensions
    val valutaposisjon = dimensjoner.series.indexOfFirst { it.id == "CURRENCY" }
    val frekvensposisjon = dimensjoner.series.indexOfFirst { it.id == "FREQ" }
    val perioder = dimensjoner.observation.singleOrNull { it.id == "TIME_PERIOD" }?.values
        ?: throw ValutakursTransformationException("ECB-svaret mangler tidsperioder", null)
    if (valutaposisjon < 0 || frekvensposisjon < 0) {
        throw ValutakursTransformationException("ECB-svaret mangler valuta eller frekvens", null)
    }

    return try {
        dataSets.flatMap { dataSet ->
            dataSet.series.flatMap { (nøkkel, serie) ->
                val indekser = nøkkel.split(":")
                fun dimensjonsverdi(posisjon: Int): String = indekser.getOrNull(posisjon)?.toIntOrNull()
                    ?.let { dimensjoner.series[posisjon].values.getOrNull(it)?.id }
                    ?: throw ValutakursTransformationException("ECB-svaret har ugyldig serienøkkel", null)

                val valuta = dimensjonsverdi(valutaposisjon)
                val frekvens = dimensjonsverdi(frekvensposisjon)
                serie?.observations.orEmpty().map { (periodeindeks, observasjon) ->
                    val periode = periodeindeks.toIntOrNull()?.let { perioder.getOrNull(it)?.id }
                        ?: throw ValutakursTransformationException("ECB-svaret har ugyldig observasjonsperiode", null)
                    val kurs = observasjon?.firstOrNull()?.toBigDecimalOrNull()
                        ?: throw ValutakursTransformationException("ECB-svaret mangler kurs", null)
                    val dato = when (frekvens) {
                        "M" -> YearMonth.parse(periode).atEndOfMonth()
                        "D" -> LocalDate.parse(periode)
                        else -> throw ValutakursTransformationException("ECB-svaret har ukjent frekvens", null)
                    }
                    Valutakurs(valuta, kurs, dato)
                }
            }
        }
    } catch (e: DateTimeParseException) {
        throw ValutakursTransformationException("ECB-svaret har ugyldig dato", e)
    }
}
