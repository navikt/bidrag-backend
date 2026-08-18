package no.nav.bidrag.grunnlag.consumer.arbeidsforhold.api

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

data class Arbeidsforhold(
    val ansettelsesdetaljer: List<AnsettelsesdetaljerAareg>? = emptyList(),
    val ansettelsesperiode: Ansettelsesperiode? = null,
    val arbeidssted: Arbeidssted? = null,
    val arbeidstaker: Arbeidstaker? = null,
    val bruksperiode: Bruksperiode? = null,
    val navArbeidsforholdId: Int? = null,
    val id: String? = null,
    val navVersjon: Int? = null,
    val opplysningspliktig: Opplysningspliktig? = null,
    val opprettet: String? = null,
    val rapporteringsordning: Kodeverksentitet? = null,
    val sistBekreftet: String? = null,
    val sistEndret: String? = null,
    val type: Kodeverksentitet? = null,
    val permisjoner: List<Permisjon>?,
    val permitteringer: List<Permittering>?,
)

data class AnsettelsesdetaljerAareg(
    val antallTimerPrUke: Double?,
    val arbeidstidsordning: Kodeverksentitet?,
    val ansettelsesform: Kodeverksentitet?,
    val avtaltStillingsprosent: Double?,
    val rapporteringsmaaneder: Rapporteringsmaaneder?,
    val type: String?,
    val yrke: Kodeverksentitet?,
    val sisteStillingsprosentendring: LocalDate?,
    val sisteLoennsendring: LocalDate?,
)

data class Ansettelsesperiode(val startdato: LocalDate?, val sluttdato: LocalDate?)

data class Arbeidssted(val identer: List<Identer>?, val type: String?)

data class Arbeidstaker(val identer: List<ArbeidsgiverIdenter>?)

data class Bruksperiode(val fom: String?, val tom: String?)

data class Opplysningspliktig(val identer: List<Identer>?, val type: String?)

data class Kodeverksentitet(val beskrivelse: String?, val kode: String?)

data class Rapporteringsmaaneder(val fra: YearMonth?, val til: YearMonth?)

data class Identer(val ident: String?, val type: String?)

data class ArbeidsgiverIdenter(val gjeldende: Boolean?, val ident: String?, val type: String?)

data class Permisjon(
    val startdato: LocalDate?,
    val sluttdato: LocalDate?,
    val id: String?,
    val type: Kodeverksentitet?,
    val prosent: Double?,
    val varsling: Kodeverksentitet?,
    val idHistorikk: List<IdHistorikk>?,
    val sporingsinformasjon: Sporingsinformasjon?,
)

data class Permittering(
    val startdato: LocalDate?,
    val sluttdato: LocalDate?,
    val id: String?,
    val type: Kodeverksentitet?,
    val prosent: Double?,
    val varsling: Kodeverksentitet?,
    val idHistorikk: List<IdHistorikk>?,
    val sporingsinformasjon: Sporingsinformasjon?,
)

data class IdHistorikk(val id: String?, val fom: LocalDate?, val tom: LocalDate?)

data class Sporingsinformasjon(
    val opprettetTidspunkt: LocalDateTime?,
    val opprettetAv: String?,
    val opprettetKilde: String?,
    val opprettetKildereferanse: String?,
    val endretTidspunkt: LocalDateTime?,
    val endretAv: String?,
    val endretKilde: String?,
    val endretKildereferanse: String?,
)
