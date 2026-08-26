package no.nav.bidrag.transport.behandling.grunnlag.response

import io.swagger.v3.oas.annotations.media.Schema

data class HentGrunnlagspakkeDto(
    @Schema(description = "grunnlagspakke-id")
    val grunnlagspakkeId: Int,
    @Schema(description = "Periodisert liste over innhentede inntekter fra a-inntekt og underliggende poster")
    val ainntektListe: List<AinntektDto>,
    @Schema(description = "Periodisert liste over innhentede fra skatt og underliggende poster")
    val skattegrunnlagListe: List<SkattegrunnlagDto>,
    @Schema(description = "Periodisert liste over innhentet utvidet barnetrygd og småbarnstillegg")
    val ubstListe: List<UtvidetBarnetrygdOgSmaabarnstilleggDto>,
    @Schema(description = "Periodisert liste over innhentet barnetillegg")
    val barnetilleggListe: List<BarnetilleggDto>,
    @Schema(description = "Periodisert liste over innhentet kontantstøtte")
    val kontantstotteListe: List<KontantstotteDto>,
    @Schema(
        description =
            "Liste over alle personer som har bodd sammen med BM/BP i perioden fra virkningstidspunkt og fremover med en liste " +
                "over hvilke perioder de har delt bolig. " +
                "Listen inkluderer i tillegg personens egne barn, selv om de ikke har delt bolig med BM/BP",
    )
    val husstandmedlemmerOgEgneBarnListe: List<RelatertPersonDto>,
    @Schema(description = "Periodisert liste over en persons sivilstand")
    val sivilstandListe: List<SivilstandDto>,
    @Schema(description = "Periodisert liste over innhentet barnetilsyn")
    val barnetilsynListe: List<BarnetilsynDto>,
)
