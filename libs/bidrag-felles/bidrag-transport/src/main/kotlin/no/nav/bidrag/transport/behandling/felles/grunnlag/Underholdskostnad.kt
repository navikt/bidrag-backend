package no.nav.bidrag.transport.behandling.felles.grunnlag

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.barnetilsyn.Skolealder
import no.nav.bidrag.domene.enums.barnetilsyn.Tilsynstype
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import java.math.BigDecimal

@Schema(description = "Underholdskostnad for person")
data class UnderholdskostnadPeriode(
    override val periode: ÅrMånedsperiode,
    val beløp: BigDecimal,
    override val manueltRegistrert: Boolean,
) : GrunnlagPeriodeInnhold

@Schema(description = "Barnetilsyn med stønad fra Enslig Forsørger")
data class BarnetilsynMedStønadPeriode(
    override val periode: ÅrMånedsperiode,
    val tilsynstype: Tilsynstype,
    val skolealder: Skolealder,
    override val manueltRegistrert: Boolean,
) : GrunnlagPeriodeInnhold
