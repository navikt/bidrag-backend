package no.nav.bidrag.transport.behandling.felles.grunnlag

import com.fasterxml.jackson.annotation.JsonAlias
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.diverse.InntektBeløpstype
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.inntekt.Inntektstype
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import java.math.BigDecimal
import java.time.YearMonth

@Schema(description = "Rapportert inntekt for person")
data class InntektsrapporteringPeriode(
    override val periode: ÅrMånedsperiode,
    override val manueltRegistrert: Boolean,
    @Schema(description = "Opprinnelig periode for inntekten hvis det er offentlig inntekt")
    val opprinneligPeriode: ÅrMånedsperiode? = null,
    @Schema(
        description =
            "Settes bare hvis manueltRegistrert = false. " +
                "Versjon bidrag-inntekt beregner inntekt basert på offentlig informasjon",
    )
    val versjon: String? = null,
    @Schema(description = "Type inntektsrapportering")
    val inntektsrapportering: Inntektsrapportering,
    @Schema(description = "Referanse til barnet inntekten gjelder for")
    val gjelderBarn: Grunnlagsreferanse? = null,
    @Schema(description = "Inntekt beløp")
    val beløp: BigDecimal,
    @Schema(description = "Om inntekt er tatt med i beregningen")
    val valgt: Boolean,
    @Schema(description = "Liste med inntektsposter som inntekten består av. Vil være tom hvis det er manuelt registrert")
    @JsonAlias("inntekstpostListe")
    val inntektspostListe: List<Inntektspost> = emptyList(),
) : GrunnlagPeriodeInnhold {
    data class Inntektspost(
        @Schema(description = "Kode for inntektspost", example = "bonus")
        val kode: String,
        @Schema(description = "Inntektstype inntektsposten er knyttet til", example = "PENSJON")
        @JsonAlias("inntekstype")
        val inntektstype: Inntektstype? = null,
        @Schema(description = "Beløp som utgjør inntektsposten", example = "60000")
        val beløp: BigDecimal,
        @Schema(description = "Typen på beløp. For barnetillegg så kan det være måndesbeløp eller dagsats", example = "60000")
        val beløpstype: InntektBeløpstype = InntektBeløpstype.ÅRSBELØP,
        @Schema(description = "Skattefaktor på barnetillegget. Brukes ved beregning av netto barnetillegg", example = "0.05")
        val skattefaktor: BigDecimal? = null,
    ) {
        constructor(
            kode: String,
            inntektstype: Inntektstype,
            beløp: BigDecimal,
            beløpstype: InntektBeløpstype,
        ) : this(kode, inntektstype, beløp, beløpstype, null)
    }
}

@Schema(description = "Inntekt beregnet av bidrag-inntekt basert på data fra bidrag-grunnlag")
data class BeregnetInntekt(
    val versjon: String,
    @Schema(description = "Liste over summerte månedsinntekter (Ainntekt ++))")
    val summertMånedsinntektListe: List<SummertMånedsinntekt> = emptyList(),
) : GrunnlagInnhold {
    data class SummertMånedsinntekt(
        @Schema(description = "Perioden inntekten gjelder for (format YYYY-MM)", example = "2023-01", type = "String", pattern = "YYYY-MM")
        val gjelderÅrMåned: YearMonth,
        @Schema(description = "Summert inntekt for måneden", example = "50000")
        val sumInntekt: BigDecimal,
        @Schema(description = "Liste over inntektsposter som utgjør grunnlaget for summert inntekt")
        val inntektPostListe: List<InntektPost>,
    )

    data class InntektPost(
        @Schema(description = "Kode for inntektspost", example = "bonus")
        val kode: String,
        @Schema(description = "Inntektstype inntektstposten er knyttet til", example = "PENSJON")
        @JsonAlias("inntekstype")
        val inntektstype: Inntektstype? = null,
        @Schema(description = "Beløp som utgjør inntektsposten", example = "60000")
        val beløp: BigDecimal,
    )
}

enum class InntektSkatteklasseType {
    INGEN,
    SKATTEKLASSE_0,
    SKATTEKLASSE_1,
    SKATTEKLASSE_2,
}

data class InntektSkattelement(
    val skattelementer: List<InntektSkattelementÅr> = emptyList(),
) : GrunnlagInnhold

data class InntektSkattelementÅr(
    val år: Int,
    val skatteklasse: InntektSkatteklasseType,
    val deltFordel: Boolean,
    val særfradragEnsligForsørger: Boolean,
    val særfradragDeltBosted: Boolean,
) : GrunnlagInnhold
