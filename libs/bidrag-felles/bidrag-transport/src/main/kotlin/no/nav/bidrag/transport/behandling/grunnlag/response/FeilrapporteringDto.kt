package no.nav.bidrag.transport.behandling.grunnlag.response

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.domene.enums.grunnlag.HentGrunnlagFeiltype
import java.time.LocalDate

data class FeilrapporteringDto(
    val grunnlagstype: GrunnlagRequestType,
    @Schema(description = "Id til personen grunnlaget er forsøkt hentet for")
    val personId: String?,
    val periodeFra: LocalDate?,
    val periodeTil: LocalDate?,
    val feiltype: HentGrunnlagFeiltype,
    val feilmelding: String?,
)
