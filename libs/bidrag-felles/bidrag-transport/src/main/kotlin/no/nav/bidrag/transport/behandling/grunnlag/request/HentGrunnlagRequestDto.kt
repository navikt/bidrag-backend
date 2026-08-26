package no.nav.bidrag.transport.behandling.grunnlag.request

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.vedtak.Formål

// Request for å hente grunnlag direkte via eksterne tjenester uten å gå via grunnlagspakke og lagring i bidrag-grunnlag
data class HentGrunnlagRequestDto(
    @Schema(description = "Formål (BIDRAG, FORSKUDD eller SÆRBIDRAG). Brukes for å hente Ainntekt")
    val formaal: Formål,
    //    @field:Valid
//    @field:NotEmpty(message = "Listen kan ikke være null eller tom.")
    @Schema(description = "Liste over hvilke typer grunnlag som skal hentes inn. På nivået under er personId og perioder angitt")
    val grunnlagRequestDtoListe: List<GrunnlagRequestDto>,
)
