package no.nav.bidrag.transport.reskontro.response.innkrevingssaksinformasjon

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.ident.Organisasjonsnummer
import java.math.BigDecimal
import java.time.LocalDateTime

@Schema(
    name = "GjeldendeBetalingsordning",
    description = "Inneholder informasjon om gjeldende betalingsordning.",
)
data class GjeldendeBetalingsordningDto(
    @field:Schema(
        description = "Hvilken type behandlingsordning er. E.g \"Lønnstrekk\"",
    )
    val typeBehandlingsordning: String?,
    @field:Schema(
        description = "Hvor innbetalingen kommer fra. Orgnr for bedrift.",
    )
    val kilde: Organisasjonsnummer?,
    @field:Schema(
        description = "Navn på bedrift.",
    )
    val kildeNavn: String?,
    @field:Schema(
        description = "Dato for siste giro.",
    )
    val datoSisteGiro: LocalDateTime?,
    @field:Schema(
        description = "Dato for neste forfall.",
    )
    val nesteForfall: LocalDateTime?,
    @field:Schema(
        description = "Månedlig beløp.",
    )
    val beløp: BigDecimal?,
    @field:Schema(
        description = "Sist endret tidspunkt.",
    )
    val sistEndret: LocalDateTime?,
    @field:Schema(
        description = "Årsak for endring.",
    )
    val sistEndretÅrsak: String?,
    @field:Schema(
        description = "Sum av ubetalt gjeld.",
    )
    val sumUbetalt: BigDecimal?,
)
