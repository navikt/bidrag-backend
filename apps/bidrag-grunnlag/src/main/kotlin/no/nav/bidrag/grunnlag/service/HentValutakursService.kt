package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.grunnlag.consumer.valutakurser.NorgesBankConsumer
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxSimplified
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakursRequest
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakursResponse
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentetValutakurs
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.transport.felles.toYearMonth
import org.springframework.stereotype.Service

@Service
class HentValutakursService(private val norgesBankConsumer: NorgesBankConsumer) {

    fun hentValutakurs(request: HentValutakursRequest): HentValutakursResponse {
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

            // Sjekk om hentet valutakode er den samme som angitt i requesten
            if (valutakode != valuta.valutakode.name) {
                throw Exception("Hentet valutakode ($valutakode) er ikke den samme som angitt i requesten (${valuta.valutakode})")
            }

            val periodeFra = valuta.dato.minusMonths(1).toYearMonth()
            val periodeTil = valuta.dato.toYearMonth()

            respons.add(HentetValutakurs(ÅrMånedsperiode(periodeFra, periodeTil), valutakursSnitt))
        }
        return HentValutakursResponse(respons)
    }
}
