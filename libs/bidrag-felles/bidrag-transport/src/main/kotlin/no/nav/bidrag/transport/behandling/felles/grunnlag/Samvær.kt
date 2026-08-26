package no.nav.bidrag.transport.behandling.felles.grunnlag

import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import java.math.BigDecimal

data class SamværsperiodeGrunnlag(
    override val periode: ÅrMånedsperiode,
    override val manueltRegistrert: Boolean = true,
    val samværsklasse: Samværsklasse,
) : GrunnlagPeriodeInnhold

data class DelberegningSamværsklasse(
    val samværsklasse: Samværsklasse,
    val gjennomsnittligSamværPerMåned: BigDecimal = BigDecimal.ZERO,
) : GrunnlagInnhold

data class DelberegningSamværsklasseNetter(
    val samværsklasserNetter: List<SamværsklasseNetter>,
) : GrunnlagInnhold {
    data class SamværsklasseNetter(
        val samværsklasse: Samværsklasse,
        val antallNetterFra: BigDecimal = BigDecimal.ZERO,
        val antallNetterTil: BigDecimal = BigDecimal.ZERO,
    ) : GrunnlagInnhold
}
