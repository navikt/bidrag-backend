package no.nav.bidrag.grunnlag.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.tid.Datoperiode
import no.nav.bidrag.domene.tid.Periode
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakurs
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakursRequest
import org.springframework.stereotype.Service
import java.time.LocalDate

private val LOGGER = KotlinLogging.logger {}

@Service
class HentHistoriskeValutakurserService(
    private val valutakursgrunnlagService: ValutakursgrunnlagService,
    private val hentValutakursService: HentValutakursService,
) {
    private val hentAntallÅr = 1

    // Periodene 1. januar til 1. juli og 1. juli til 1. januar `hentAntallÅr` år bakover i tid
    private val historiskePerioder: List<Periode<LocalDate>> = (0..<hentAntallÅr).flatMap {
        val nå = LocalDate.now()
        val år = nå.minusYears(it.toLong())
        val førsteJuli = LocalDate.of(år.year, 7, 1)

        listOf(
            Datoperiode(LocalDate.of(år.year, 1, 1), førsteJuli),
            Datoperiode(førsteJuli, førsteJuli.plusMonths(6)),
        )
    }

    // TODO Api for å trigge manuelt
    // TODO Api for å hente en spesifikk valuta for en periode

    fun hentHistoriskeValutakurser() {
        val utenlandskeValutakoder = Valutakode.entries.toTypedArray().filter { it != Valutakode.NOK }
        try {
            val perioderUtenGrunnlag = historiskePerioder.map { periode ->
                val valutakoderUtenGrunnlag = utenlandskeValutakoder.filter { valutakode ->
                    valutakursgrunnlagService.hentValutakursgrunnlag(valutakode, periode.fom) == null
                }
                periode to valutakoderUtenGrunnlag
            }.filter { (_, valutakoder) -> valutakoder.isNotEmpty() }

            LOGGER.info { "Henter historiske valutakurser for ${perioderUtenGrunnlag.size} perioder" }
            perioderUtenGrunnlag.forEach { (periode, valutakoder) ->
                LOGGER.info { "Henter historiske valutakurser for periode $periode" }
                val hentValutakursListe =
                    valutakoder.map { valutakode ->
                        HentValutakurs(
                            dato = periode.fom,
                            valutakode = valutakode,
                        )
                    }

                val hentValutakursRequest = HentValutakursRequest(hentValutakursListe)
                val hentetValutakursResponse = hentValutakursService.hentValutakurs(hentValutakursRequest)
                valutakursgrunnlagService.opprettValutakursgrunnlag(hentetValutakursResponse.hentetValutakursListe, periode)
            }
        } catch (e: Exception) {
            // TODO
            throw e
        }
    }
}
