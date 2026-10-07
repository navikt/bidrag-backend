package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.tid.Datoperiode
import no.nav.bidrag.domene.tid.Periode
import no.nav.bidrag.grunnlag.bo.ValutakursgrunnlagBo
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakurs
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakursRequest
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentetValutakursResultat
import no.nav.bidrag.grunnlag.persistence.entity.Valutakursgrunnlag
import no.nav.bidrag.grunnlag.persistence.entity.toValutakursgrunnlagBo
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime

data class Valutaberegning(
    val beløp: BigDecimal,
    val kurs: BigDecimal,
    val kursgrunnlag: ValutakursgrunnlagBo?,
    val fraValuta: Valutakode,
    val tilValuta: Valutakode,
    val dato: LocalDate,
)

@Service
class ValutakursgrunnlagService(
    private val persistenceService: PersistenceService,
    private val hentValutakursService: HentValutakursService,
) {
    fun innhent(valutakode: Valutakode, gyldigFra: LocalDate): ValutakursgrunnlagBo {
        val resultat = hentValutakursService.hentValutakurs(HentValutakursRequest(listOf(HentValutakurs(gyldigFra, valutakode))))
        return opprettValutakursgrunnlag(resultat.hentetValutakursListe, Datoperiode(gyldigFra, gyldigFra.plusMonths(6))).single().toValutakursgrunnlagBo()
    }

    fun overstyr(id: Int, kurs: BigDecimal): ValutakursgrunnlagBo = persistenceService.overstyrValutakursgrunnlag(id, kurs).toValutakursgrunnlagBo()

    fun opprettValutakursgrunnlag(
        hentetValutakurser: List<HentetValutakursResultat>,
        gyldighetsperiode: Periode<LocalDate>,
    ): List<Valutakursgrunnlag> {
        requireNotNull(gyldighetsperiode.til) { "Gyldighetsperioden må ha en sluttdato" }
        return hentetValutakurser.map {
            val grunnlag = tilValutakursGrunnlagBo(it, gyldighetsperiode)
            try {
                persistenceService.opprettValutakursgrunnlag(grunnlag)
            } catch (e: DataIntegrityViolationException) {
                persistenceService.opprettValutakursgrunnlag(grunnlag)
            }
        }
    }

    private fun tilValutakursGrunnlagBo(
        hentetValutakursResultat: HentetValutakursResultat,
        gyldighetsperiode: Periode<LocalDate>,
    ): ValutakursgrunnlagBo = when (hentetValutakursResultat) {
        is HentetValutakursResultat.FeiledValutakurs ->
            ValutakursgrunnlagBo(
                feiletHenting = true,
                brukFra = gyldighetsperiode.fom.atStartOfDay(),
                brukTil = gyldighetsperiode.til!!.atStartOfDay(),
                hentetTidspunkt = LocalDateTime.now(),
                kurs = null,
                multiplikator = null,
                basisvaluta = hentetValutakursResultat.basisvaluta,
                kvoteringsvaluta = hentetValutakursResultat.kvoteringsvaluta,
            )

        is HentetValutakursResultat.HentetValutakurs ->
            ValutakursgrunnlagBo(
                brukFra = gyldighetsperiode.fom.atStartOfDay(),
                brukTil = gyldighetsperiode.til!!.atStartOfDay(),
                hentetTidspunkt = hentetValutakursResultat.hentetTidspunkt,
                kurs = hentetValutakursResultat.valutakursSnitt,
                multiplikator = hentetValutakursResultat.multiplikator,
                basisvaluta = hentetValutakursResultat.basisvaluta,
                kvoteringsvaluta = hentetValutakursResultat.kvoteringsvaluta,
                kilde = hentetValutakursResultat.kilde,
                observasjonsdato = hentetValutakursResultat.periode.fom.atDay(1),
            )
    }

    fun hentValutakursgrunnlag(valutakode: Valutakode, dato: LocalDate): ValutakursgrunnlagBo? = persistenceService.hentValutakursgrunnlag(valutakode, dato)?.toValutakursgrunnlagBo()

    fun hentFeiledeValutakursgrunnlag(pageable: Pageable): Page<ValutakursgrunnlagBo> = persistenceService.hentFeiledeValutakursgrunnlag(pageable).map { it.toValutakursgrunnlagBo() }

    fun fraNok(beløpNok: BigDecimal, valutakode: Valutakode, dato: LocalDate = LocalDate.now()): BigDecimal {
        validerBeløp(beløpNok)
        if (valutakode == Valutakode.NOK) return beløpNok.setScale(4, RoundingMode.HALF_UP)
        return beløpNok.divide(hentGyldigKursgrunnlag(valutakode, dato).kurs, 4, RoundingMode.HALF_UP)
    }

    fun tilNok(beløp: BigDecimal, valutakode: Valutakode, dato: LocalDate = LocalDate.now()): BigDecimal {
        validerBeløp(beløp)
        if (valutakode == Valutakode.NOK) return beløp.setScale(4, RoundingMode.HALF_UP)
        return beløp.multiply(hentGyldigKursgrunnlag(valutakode, dato).kurs).setScale(4, RoundingMode.HALF_UP)
    }

    fun beregn(beløp: BigDecimal, fraValuta: Valutakode, tilValuta: Valutakode, dato: LocalDate): Valutaberegning {
        validerBeløp(beløp)
        if (fraValuta == Valutakode.NOK && tilValuta == Valutakode.NOK) return Valutaberegning(beløp.setScale(4, RoundingMode.HALF_UP), BigDecimal.ONE, null, fraValuta, tilValuta, dato)
        if (fraValuta != Valutakode.NOK && tilValuta != Valutakode.NOK) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Kun omregning til eller fra NOK støttes")
        }
        val valutakode = if (fraValuta == Valutakode.NOK) tilValuta else fraValuta
        val grunnlag = hentGyldigKursgrunnlag(valutakode, dato)
        val resultat = if (tilValuta == Valutakode.NOK) beløp.multiply(grunnlag.kurs) else beløp.divide(grunnlag.kurs, 4, RoundingMode.HALF_UP)
        return Valutaberegning(resultat.setScale(4, RoundingMode.HALF_UP), grunnlag.kurs, grunnlag.grunnlag, fraValuta, tilValuta, dato)
    }

    private fun validerBeløp(beløp: BigDecimal) {
        if (beløp.precision() > 38 || beløp.scale() > 16 || beløp.precision().toLong() - beløp.scale().toLong() > 22) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Beløp kan ha maksimalt 22 heltallssifre og 16 desimalplasser")
        }
    }

    private fun hentGyldigKursgrunnlag(valutakode: Valutakode, dato: LocalDate): GyldigKursgrunnlag {
        val grunnlag = hentValutakursgrunnlag(valutakode, dato)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Valutakursgrunnlag finnes ikke")
        val kurs = grunnlag.kurs
        if (grunnlag.feiletHenting || kurs == null || kurs.signum() <= 0 || grunnlag.multiplikator != 0) {
            throw ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, "Valutakurs er ikke tilgjengelig")
        }
        return GyldigKursgrunnlag(kurs, grunnlag)
    }

    private data class GyldigKursgrunnlag(val kurs: BigDecimal, val grunnlag: ValutakursgrunnlagBo)
}
