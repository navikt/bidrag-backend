package no.nav.bidrag.transport.behandling.grunnlag.request

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.vedtak.Formål

@Schema(description = "Request for å opprette ny grunnlagspakke, uten annet innhold")
data class OpprettGrunnlagspakkeRequestDto(
    @Schema(description = "Til hvilket formål skal grunnlagspakken benyttes. BIDRAG, FORSKUDD eller SÆRBIDRAG")
    val formaal: Formål,
    @Schema(
        description = """
Id på hvem som oppretter grunnlagspakken. Kan være NAV-ident til saksbehandler eller Id på batchjobb.
        
`opprettetAv` må settes hvis grunnlagspakken opprettes i system kontekst (feks maskin-maskin kommunikasjon). Ellers hentes NAV-ident til sakbehandler fra tokenet
    """,
        example = "BATCH_1234/Z99999",
    )
    val opprettetAv: String? = null,
)
