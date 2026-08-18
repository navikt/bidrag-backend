package no.nav.bidrag.statistikk.bo

import no.nav.bidrag.domene.enums.særbidrag.Særbidragskategori
import no.nav.bidrag.transport.behandling.statistikk.Inntekt
import java.math.BigDecimal
import java.time.LocalDateTime

data class SærbidragHendelse(
    val vedtaksid: Int,
    val vedtakstidspunkt: LocalDateTime,
    val type: String,
    val kategori: Særbidragskategori?, // KONFIRMASJON, TANNREGULERING, OPTIKK, ANNET
    val saksnr: String,
    val skyldner: String,
    val kravhaver: String,
    val mottaker: String,
    val beløp: BigDecimal?, // Skyldners andel av godkjent beløp
    val valutakode: String?,
    val resultat: String,
    val innkreving: Boolean,
    val omgjørVedtakId: Int?, // Vedtak som er omgjort av dette vedtaket
    val historiskVedtak: Boolean,
    val kravbeløp: BigDecimal?, // Beløpet mottaker ba om
    val godkjentBeløp: BigDecimal?, // Beløpet som ble godkjent av saksbehandler
    val betaltBeløp: BigDecimal?, // Beløpet som eventuelt er betalt tidligere av skyldner
    val bPInntektListe: List<Inntekt>?,
    val bMInntektListe: List<Inntekt>?,
)
