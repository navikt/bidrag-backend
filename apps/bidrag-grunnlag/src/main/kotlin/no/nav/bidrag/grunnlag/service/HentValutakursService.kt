package no.nav.bidrag.grunnlag.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.grunnlag.consumer.ecb.ECBService
import no.nav.bidrag.grunnlag.consumer.ecb.ECBServiceException
import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.Valutakurs
import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.norgesbank.Frekvens
import no.nav.bidrag.grunnlag.consumer.valutakurs.exception.IngenValutakursException
import no.nav.bidrag.grunnlag.consumer.valutakurs.exception.NorgesBankValutakursMappingException
import no.nav.bidrag.grunnlag.consumer.valutakurs.exception.ValutakursClientException
import no.nav.bidrag.grunnlag.consumer.valutakurs.exception.ValutakursException
import no.nav.bidrag.grunnlag.consumer.valutakurser.NorgesBankConsumer
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.tilValutakurs
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakursRequest
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakursResponse
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentetValutakursResultat
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.persistence.entity.ValutakursgrunnlagKilde
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

private val LOGGER = KotlinLogging.logger {}

@Service
class HentValutakursService(
    private val ecbService: ECBService,
    private val norgesBankConsumer: NorgesBankConsumer,
) {
    fun hentValutakurs(request: HentValutakursRequest): HentValutakursResponse {
        require(request.hentValutakursListe.isNotEmpty()) { "Minst én valuta må oppgis" }
        require(request.hentValutakursListe.all { it.dato.dayOfMonth == 1 && it.dato.monthValue in listOf(1, 7) && !it.dato.isAfter(LocalDate.now()) }) {
            "Kursgrunnlag må starte 1. januar eller 1. juli og kan ikke starte i fremtiden"
        }
        require(request.hentValutakursListe.none { it.valutakode == Valutakode.NOK }) { "NOK trenger ikke kursgrunnlag" }
        val ecbKurser = request.hentValutakursListe.filter { it.valutakode.aktiv(it.dato) }
            .groupBy { YearMonth.from(it.dato).minusMonths(1) }
            .mapValues { (måned, forespørsler) ->
                try {
                    ecbService.hentValutakurser(forespørsler.map { it.valutakode.name }.distinct(), måned.atEndOfMonth())
                } catch (e: ECBServiceException) {
                    LOGGER.warn { "ECB-innhenting feilet for $måned: ${e.message}. Prøver Norges Bank" }
                    emptyMap()
                }
            }
        return HentValutakursResponse(
            request.hentValutakursListe.map { (dato, valutakode) ->
                hentValutakurs(dato, valutakode, ecbKurser[YearMonth.from(dato).minusMonths(1)]?.get(valutakode.name))
            },
        )
    }

    private fun hentValutakurs(dato: LocalDate, utenlandskValutakode: Valutakode, ecbKurs: Valutakurs?): HentetValutakursResultat {
        require(utenlandskValutakode != Valutakode.NOK) { "NOK trenger ikke kursgrunnlag" }
        val observasjonsmåned = YearMonth.from(dato.minusMonths(1))
        val periode = ÅrMånedsperiode(observasjonsmåned, YearMonth.from(dato))
        if (!utenlandskValutakode.aktiv(dato)) {
            return HentetValutakursResultat.FeiledValutakurs(periode, utenlandskValutakode, Valutakode.NOK)
        }
        if (ecbKurs != null && ecbKurs.kurs.signum() > 0 && ecbKurs.valuta == utenlandskValutakode.name && YearMonth.from(ecbKurs.kursDato) == observasjonsmåned) {
            return HentetValutakursResultat.HentetValutakurs(
                periode = periode,
                valutakursSnitt = ecbKurs.kurs,
                multiplikator = 0,
                basisvaluta = utenlandskValutakode,
                kvoteringsvaluta = Valutakode.NOK,
                hentetTidspunkt = LocalDateTime.now(),
                kilde = ValutakursgrunnlagKilde.ECB,
            )
        }
        LOGGER.warn { "ECB-kurs mangler for $utenlandskValutakode i $observasjonsmåned. Prøver Norges Bank" }
        return try {
            val respons = norgesBankConsumer.hentValutakurs(Frekvens.MÅNEDLIG, utenlandskValutakode.name, observasjonsmåned.atEndOfMonth())
            val valutakurs = when (respons) {
                is RestResponse.Success -> respons.body.tilValutakurs(utenlandskValutakode.name, Frekvens.MÅNEDLIG, observasjonsmåned.atEndOfMonth())
                is RestResponse.Failure -> throw ValutakursException("Norges Bank svarte med status ${respons.statusCode.value()}: ${respons.message}", respons.restClientException)
            }
            if (valutakurs.kurs.signum() <= 0 || YearMonth.from(valutakurs.kursDato) != observasjonsmåned || valutakurs.valuta != utenlandskValutakode.name) {
                throw IngenValutakursException("Ugyldig kurs fra Norges Bank", null)
            }
            HentetValutakursResultat.HentetValutakurs(periode, valutakurs.kurs, 0, utenlandskValutakode, Valutakode.NOK, LocalDateTime.now(), ValutakursgrunnlagKilde.NORGES_BANK)
        } catch (e: ValutakursClientException) {
            LOGGER.error { "Ingen lagringsbar kurs fra ECB eller Norges Bank for $utenlandskValutakode i $observasjonsmåned: ${e.message}" }
            HentetValutakursResultat.FeiledValutakurs(periode, utenlandskValutakode, Valutakode.NOK)
        } catch (e: NorgesBankValutakursMappingException) {
            LOGGER.error { "Ugyldig svar fra Norges Bank for $utenlandskValutakode i $observasjonsmåned: ${e.message}" }
            HentetValutakursResultat.FeiledValutakurs(periode, utenlandskValutakode, Valutakode.NOK)
        }
    }
}
