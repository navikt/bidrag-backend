package no.nav.bidrag.transport.samhandler

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.transport.felles.QueryObject

@Schema(description = "Query-felter for søk etter samhandlere.")
@Deprecated(message = "Utgår da denne er basert på TSS.", replaceWith = ReplaceWith("SamhandlerSøk"))
data class SøkSamhandlerQuery(
    val navn: String,
    val postnummer: String? = null,
    @JsonProperty("omrade")
    val område: String? = null,
    @Schema(description = "Sidenummer med resultater man ønsker, hvis det finnes og man ønsker påfølgende resultater.")
    val side: Int = 0,
) : QueryObject
