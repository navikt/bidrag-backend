package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.tid.Periode
import no.nav.bidrag.grunnlag.bo.ValutakursgrunnlagBo
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentetValutakurs
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.math.pow

@Service
class ValutakursgrunnlagService(
    private val persistenceService: PersistenceService
) {
    fun opprettValutakursgrunnlag(hentetValutakurser: List<HentetValutakurs>, gyldighetsperiode: Periode<LocalDate>) {
        if (gyldighetsperiode.til == null) {
            return
        }
        val now = LocalDate.now()
        val aktiv = gyldighetsperiode.inneholder(now)

        hentetValutakurser.map {
            ValutakursgrunnlagBo(
                aktiv = aktiv,
                brukFra = gyldighetsperiode.fom.atStartOfDay(),
                brukTil = gyldighetsperiode.til!!.atStartOfDay(), // TODO
                hentetTidspunkt = it.hentetTidspunkt,
                kurs = it.valutakursSnitt,
                multiplikator = it.multiplikator,
                basisvaluta = it.basisvaluta,
                kvoteringsvaluta = it.kvoteringsvaluta
            )
        }.forEach(persistenceService::opprettValutakursgrunnlag)
    }

    fun hentValutakursgrunnlag(valutakode: Valutakode, dato: LocalDate): ValutakursgrunnlagBo {
        return persistenceService.hentValutakursgrunnlag(valutakode, dato).let {
            ValutakursgrunnlagBo(
                valutakursgrunnlagId = it.valutakursgrunnlagId,
                aktiv = it.aktiv,
                brukFra = it.brukFra,
                brukTil = it.brukTil,
                hentetTidspunkt = it.hentetTidspunkt,
                kurs = it.kurs,
                multiplikator = it.multiplikator,
                basisvaluta = it.basisvaluta,
                kvoteringsvaluta = it.kvoteringsvaluta
            )
        }
    }

    fun fraNok(beløpNok: BigDecimal, valutakode: Valutakode, dato: LocalDate = LocalDate.now()): BigDecimal {
        val valutakursgrunnlag = hentValutakursgrunnlag(valutakode, dato) // TODO error handling
        val beløp = ((beløpNok / valutakursgrunnlag.kurs) * BigDecimal(10.0.pow(valutakursgrunnlag.multiplikator.toDouble())))
        return beløp
    }

    fun tilNok(beløp: BigDecimal, valutakode: Valutakode, dato: LocalDate = LocalDate.now()): BigDecimal {
        // TODO ensure kurs > 0
        val valutakursgrunnlag=hentValutakursgrunnlag(valutakode, dato)
        val beløpNok = (beløp * BigDecimal(10.0.pow(valutakursgrunnlag.multiplikator.toDouble()))) / valutakursgrunnlag.kurs
        return beløpNok
    }
}