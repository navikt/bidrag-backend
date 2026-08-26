package no.nav.bidrag.transport.behandling.felles.grunnlag

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.tid.ÅrMånedsperiode

@Schema(description = "Samværsklasse for et barn")
data class SamværsklassePeriode(
    override val periode: ÅrMånedsperiode,
    val samværsklasse: Samværsklasse,
    override val manueltRegistrert: Boolean,
) : GrunnlagPeriodeInnhold
