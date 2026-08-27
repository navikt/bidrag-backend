package no.nav.bidrag.transport.reskontro.response.innkrevingssaksinformasjon

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    name = "Innkrevingssaksinformasjon",
    description = "Inneholder informasjon om innkrevingssaken.",
)
data class InnkrevingssaksinformasjonDto(
    val skyldnerinformasjon: SkyldnerinformasjonDto?,
    val gjeldendeBetalingsordning: GjeldendeBetalingsordningDto?,
    val nyBetalingsordning: NyBetalingsordningDto?,
    val innkrevingssakshistorikk: List<InnkrevingssakshistorikkDto>?,
)
