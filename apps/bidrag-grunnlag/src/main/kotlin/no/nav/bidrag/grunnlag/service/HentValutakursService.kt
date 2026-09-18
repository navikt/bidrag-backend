package no.nav.bidrag.grunnlag.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.grunnlag.consumer.ecb.ECBService
import no.nav.bidrag.grunnlag.consumer.ecb.ECBServiceException
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakursRequest
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakursResponse
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentetValutakursResultat
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

private val LOGGER = KotlinLogging.logger {}

@Service
class HentValutakursService(
    private val ecbService: ECBService,
//    private val norgesBankValutakursRestKlient: NorgesBankValutakursRestKlient,
) {
    fun hentValutakurs(request: HentValutakursRequest): HentValutakursResponse {
        val dato = request.hentValutakursListe.first().dato // TODO

        return request.hentValutakursListe
            .filter { (dato, valutakode) -> valutakode.aktiv(dato) }
            .map { (_, valutakode) ->
                hentValutakurs(dato, valutakode)
            }.let {
                HentValutakursResponse(it)
            }
    }

    private fun hentValutakurs(dato: LocalDate, utenlandskValutakode: Valutakode): HentetValutakursResultat {
        try {
            val valutakurs = ecbService.hentValutakurs(utenlandskValutakode.name, dato)
            LOGGER.info { "Hentet valutakurs for $utenlandskValutakode." } // TODO fjernes eller debug
            return HentetValutakursResultat.HentetValutakurs(
                periode = ÅrMånedsperiode(dato, dato), // TODO
                valutakursSnitt = valutakurs.kurs,
                multiplikator = 10, // TODO
                basisvaluta = utenlandskValutakode,
                kvoteringsvaluta = Valutakode.NOK,
                hentetTidspunkt = LocalDateTime.now()
            )
        } catch (e: ECBServiceException) {
            LOGGER.error(e) { "Feil ved henting av valutakurs for $utenlandskValutakode." }
            return HentetValutakursResultat.FeiledValutakurs(
                periode = ÅrMånedsperiode(dato, dato), // TODO
                basisvaluta = utenlandskValutakode,
                kvoteringsvaluta = Valutakode.NOK,
            )
//            val valutakurs = norgesBankValutakursRestKlient.hentValutakurs(Frekvens.MÅNEDLIG, utenlandskValutakode.name, dato)
//            return HentetValutakurs(
//                periode = ÅrMånedsperiode(dato, dato), // TODO
//                valutakursSnitt = valutakurs.kurs,
//                multiplikator = 10, // TODO
//                basisvaluta = utenlandskValutakode,
//                kvoteringsvaluta = Valutakode.NOK,
//                hentetTidspunkt = LocalDateTime.now()
//            )
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
