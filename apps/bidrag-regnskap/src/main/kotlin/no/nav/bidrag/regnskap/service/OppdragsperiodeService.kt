package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.dto.vedtak.Hendelse
import no.nav.bidrag.regnskap.dto.vedtak.Periode
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import no.nav.bidrag.regnskap.persistence.repository.OppdragsperiodeRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
class OppdragsperiodeService(
    private val oppdragsperiodeRepository: OppdragsperiodeRepository,
) {

    fun opprettNyOppdragsperiode(hendelse: Hendelse, periode: Periode, oppdrag: Oppdrag): Oppdragsperiode = Oppdragsperiode(
        vedtakId = hendelse.vedtakId,
        referanse = hendelse.referanse,
        vedtakType = hendelse.vedtakType.toString(),
        beløp = periode.beløp ?: BigDecimal.ZERO,
        valuta = periode.valutakode ?: "NOK",
        periodeFra = periode.periodeFomDato,
        periodeTil = periode.periodeTilDato,
        vedtaksdato = hendelse.vedtakDato,
        opprettetAv = hendelse.opprettetAv,
        enhetsnummer = hendelse.enhetsnummer?.verdi,
        delytelseId = periode.delytelsesId,
        eksternReferanse = hendelse.eksternReferanse,
        oppdrag = oppdrag,
        opphørendeOppdragsperiode = periode.beløp == null,
    )

    fun oppdaterAktivTilDato(oppdrag: Oppdrag, nyOppdragsperiodePeriodeFra: LocalDate) {
        oppdrag.oppdragsperioder.forEach {
            if (it.aktivTil != null && it.aktivTil!!.isBefore(nyOppdragsperiodePeriodeFra)) {
                return@forEach
            }

            if (it.periodeTil != null && nyOppdragsperiodePeriodeFra.isAfter(it.periodeTil)) {
                it.aktivTil = it.periodeTil
            } else {
                it.aktivTil = nyOppdragsperiodePeriodeFra
            }
        }
    }

    fun hentAlleOppdragsperiodeMedVedtaksId(vedtakId: Int): List<Oppdragsperiode> = oppdragsperiodeRepository.findAllByVedtakId(vedtakId)
}
