package no.nav.bidrag.grunnlag.consumer.arbeidsforhold.api

import java.time.LocalDate
import java.time.LocalDateTime

data class HentEnhetsregisterResponse(
    val organisasjonsnummer: String? = null,
    val navn: Navn? = null,
    val enhetstype: String? = null,
    val adresse: Adresse? = null,
    val opphoersdato: String? = null,
)

data class Navn(
    val bruksperiode: BruksperiodeEreg?,
    val gyldighetsperiode: Gyldighetsperiode?,
    val sammensattnavn: String?,
    val navnelinje1: String?,
    val navnelinje2: String?,
    val navnelinje3: String?,
    val navnelinje4: String?,
    val navnelinje5: String?,
)

data class Adresse(
    val bruksperiode: BruksperiodeEreg?,
    val gyldighetsperiode: Gyldighetsperiode?,
    val adresselinje1: String?,
    val adresselinje2: String?,
    val adresselinje3: String?,
    val postnummer: String?,
    val poststed: String?,
    val kommunenummer: String?,
    val landkode: String?,
)

data class BruksperiodeEreg(val fom: LocalDateTime?, val tom: LocalDateTime?)

data class Gyldighetsperiode(val fom: LocalDate?, val tom: LocalDate?)
