package no.nav.bidrag.transport.reskontro.response.innkrevingssak

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.sak.Saksnummer
import java.math.BigDecimal

@Schema(
    name = "Bidragssak",
    description = "Inneholder informasjon om bidragssaken fra skatt",
)
data class BidragssakDto(
    @field:Schema(
        description = "Identifikasjonen til bidragssaken.",
    )
    val saksnummer: Saksnummer?,
    @field:Schema(
        description = "Resterende gjeld til BM på fastsettelsesgebyret (G1).",
    )
    val bmGjeldFastsettelsesgebyr: BigDecimal?,
    @field:Schema(
        description = "Resterende gjeld til BM. Gjelder for H1 - Tilbakekreving.",
    )
    val bmGjeldRest: BigDecimal?,
    @field:Schema(
        description = "Resterende gjeld til BP på fastsettelsesgebyret (G1).",
    )
    val bpGjeldFastsettelsesgebyr: BigDecimal?,
    @field:Schema(
        description = "Liste over alle barn i bidragssaken med tilhørende innkrevingsinformasjon.",
    )
    val barn: List<SaksinformasjonBarnDto>,
)
