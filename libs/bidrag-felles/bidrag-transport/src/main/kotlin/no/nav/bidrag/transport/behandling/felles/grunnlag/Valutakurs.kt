package no.nav.bidrag.transport.behandling.felles.grunnlag

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import java.math.BigDecimal

@Schema(description = "Liste over valutakurser")
data class ValutakursGrunnlag(
    val periode: ÅrMånedsperiode,
    val valutakursListe: List<ValutaPar>,
) : GrunnlagInnhold

@Schema(description = "Valutakurs mellom 2 valutaer")
data class ValutaPar(
    val valutakode1: Valutakode,
    val valutakode2: Valutakode,
    val valutakurs: BigDecimal,
) : GrunnlagInnhold
