package no.nav.bidrag.transport.behandling.grunnlag.response

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestStatus
import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType

@Schema(description = "Respons ved oppdatering av  grunnlagspakke")
data class OppdaterGrunnlagspakkeDto(
    @Schema(description = "Grunnlagspakke-id")
    val grunnlagspakkeId: Int,
    @Schema(description = "Liste over grunnlagene som er hentet inn med person-id og status")
    val grunnlagTypeResponsListe: List<OppdaterGrunnlagDto>,
)

data class OppdaterGrunnlagDto(
    @Schema(description = "Hvilken type grunnlag som er hentet")
    val type: GrunnlagRequestType,
    @Schema(description = "Angir personId som grunnlag er hentet for")
    val personId: String,
    @Schema(description = "Status for utført kall")
    val status: GrunnlagRequestStatus,
    @Schema(description = "Statusmelding for utført kall")
    val statusMelding: String,
)
