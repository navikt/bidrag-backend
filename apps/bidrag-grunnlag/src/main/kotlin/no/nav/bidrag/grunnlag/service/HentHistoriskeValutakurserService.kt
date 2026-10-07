package no.nav.bidrag.grunnlag.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.tid.Datoperiode
import no.nav.bidrag.domene.tid.Periode
import no.nav.bidrag.grunnlag.bo.ValutakursgrunnlagBo
import no.nav.bidrag.grunnlag.comparator.isAfterOrEqual
import no.nav.bidrag.grunnlag.comparator.isBeforeOrEqual
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakurs
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakursRequest
import no.nav.bidrag.grunnlag.persistence.entity.toValutakursgrunnlagBo
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit

private val LOGGER = KotlinLogging.logger {}

@Service
class HentHistoriskeValutakurserService(
    private val valutakursgrunnlagService: ValutakursgrunnlagService,
    private val hentValutakursService: HentValutakursService,
) {
    fun hentHistoriskeValutakurser(fra: LocalDate, til: LocalDate): List<ValutakursgrunnlagBo> {
        if (fra.dayOfMonth != 1 || fra.month !in listOf(Month.JANUARY, Month.JULY)) {
            throw UgyldigDatoException("Kan ikke hente valutakurser med fra-dato $fra. Dato må være 1. januar eller 1. juli.")
        }
        if (til.dayOfMonth != 1 || til.month !in listOf(Month.JANUARY, Month.JULY)) {
            throw UgyldigDatoException("Kan ikke hente valutakurser med til-dato $til. Dato må være 1. januar eller 1. juli.")
        }
        if (fra.isAfterOrEqual(til)) {
            throw UgyldigDatoException("Fra-dato må være før til-dato")
        }
        val nå = LocalDate.now()
        if (til.minusMonths(6).isAfter(nå)) {
            throw UgyldigDatoException("Dato kan ikke være i fremtiden")
        }
        validerAntallPerioder(ChronoUnit.MONTHS.between(fra, til) / 6)

        // Bygger perioder 1. januar til 1. juli og 1. juli til 1. januar for årene i fra og til.
        // Beholder bare halvår som starter på eller etter fra-dato og slutter på eller før til-dato.
        val perioder: List<Periode<LocalDate>> = (fra.year..til.year).flatMap { år ->
            val førsteJuli = LocalDate.of(år, 7, 1)

            listOf(
                Datoperiode(LocalDate.of(år, 1, 1), førsteJuli),
                Datoperiode(førsteJuli, førsteJuli.plusMonths(6)),
            ).filter { datoperiode ->
                datoperiode.fom.isAfterOrEqual(fra) &&
                    (datoperiode.til != null && datoperiode.til!!.isBeforeOrEqual(til))
            }
        }

        return hentHistoriskeValutakurser(perioder)
    }

    fun hentHistoriskeValutakurser(perioder: List<Periode<LocalDate>>): List<ValutakursgrunnlagBo> {
        validerAntallPerioder(perioder.size.toLong())
        val utenlandskeValutakoder = Valutakode.entries.toTypedArray().filter { it != Valutakode.NOK }
        val valutakursgrunnlag = mutableListOf<ValutakursgrunnlagBo>()
        try {
            val perioderUtenGrunnlag = perioder.map { periode ->
                val valutakoderUtenGrunnlag = utenlandskeValutakoder.filter { valutakode ->
                    valutakode.aktiv(periode.fom)
                }.filter { valutakode ->
                    val eksisterende = valutakursgrunnlagService.hentValutakursgrunnlag(valutakode, periode.fom)
                    eksisterende == null || eksisterende.feiletHenting
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
                    .map { it.toValutakursgrunnlagBo() }
                    .forEach { bo -> valutakursgrunnlag.add(bo) }
            }
        } catch (e: Exception) {
            // TODO
            throw e
        }

        return buildList { addAll(valutakursgrunnlag) }
    }

    private fun validerAntallPerioder(antallPerioder: Long) {
        if (antallPerioder > 10) {
            throw UgyldigDatoException("Kan hente maksimalt 10 halvårsperioder av gangen")
        }
    }
}

private class UgyldigDatoException(override val message: String) : HttpStatusCodeException(HttpStatus.BAD_REQUEST, message)
