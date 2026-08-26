package no.nav.bidrag.transport.behandling.statistikk

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class ForskuddHendelse(
    val vedtaksid: Int,
    val vedtakstidspunkt: LocalDateTime,
    val type: String,
    val saksnr: String,
    val kravhaver: String,
    val mottaker: String,
    val historiskVedtak: Boolean,
    val forskuddPeriodeListe: List<ForskuddPeriode>,
)

data class ForskuddPeriode(
    val periodeFra: LocalDate,
    val periodeTil: LocalDate?,
    val beløp: BigDecimal?,
    val resultat: String,
    val barnetsAldersgruppe: String?,
    val antallBarnIEgenHusstand: Double?,
    val sivilstand: String?,
    val barnBorMedMottaker: Boolean?,
    val mottakerInntektListe: List<Inntekt>,
    val kravhaverInntektListe: List<Inntekt>?,
)
