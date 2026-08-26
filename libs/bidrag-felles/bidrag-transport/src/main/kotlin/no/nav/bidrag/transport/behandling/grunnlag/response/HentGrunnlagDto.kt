package no.nav.bidrag.transport.behandling.grunnlag.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class HentGrunnlagDto(
    @Schema(description = "Periodisert liste over innhentede inntekter fra a-inntekt og underliggende poster")
    val ainntektListe: List<AinntektGrunnlagDto>,
    @Schema(description = "Periodisert liste over innhentede inntekter fra skatt og underliggende poster")
    val skattegrunnlagListe: List<SkattegrunnlagGrunnlagDto>,
    @Schema(description = "Periodisert liste over innhentet utvidet barnetrygd")
    val utvidetBarnetrygdListe: List<UtvidetBarnetrygdGrunnlagDto>,
    @Schema(description = "Periodisert liste over innhentet småbarnstillegg")
    val småbarnstilleggListe: List<SmåbarnstilleggGrunnlagDto>,
    @Schema(description = "Periodisert liste over innhentet barnetillegg")
    val barnetilleggListe: List<BarnetilleggGrunnlagDto>,
    @Schema(description = "Periodisert liste over innhentet kontantstøtte")
    val kontantstøtteListe: List<KontantstøtteGrunnlagDto>,
    @Schema(
        description =
            "Liste over alle personer som har bodd sammen med BM/BP i perioden fra virkningstidspunkt og fremover med en liste " +
                "over hvilke perioder de har delt bolig. " +
                "Listen inkluderer i tillegg personens egne barn, selv om de ikke har delt bolig med BM/BP",
    )
    val husstandsmedlemmerOgEgneBarnListe: List<RelatertPersonGrunnlagDto>,
    @Schema(description = "Periodisert liste over en persons sivilstand")
    val sivilstandListe: List<SivilstandGrunnlagDto>,
    @Schema(description = "Periodisert liste over innhentet barnetilsyn")
    val barnetilsynListe: List<BarnetilsynGrunnlagDto>,
    @Schema(description = "Periodisert liste over arbeidsforhold")
    val arbeidsforholdListe: List<ArbeidsforholdGrunnlagDto>,
    @Schema(
        description =
            "Indikator på om person mottar eller har mottatt tilleggsstønad til barnetilsyn i " +
                "enten Arena eller tilleggsstonader-sak. Responsen skal utvides med periode og beløp senere",
    )
    val tilleggsstønadBarnetilsynListe: List<TilleggsstønadGrunnlagDto> = emptyList(),
    @Schema(description = "Liste over evt. feil rapportert under henting av grunnlag")
    val feilrapporteringListe: List<FeilrapporteringDto>,
    val hentetTidspunkt: LocalDateTime,
)
