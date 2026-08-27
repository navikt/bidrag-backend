package no.nav.bidrag.transport.behandling.felles.grunnlag

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.tid.ÅrMånedsperiode

@Schema(description = "Delt bosted for person")
data class DeltBostedPeriode(
    override val periode: ÅrMånedsperiode,
    val deltBosted: Boolean,
    override val manueltRegistrert: Boolean,
) : GrunnlagPeriodeInnhold
