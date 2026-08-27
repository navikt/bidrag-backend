package no.nav.bidrag.transport.reskontro.response.innkrevingssak

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    name = "Bidragssak med skyldner",
    description = "Inneholder informasjon om bidragssaken fra skatt med skyldner",
)
data class BidragssakMedSkyldnerDto(
    val skyldner: SkyldnerDto?,
    val bidragssaker: List<BidragssakDto>?,
)
