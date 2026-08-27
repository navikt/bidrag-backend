package no.nav.bidrag.transport.reskontro.response.innkrevingssak

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.ident.Personident
import java.math.BigDecimal

@Schema(
    name = "Skyldner",
    description = "Informasjon om skyldner.",
)
data class SkyldnerDto(
    @field:Schema(
        description = "Identen til skyldner",
    )
    val personident: Personident?,
    @field:Schema(
        description = "Sum av beløpsfelt fra innbetalinger i historikk avhengig av fordelingsstatus.",
    )
    val innbetaltBeløpUfordelt: BigDecimal?,
    @field:Schema(
        description = "Sum av saldo gebyrer på sak.",
    )
    val gjeldIlagtGebyr: BigDecimal?,
)
