package no.nav.bidrag.transport.reskontro.response.innkrevingssaksinformasjon

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.ident.Ident
import java.math.BigDecimal
import java.time.LocalDateTime

@Schema(
    name = "Innkrevingssakshistorikk",
    description = "Inneholder informasjon om historikken til innkrevingssaken.",
)
data class InnkrevingssakshistorikkDto(
    @field:Schema(
        description = "Beskrivelse av hva posten innebar. E.g \"OCR Innbetaling\" eller \"Påløp avdragsordning\".",
    )
    val beskrivelse: String?,
    @field:Schema(
        description = "Ident knyttet til det historiske innslaget.",
    )
    val ident: Ident?,
    @field:Schema(
        description = "Navn til ident knyttet til det historiske innslaget.",
    )
    val navn: String?,
    @field:Schema(
        description = "Tidspunkt for innslaget.",
    )
    val dato: LocalDateTime?,
    @field:Schema(
        description = "Innbetalt beløp.",
    )
    val beløp: BigDecimal?,
)
