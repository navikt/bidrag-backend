package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.tid.Periode
import no.nav.bidrag.grunnlag.bo.ValutakursgrunnlagBo
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentetValutakursResultat
import no.nav.bidrag.grunnlag.persistence.entity.Valutakursgrunnlag
import no.nav.bidrag.grunnlag.persistence.entity.toValutakursgrunnlagBo
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.math.pow

@Service
class ValutakursgrunnlagService(
    private val persistenceService: PersistenceService
) {
    fun opprettValutakursgrunnlag(
        hentetValutakurser: List<HentetValutakursResultat>,
        gyldighetsperiode: Periode<LocalDate>
    ): List<Valutakursgrunnlag> {
        if (gyldighetsperiode.til == null) {
            return emptyList()
        }
        val nå = LocalDate.now()
        val aktiv = gyldighetsperiode.inneholder(nå)

        return hentetValutakurser.map {
            val grunnlag = tilValutakursGrunnlagBo(it, gyldighetsperiode, nå, aktiv)
            persistenceService.opprettValutakursgrunnlag(grunnlag)
        }
    }

    private fun tilValutakursGrunnlagBo(
        hentetValutakursResultat: HentetValutakursResultat,
        gyldighetsperiode: Periode<LocalDate>,
        nå: LocalDate,
        aktiv: Boolean,
    ): ValutakursgrunnlagBo {
        return when (hentetValutakursResultat) {
            is HentetValutakursResultat.FeiledValutakurs ->
                ValutakursgrunnlagBo(
                    feiletHenting = true,
                    aktiv = aktiv,
                    brukFra = gyldighetsperiode.fom.atStartOfDay(),
                    brukTil = gyldighetsperiode.til!!.atStartOfDay(),
                    hentetTidspunkt = nå.atStartOfDay(),
                    kurs = null,
                    multiplikator = null,
                    basisvaluta = hentetValutakursResultat.basisvaluta,
                    kvoteringsvaluta = hentetValutakursResultat.kvoteringsvaluta,
                )

            is HentetValutakursResultat.HentetValutakurs ->
                ValutakursgrunnlagBo(
                    aktiv = aktiv,
                    brukFra = gyldighetsperiode.fom.atStartOfDay(),
                    brukTil = gyldighetsperiode.til!!.atStartOfDay(),
                    hentetTidspunkt = nå.atStartOfDay(),
                    kurs = hentetValutakursResultat.valutakursSnitt,
                    multiplikator = hentetValutakursResultat.multiplikator,
                    basisvaluta = hentetValutakursResultat.basisvaluta,
                    kvoteringsvaluta = hentetValutakursResultat.kvoteringsvaluta,
                )
        }
    }

    fun hentValutakursgrunnlag(valutakode: Valutakode, dato: LocalDate): ValutakursgrunnlagBo? {
        return persistenceService.hentValutakursgrunnlag(valutakode, dato)?.toValutakursgrunnlagBo()
    }

    fun fraNok(beløpNok: BigDecimal, valutakode: Valutakode, dato: LocalDate = LocalDate.now()): BigDecimal {
        val valutakursgrunnlag = hentValutakursgrunnlag(valutakode, dato) ?: return BigDecimal.ZERO // TODO error handling
        val kurs = valutakursgrunnlag.kurs ?: return BigDecimal.ZERO
        val multiplikator = valutakursgrunnlag.multiplikator ?: return BigDecimal.ZERO

        val beløp = ((beløpNok / kurs) * BigDecimal(10.0.pow(multiplikator.toDouble())))
        return beløp
    }

    fun tilNok(beløp: BigDecimal, valutakode: Valutakode, dato: LocalDate = LocalDate.now()): BigDecimal {
        // TODO ensure kurs > 0
        val valutakursgrunnlag = hentValutakursgrunnlag(valutakode, dato) ?: return BigDecimal.ZERO
        val kurs = valutakursgrunnlag.kurs ?: return BigDecimal.ZERO
        val multiplikator = valutakursgrunnlag.multiplikator ?: return BigDecimal.ZERO

        val beløpNok = (beløp * BigDecimal(10.0.pow(multiplikator.toDouble()))) / kurs
        return beløpNok
    }
}