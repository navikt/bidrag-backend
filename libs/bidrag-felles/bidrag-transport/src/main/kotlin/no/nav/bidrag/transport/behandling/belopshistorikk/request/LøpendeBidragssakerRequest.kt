package no.nav.bidrag.transport.behandling.belopshistorikk.request

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.ident.Personident
import java.time.LocalDate

@Schema(description = "Request for å hente alle løpende bidragssaker for angitt skyldner på angitt dato")
data class LøpendeBidragssakerRequest(
    @Schema(description = "Personen som er skyldner")
    val skyldner: Personident,
    @Schema(description = "Dato som det ønskes å hente gyldige perioder for")
    val dato: LocalDate = LocalDate.now(),
)
