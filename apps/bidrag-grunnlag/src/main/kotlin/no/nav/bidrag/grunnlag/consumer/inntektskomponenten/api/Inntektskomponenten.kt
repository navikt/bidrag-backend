package no.nav.bidrag.grunnlag.consumer.inntektskomponenten.api

import org.springframework.http.HttpStatusCode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

data class HentInntektRequest(val ident: String, val maanedFom: String, val maanedTom: String, val ainntektsfilter: String, val formaal: String)

data class HentInntektListeRequest(
    val ident: Aktoer,
    val maanedFom: YearMonth,
    val maanedTom: YearMonth,
    val ainntektsfilter: String,
    val formaal: String,
)

data class Aktoer(val identifikator: String, val aktoerType: String = "NATURLIG_IDENT")

data class HentInntektListeResponseIntern(
    val httpStatus: HttpStatusCode,
    val melding: String,
    val arbeidsInntektMaanedIntern: List<ArbeidsInntektMaanedIntern>?,
    val exceptionKastet: Boolean,
)

data class ArbeidsInntektMaanedIntern(val aarMaaned: String, var arbeidsInntektInformasjonIntern: ArbeidsInntektInformasjonIntern)

data class ArbeidsInntektInformasjonIntern(val inntektIntern: List<InntektIntern>?)

data class InntektIntern(
    val inntektType: String,
    val beloep: BigDecimal,
    val fordel: String?,
    val inntektsperiodetype: String?,
    val opptjeningsperiodeFom: LocalDate?,
    val opptjeningsperiodeTom: LocalDate?,
    val utbetaltIMaaned: String?,
    val opplysningspliktig: OpplysningspliktigIntern?,
    val virksomhet: VirksomhetIntern?,
    val tilleggsinformasjon: TilleggsinformasjonIntern?,
    val beskrivelse: String?,
)

data class OpplysningspliktigIntern(val identifikator: String?, val aktoerType: String?)

data class VirksomhetIntern(val identifikator: String?, val aktoerType: String?)

data class TilleggsinformasjonIntern(val kategori: String, val tilleggsinformasjonDetaljer: TilleggsinformasjonDetaljerIntern)

data class TilleggsinformasjonDetaljerIntern(val etterbetalingsperiodeFom: LocalDate?, val etterbetalingsperiodeTom: LocalDate?)
