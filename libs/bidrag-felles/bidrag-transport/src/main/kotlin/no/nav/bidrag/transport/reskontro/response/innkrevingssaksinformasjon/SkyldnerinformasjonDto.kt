package no.nav.bidrag.transport.reskontro.response.innkrevingssaksinformasjon

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.ident.Personident
import java.math.BigDecimal

@Schema(
    name = "Skyldnerinformasjon",
    description = "Inneholder informasjon om skyldneren i innkrevingssaken.",
)
data class SkyldnerinformasjonDto(
    @field:Schema(
        description = "Identen til skyldner",
    )
    val personident: Personident?,
    @field:Schema(
        description = "Summen av det løpende bidraget på skyldner. ",
    )
    val sumLøpendeBidrag: BigDecimal?,
    @field:Schema(
        description = "Gjeldene status på innkrevingssaken. ",
    )
    val innkrevingssaksstatus: String?,
    @field:Schema(
        description =
            "Fakturamåte. \nGyldige verdier er følgende:\n" +
                "Vanlig giro\n" +
                "Avtalegiro m/orientering\n" +
                "Avtalegiro u/orientering\n" +
                "Ingen med purring/arbeidsflyt\n" +
                "Ingen uten purring/arbeidsflyt\n",
    )
    val fakturamåte: String?,
    @field:Schema(
        description = "Siste aktivitet som har oppdatert status på saken.",
    )
    val sisteAktivitet: String?,
)
