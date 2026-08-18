package no.nav.bidrag.grunnlag.consumer.familieefsak.api

import java.time.LocalDate

data class EksternePerioderRequest(val personIdent: String, val fomDato: LocalDate, val tomDato: LocalDate)

data class Ressurs(val data: EksternePerioderMedBeløpResponse)

data class EksternePerioderMedBeløpResponse(val perioder: List<EksternPeriodeMedBeløp>)

data class EksternPeriodeMedBeløp(val personIdent: String, val fomDato: LocalDate, val tomDato: LocalDate, val beløp: Int, val datakilde: String)
