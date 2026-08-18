package no.nav.bidrag.grunnlag.consumer.pensjon.api

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import java.math.BigDecimal
import java.time.LocalDate

data class HentBarnetilleggPensjonRequest(
    val mottaker: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonSerialize(using = LocalDateSerializer::class)
    val fom: LocalDate,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonSerialize(using = LocalDateSerializer::class)
    val tom: LocalDate,
    val returnerFellesbarn: Boolean = true,
    val returnerSaerkullsbarn: Boolean = true,
)

data class HentBarnetilleggPensjonResponse(val barnetilleggPensjonListe: List<BarnetilleggPensjon>? = emptyList())

data class BarnetilleggPensjon(val barn: String, val beloep: BigDecimal, val fom: LocalDate, val tom: LocalDate, val erFellesbarn: Boolean)
