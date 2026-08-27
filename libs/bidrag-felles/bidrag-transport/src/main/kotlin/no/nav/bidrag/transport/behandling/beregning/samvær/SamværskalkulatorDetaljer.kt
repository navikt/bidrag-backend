package no.nav.bidrag.transport.behandling.beregning.samvær

import no.nav.bidrag.domene.enums.samværskalkulator.SamværskalkulatorFerietype
import no.nav.bidrag.domene.enums.samværskalkulator.SamværskalkulatorNetterFrekvens
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagInnhold
import java.math.BigDecimal

data class SamværskalkulatorDetaljer(
    val ferier: List<SamværskalkulatorFerie> = emptyList(),
//    @field:PositiveOrZero(message = "Reglemessig samvær netter kan ikke være negativ")
//    @field:Max(value = 15, message = "Regelmessig samvær kan ikke være over 15 netter")
    val regelmessigSamværNetter: BigDecimal,
) : GrunnlagInnhold {
    data class SamværskalkulatorFerie(
        val type: SamværskalkulatorFerietype,
//        @field:PositiveOrZero(message = "Bidragsmottaker netter kan ikke være negativ")
        val bidragsmottakerNetter: BigDecimal = BigDecimal.ZERO,
//        @field:PositiveOrZero(message = "Bidragspliktig netter kan ikke være negativ")
        val bidragspliktigNetter: BigDecimal = BigDecimal.ZERO,
        val frekvens: SamværskalkulatorNetterFrekvens,
    )
}
