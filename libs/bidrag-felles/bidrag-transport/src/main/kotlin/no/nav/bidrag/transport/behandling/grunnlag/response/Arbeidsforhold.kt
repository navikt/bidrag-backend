package no.nav.bidrag.transport.behandling.grunnlag.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.YearMonth

data class ArbeidsforholdGrunnlagDto(
    @Schema(description = "Id til personen arbeidsforholdet gjelder")
    val partPersonId: String,
    @Schema(description = "Startdato for arbeidsforholdet")
    val startdato: LocalDate?,
    @Schema(description = "Eventuell sluttdato for arbeidsforholdet")
    val sluttdato: LocalDate?,
    @Schema(description = "Navn på arbeidsgiver")
    val arbeidsgiverNavn: String?,
    @Schema(description = "Arbeidsgivers organisasjonsnummer")
    val arbeidsgiverOrgnummer: String?,
    @Schema(description = "Liste av ansettelsesdetaljer, med eventuell historikk")
    val ansettelsesdetaljerListe: List<Ansettelsesdetaljer>?,
    @Schema(description = "Liste over registrerte permisjoner")
    val permisjonListe: List<Permisjon>?,
    @Schema(description = "Liste over registrerte permitteringer")
    val permitteringListe: List<Permittering>?,
)

data class Ansettelsesdetaljer(
    @Schema(description = "Fradato for ansettelsesdetalj. År + måned")
    val periodeFra: YearMonth?,
    @Schema(description = "Eventuell sluttdato for ansettelsesdetalj. År + måned")
    val periodeTil: YearMonth?,
    @Schema(description = "Type arbeidsforhold, Ordinaer, Maritim, Forenklet, Frilanser'")
    val arbeidsforholdType: String?,
    @Schema(description = "Beskrivelse av arbeidstidsordning. Eks: 'Ikke skift'")
    val arbeidstidsordningBeskrivelse: String?,
    @Schema(description = "Beskrivelse av ansettelsesform. Eks: 'Fast ansettelse'")
    val ansettelsesformBeskrivelse: String?,
    @Schema(description = "Beskrivelse av yrke. Eks: 'KONTORLEDER'")
    val yrkeBeskrivelse: String?,
    @Schema(description = "Avtalt antall timer i uken")
    val antallTimerPrUke: Double?,
    @Schema(description = "Avtalt stillingsprosent")
    val avtaltStillingsprosent: Double?,
    @Schema(description = "Dato for forrige endring i stillingsprosent")
    val sisteStillingsprosentendringDato: LocalDate?,
    @Schema(description = "Dato for forrige lønnsendring")
    val sisteLønnsendringDato: LocalDate?,
)

data class Permisjon(
    val startdato: LocalDate?,
    val sluttdato: LocalDate?,
    val beskrivelse: String?,
    val prosent: Double?,
)

data class Permittering(
    val startdato: LocalDate?,
    val sluttdato: LocalDate?,
    val beskrivelse: String?,
    val prosent: Double?,
)
