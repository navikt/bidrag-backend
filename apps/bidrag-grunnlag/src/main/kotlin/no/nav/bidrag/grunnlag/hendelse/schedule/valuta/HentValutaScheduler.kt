package no.nav.bidrag.grunnlag.hendelse.schedule.valuta

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.tid.Datoperiode
import no.nav.bidrag.domene.tid.Periode
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakurs
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakursRequest
import no.nav.bidrag.grunnlag.service.HentValutakursService
import no.nav.bidrag.grunnlag.service.ValutakursgrunnlagService
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.LocalDate

private val LOGGER = KotlinLogging.logger { }

@Configuration
@EnableScheduling
//@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
class HentValutaScheduler(
    private val hentValutakursService: HentValutakursService,
    private val valutakursgrunnlagService: ValutakursgrunnlagService,
) {
//            @Scheduled
    fun hentValutakurs() {
        LOGGER.info { "Henter valutakurs" }
        val valutakoder = Valutakode.entries.toTypedArray()
        val hentValutakursRequest = HentValutakursRequest(
            hentValutakursListe = valutakoder.map { valutakode ->
                HentValutakurs(
                    dato = LocalDate.now(), valutakode = valutakode
                )
            })

        val gyldighetsperiode = lagGyldighetsperiode()
        try {
            val hentvalutakursResponse = hentValutakursService.hentValutakurs(hentValutakursRequest)
            valutakursgrunnlagService.opprettValutakursgrunnlag(hentvalutakursResponse.hentetValutakursListe, gyldighetsperiode)

            // TODO sett forrige grunnlag som inaktiv
        } catch (e: NoSuchElementException) {
            LOGGER.error(e) { "Feil ved henting av valutakurs" }
        } catch (e: Exception) { // TODO mer spesifikk exception i service
            LOGGER.error(e) { "Feil ved henting av valutakurs" }
            // TODO varsle på slack
        }
    }

    // 1. januar til 1. juli eller 1. juli til 1. januar avhengig av når metoden kalles
    fun lagGyldighetsperiode(): Periode<LocalDate> {
        val nå = LocalDate.now()
        val fom = if (nå.monthValue < 7) {
            LocalDate.of(nå.year, 1, 1)
        } else {
            LocalDate.of(nå.year, 7, 1)
        }

        return Datoperiode(fom, fom.plusMonths(6))
    }
}