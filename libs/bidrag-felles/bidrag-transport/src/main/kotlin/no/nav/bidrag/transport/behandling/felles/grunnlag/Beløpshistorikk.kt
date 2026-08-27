package no.nav.bidrag.transport.behandling.felles.grunnlag

import com.fasterxml.jackson.annotation.JsonAlias
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import java.math.BigDecimal
import java.time.LocalDateTime

data class BeløpshistorikkGrunnlag(
    val tidspunktInnhentet: LocalDateTime = LocalDateTime.now(),
    @JsonAlias("førsteIndeksreguleringsår")
    val nesteIndeksreguleringsår: Int? = null,
    val beløpshistorikk: List<BeløpshistorikkPeriode>,
) : GrunnlagInnhold

data class BeløpshistorikkPeriode(
    val periode: ÅrMånedsperiode,
    val beløp: BigDecimal?,
    val valutakode: String?,
    val vedtaksid: Int? = null,
)
