package no.nav.bidrag.grunnlag.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.grunnlag.consumer.ecb.ECBService
import no.nav.bidrag.grunnlag.consumer.ecb.ECBServiceException
import no.nav.bidrag.grunnlag.consumer.valutakurs.NorgesBankValutakursRestKlient
import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.norgesbank.Frekvens
import no.nav.bidrag.grunnlag.consumer.valutakurs.exception.IngenValutakursException
import no.nav.bidrag.grunnlag.consumer.valutakurs.exception.ValutakursClientException
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakursRequest
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakursResponse
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentetValutakursResultat
import no.nav.bidrag.grunnlag.persistence.entity.ValutakursgrunnlagKilde
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

private val LOGGER = KotlinLogging.logger {}

@Service
class HentValutakursService(
    private val ecbService: ECBService,
    private val norgesBankValutakursRestKlient: NorgesBankValutakursRestKlient,
) {
    fun hentValutakurs(request: HentValutakursRequest): HentValutakursResponse {
        require(request.hentValutakursListe.isNotEmpty()) { "Minst én valuta må oppgis" }
        require(request.hentValutakursListe.all { it.dato.dayOfMonth == 1 && it.dato.monthValue in listOf(1, 7) && !it.dato.isAfter(LocalDate.now()) }) {
            "Kursgrunnlag må starte 1. januar eller 1. juli og kan ikke starte i fremtiden"
        }
        return HentValutakursResponse(request.hentValutakursListe.map { (dato, valutakode) -> hentValutakurs(dato, valutakode) })
    }

    private fun hentValutakurs(dato: LocalDate, utenlandskValutakode: Valutakode): HentetValutakursResultat {
        require(utenlandskValutakode != Valutakode.NOK) { "NOK trenger ikke kursgrunnlag" }
        val observasjonsmåned = YearMonth.from(dato.minusMonths(1))
        val periode = ÅrMånedsperiode(observasjonsmåned, YearMonth.from(dato))
        if (utenlandskValutakode.utgåttDato?.let { !dato.isBefore(it) } == true) {
            return HentetValutakursResultat.FeiledValutakurs(periode, utenlandskValutakode, Valutakode.NOK)
        }
        try {
            val valutakurs = ecbService.hentValutakurs(utenlandskValutakode.name, observasjonsmåned.atEndOfMonth())
            if (valutakurs.kurs.signum() <= 0) throw ECBServiceException("Ugyldig ECB-kurs for $utenlandskValutakode")
            return HentetValutakursResultat.HentetValutakurs(
                periode = periode,
                valutakursSnitt = valutakurs.kurs,
                multiplikator = 0,
                basisvaluta = utenlandskValutakode,
                kvoteringsvaluta = Valutakode.NOK,
                hentetTidspunkt = LocalDateTime.now(),
                kilde = ValutakursgrunnlagKilde.ECB,
            )
        } catch (e: ECBServiceException) {
            LOGGER.warn { "ECB-kurs mangler for $utenlandskValutakode i $observasjonsmåned: ${e.message}. Prøver Norges Bank" }
        }
        return try {
            val valutakurs = norgesBankValutakursRestKlient.hentValutakurs(Frekvens.MÅNEDLIG, utenlandskValutakode.name, observasjonsmåned.atEndOfMonth())
            if (valutakurs.kurs.signum() <= 0 || YearMonth.from(valutakurs.kursDato) != observasjonsmåned || valutakurs.valuta != utenlandskValutakode.name) {
                throw IngenValutakursException("Ugyldig kurs fra Norges Bank", null)
            }
            HentetValutakursResultat.HentetValutakurs(periode, valutakurs.kurs, 0, utenlandskValutakode, Valutakode.NOK, LocalDateTime.now(), ValutakursgrunnlagKilde.NORGES_BANK)
        } catch (e: ValutakursClientException) {
            LOGGER.error { "Ingen lagringsbar kurs fra ECB eller Norges Bank for $utenlandskValutakode i $observasjonsmåned: ${e.message}" }
            HentetValutakursResultat.FeiledValutakurs(periode, utenlandskValutakode, Valutakode.NOK)
        }
    }
}
/*
    private fun hentValutakursNorgesBank(valutakode: Valutakode) {
        val respons = mutableListOf<HentetValutakurs>()
        request.hentValutakursListe.forEach { valuta ->
            val responsNb: SdmxSimplified =
                when (
                    val innhentetValutakurs =
                        norgesBankConsumer.hentValutakurs(valutakode = valuta.valutakode.toString(), dato = valuta.dato)
                ) {
                    is RestResponse.Success -> innhentetValutakurs.body
                    is RestResponse.Failure -> throw Exception("Feil ved henting av valutakurs: ${innhentetValutakurs.message}")
                }

            val valutakursSnitt = responsNb.data.dataSets.first().series.values.first().observations.values.first().first().toBigDecimal()
            val valutakode = responsNb.data.structure.dimensions.series.first { it.id == "BASE_CUR" }.values.first().id
            val multiplikator = responsNb.data.structure.dimensions.series.first { it.id == "UNIT_MULT" }.values.first().id.toInt()

            // Sjekk om hentet valutakode er den samme som angitt i requesten
            if (valutakode != valuta.valutakode.name) {
                throw Exception("Hentet valutakode ($valutakode) er ikke den samme som angitt i requesten (${valuta.valutakode})")
            }

            val periodeFra = valuta.dato.minusMonths(1).toYearMonth()
            val periodeTil = valuta.dato.toYearMonth()

            respons.add(
                HentetValutakurs(
                    periode = ÅrMånedsperiode(periodeFra, periodeTil),
                    valutakursSnitt = valutakursSnitt,
                    multiplikator = multiplikator,
                    basisvaluta = valuta.valutakode,
                    kvoteringsvaluta = Valutakode.NOK,
                    hentetTidspunkt = LocalDateTime.now(),
                )
            )
        }
        return HentValutakursResponse(respons)
    }

    private fun hentValutakursEcb() {
        val nokE03Kurs = hentNokE03Kurs()
//            europeiskeSentralbankenConsumer.hentValutakurs(valutakode = Valutakode.NOK.toString(), dato = request.hentValutakursListe.first().dato)
        val responseNokE03: SdmxData =
            when (nokE03Kurs) {
                is RestResponse.Success -> nokE03Kurs.body
                is RestResponse.Failure -> throw Exception("Feil ved henting av valutakurs: ${nokE03Kurs.message}")
            }
        val nokValutakursSnitt =
            responseNokE03.dataSets.first().series.values.first()?.observations?.values?.first()?.first()?.toBigDecimal() ?: BigDecimal.ZERO

        val respons = mutableListOf<HentetValutakurs>()
        request.hentValutakursListe.forEach { valuta ->
            val responsNb: SdmxData =
                when (
                    val innhentetValutakurs =
                        europeiskeSentralbankenConsumer.hentValutakurs(valutakode = valuta.valutakode.toString(), dato = valuta.dato)
                ) {
                    is RestResponse.Success -> innhentetValutakurs.body
                    is RestResponse.Failure -> throw Exception("Feil ved henting av valutakurs: ${innhentetValutakurs.message}")
                }

            val valutakursSnitt =
                responseNokE03.dataSets.first().series.values.first()?.observations?.values?.first()?.first()?.toBigDecimal() ?: BigDecimal.ZERO
//            val valutakode = responsNb.structure.dimensions.series.first { it.id == "BASE_CUR" }.values.first().id
            val multiplikator = responsNb.structure.dimensions.series.firstOrNull() { it.id == "UNIT_MULT" }?.values?.first()?.id?.toInt() ?: 0

            // Sjekk om hentet valutakode er den samme som angitt i requesten
//            if (valutakode != valuta.valutakode.name) {
//                throw Exception("Hentet valutakode ($valutakode) er ikke den samme som angitt i requesten (${valuta.valutakode})")
//            }

            val periodeFra = valuta.dato.minusMonths(1).toYearMonth()
            val periodeTil = valuta.dato.toYearMonth()

            // Vi regner om fra NOK til valuta via E03 (Broad EER group of trading partners)
            val omregnetKurs = nokValutakursSnitt / valutakursSnitt // TODO multiplikator mellom de???

            respons.add(
                HentetValutakurs(
                    periode = ÅrMånedsperiode(periodeFra, periodeTil),
                    valutakursSnitt = omregnetKurs,
                    multiplikator = multiplikator,
                    basisvaluta = valuta.valutakode,
                    kvoteringsvaluta = Valutakode.NOK,
                    hentetTidspunkt = LocalDateTime.now(),
                )
            )
        }
        return HentValutakursResponse(respons)
}
 */
