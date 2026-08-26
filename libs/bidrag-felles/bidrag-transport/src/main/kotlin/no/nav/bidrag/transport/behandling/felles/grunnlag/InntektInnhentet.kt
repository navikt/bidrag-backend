package no.nav.bidrag.transport.behandling.felles.grunnlag

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.barnetilsyn.Skolealder
import no.nav.bidrag.domene.enums.barnetilsyn.Tilsynstype
import no.nav.bidrag.domene.enums.grunnlag.GrunnlagDatakilde
import no.nav.bidrag.domene.tid.Datoperiode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

data class InnhentetArbeidsforhold(
    override val datakilde: GrunnlagDatakilde = GrunnlagDatakilde.AAREG,
    override val grunnlag: List<Arbeidsforhold>,
    override val hentetTidspunkt: LocalDateTime,
) : InnhentetGrunnlagInnhold<List<InnhentetArbeidsforhold.Arbeidsforhold>> {
    data class Arbeidsforhold(
        @Schema(description = "Startdato for arbeidsforholdet")
        val startdato: LocalDate?,
        @Schema(description = "Eventuell sluttdato for arbeidsforholdet")
        val sluttdato: LocalDate?,
        @Schema(description = "Navn på arbeidsgiver")
        val arbeidsgiverNavn: String?,
        @Schema(description = "Arbeidsgivers organisasjonsnummer")
        val arbeidsgiverOrgnummer: String?,
        @Schema(description = "Liste av ansettelsesdetaljer, med eventuell historikk")
        val ansettelsesdetaljerListe: List<Ansettelsesdetaljer> = emptyList(),
        @Schema(description = "Liste over registrerte permisjoner")
        val permisjonListe: List<Permisjon> = emptyList(),
        @Schema(description = "Liste over registrerte permitteringer")
        val permitteringListe: List<Permittering> = emptyList(),
    ) {
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
    }

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
}

data class InnhentetSkattegrunnlag(
    val periode: Datoperiode,
    val år: Int = periode.fom.year,
    override val datakilde: GrunnlagDatakilde = GrunnlagDatakilde.SIGRUN,
    override val hentetTidspunkt: LocalDateTime,
    override val grunnlag: List<Skattegrunnlagspost>,
) : InnhentetGrunnlagInnhold<List<InnhentetSkattegrunnlag.Skattegrunnlagspost>> {
    data class Skattegrunnlagspost(
        @Schema(description = "Type skattegrunnlag: Ordinær eller Svalbard")
        val skattegrunnlagType: String,
        @Schema(
            description =
                "Tekniske navnet på inntektsposten. Er samme verdi som \"Summert skattegrunnlag\"" +
                    " fra NAV kodeverk ( https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/Summert%20skattegrunnlag )",
        )
        val kode: String,
        @Schema(description = "Beløp")
        val beløp: BigDecimal,
    )
}

data class InnhentetBarnetillegg(
    override val datakilde: GrunnlagDatakilde = GrunnlagDatakilde.PENSJON,
    override val hentetTidspunkt: LocalDateTime,
    override val grunnlag: List<Barnetillegg>,
) : InnhentetGrunnlagInnhold<List<InnhentetBarnetillegg.Barnetillegg>> {
    data class Barnetillegg(
        val periode: Datoperiode,
        @Schema(description = "Referansen barnet barnetillegget er rapportert for")
        val gjelderBarn: Grunnlagsreferanse,
        @Schema(description = "Type barnetillegg.")
        val barnetilleggType: String,
        val beløpBrutto: BigDecimal,
        @Schema(description = "Angir om barnet er felles- eller særkullsbarn")
        val barnType: String,
    )

    fun hentBarnetilleggForBarn(barnReferanse: Grunnlagsreferanse) = grunnlag.filter { it.gjelderBarn == barnReferanse }
}

data class InnhentetTilleggstønad(
    override val datakilde: GrunnlagDatakilde = GrunnlagDatakilde.TILLEGGSSTØNAD_SAK,
    override val hentetTidspunkt: LocalDateTime,
    override val grunnlag: List<Tilleggsstønad>,
) : InnhentetGrunnlagInnhold<List<InnhentetTilleggstønad.Tilleggsstønad>> {
    data class Tilleggsstønad(
        val periode: Datoperiode,
        val harInnvilgetVedtak: Boolean,
    )
}

data class InnhentetAinntekt(
    override val datakilde: GrunnlagDatakilde = GrunnlagDatakilde.INNTEKSKOMPONENTEN,
    override val hentetTidspunkt: LocalDateTime,
    override val grunnlag: List<AinntektInnhentet>,
) : InnhentetGrunnlagInnhold<List<InnhentetAinntekt.AinntektInnhentet>> {
    data class AinntektInnhentet(
        val periode: Datoperiode,
        val ainntektspostListe: List<Ainntektspost>,
    )

    data class Ainntektspost(
        val utbetalingsperiode: String?,
        val opptjeningsperiodeFra: LocalDate?,
        val opptjeningsperiodeTil: LocalDate?,
        val kategori: String,
        val opplysningspliktigId: String?,
        val virksomhetId: String?,
        val fordelType: String?,
        val beløp: BigDecimal,
        val etterbetalingsperiodeFra: LocalDate?,
        val etterbetalingsperiodeTil: LocalDate?,
        val beskrivelse: String? = null,
    )
}

// Innhentet grunnlag (rådata)

data class InnhentetUtvidetBarnetrygd(
    override val datakilde: GrunnlagDatakilde = GrunnlagDatakilde.FAMILIE_BA_SAK,
    override val hentetTidspunkt: LocalDateTime,
    override val grunnlag: List<UtvidetBarnetrygd>,
) : InnhentetGrunnlagInnhold<List<InnhentetUtvidetBarnetrygd.UtvidetBarnetrygd>> {
    data class UtvidetBarnetrygd(
        val periode: Datoperiode,
        @Schema(description = "Beløp")
        val beløp: BigDecimal,
        @Schema(description = "Angir om stønaden er manuelt beregnet")
        val manueltBeregnet: Boolean,
    )
}

data class InnhentetSmåbarnstillegg(
    override val datakilde: GrunnlagDatakilde = GrunnlagDatakilde.FAMILIE_BA_SAK,
    override val hentetTidspunkt: LocalDateTime,
    override val grunnlag: List<Småbarnstillegg>,
) : InnhentetGrunnlagInnhold<List<InnhentetSmåbarnstillegg.Småbarnstillegg>> {
    data class Småbarnstillegg(
        val periode: Datoperiode,
        @Schema(description = "Beløp")
        val beløp: BigDecimal,
        @Schema(description = "Angir om stønaden er manuelt beregnet")
        val manueltBeregnet: Boolean,
    )
}

data class InnhentetBarnetilsyn(
    override val datakilde: GrunnlagDatakilde = GrunnlagDatakilde.FAMILIE_EF_SAK,
    override val hentetTidspunkt: LocalDateTime,
    override val grunnlag: List<Barnetilsyn>,
) : InnhentetGrunnlagInnhold<List<InnhentetBarnetilsyn.Barnetilsyn>> {
    data class Barnetilsyn(
        val periode: Datoperiode,
        val gjelderBarn: Grunnlagsreferanse,
        val beløp: Int?,
        val tilsynstype: Tilsynstype?,
        val skolealder: Skolealder?,
    )

    fun hentBarnetilsynForBarn(barnReferanse: Grunnlagsreferanse) = grunnlag.filter { it.gjelderBarn == barnReferanse }
}

data class InnhentetKontantstøtte(
    override val datakilde: GrunnlagDatakilde = GrunnlagDatakilde.FAMILIE_KONTANTSTØTTE_SAK,
    override val hentetTidspunkt: LocalDateTime,
    override val grunnlag: List<Kontantstøtte>,
) : InnhentetGrunnlagInnhold<List<InnhentetKontantstøtte.Kontantstøtte>> {
    data class Kontantstøtte(
        val periode: Datoperiode,
        val gjelderBarn: Grunnlagsreferanse,
        val beløp: Int,
    )

    fun hentKontantstøtteForBarn(barnReferanse: Grunnlagsreferanse) = grunnlag.filter { it.gjelderBarn == barnReferanse }
}
