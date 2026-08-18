package no.nav.bidrag.grunnlag.consumer.arbeidsforhold.api

data class HentArbeidsforholdRequest(
    val arbeidstakerId: String,
    val arbeidsforholdtypeFilter: String? = null,
    val rapporteringsordningFilter: String? = null,
    val arbeidsforholdstatusFilter: String? = null,
    val historikk: Boolean = true,
    val sporingsinformasjon: Boolean = true,
)
