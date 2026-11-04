package no.nav.bidrag.grunnlag.consumer.aap.api

import java.math.BigDecimal
import java.time.LocalDate

data class HentBarnetilleggAAPRequest(val personidentifikator: String)

data class HentBarnetilleggAAPResponse(val barnMedBarnetillegg: List<Barn> = emptyList())

data class Barn(val ident: String, val perioderMedBarnetillegg: List<PeriodeMedBarnetillegg>)

data class PeriodeMedBarnetillegg(val fra: LocalDate, val til: LocalDate?, val beløp: BigDecimal)
