package no.nav.bidrag.transport.behandling.inntekt.response

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.inntekt.Inntektstype
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.felles.grunnlag.Grunnlagsreferanse
import java.math.BigDecimal
import java.time.YearMonth

data class TransformerInntekterResponse(
    @Schema(description = "Dato + commit hash", example = "20230705081501_68e71c7")
    val versjon: String = "",
    @Schema(description = "Liste over summerte månedsinntekter (Ainntekt ++))")
    val summertMånedsinntektListe: List<SummertMånedsinntekt> = emptyList(),
    @Schema(description = "Liste over summerte årsinntekter (Ainntekt + Sigrun ++)")
    val summertÅrsinntektListe: List<SummertÅrsinntekt> = emptyList(),
)

data class SummertMånedsinntekt(
    @Schema(description = "Perioden inntekten gjelder for (format YYYY-MM)", example = "2023-01", type = "String", pattern = "YYYY-MM")
    val gjelderÅrMåned: YearMonth,
    @Schema(description = "Summert inntekt for måneden", example = "50000")
    val sumInntekt: BigDecimal,
    @Schema(description = "Liste over inntektsposter som utgjør grunnlaget for summert inntekt")
    val inntektPostListe: List<InntektPost>,
    val grunnlagsreferanseListe: List<Grunnlagsreferanse> = emptyList(),
)

data class SummertÅrsinntekt(
    @Schema(description = "Type inntektrapportering", example = "AINNTEKT")
    val inntektRapportering: Inntektsrapportering,
    @Schema(description = "Summert inntekt for perioden, omgjort til årsinntekt", example = "600000")
    val sumInntekt: BigDecimal,
    @Schema(description = "Perioden inntekten gjelder for (fom-til)")
    val periode: ÅrMånedsperiode,
    @Schema(description = "Id til barnet inntekten mottas for, brukes for kontantstøtte og barnetillegg")
    val gjelderBarnPersonId: String = "",
    @Schema(description = "Liste over inntektsposter (generisk, avhengig av type) som utgjør grunnlaget for summert inntekt")
    val inntektPostListe: List<InntektPost> = emptyList(),
    val grunnlagsreferanseListe: List<Grunnlagsreferanse> = emptyList(),
)

data class InntektPost(
    @Schema(description = "Kode for inntektspost", example = "bonus")
    val kode: String,
    @Schema(description = "Inntekstype inntekstposten er knyttet til", example = "PENSJON")
    val inntekstype: Inntektstype? = null,
    @Schema(description = "Beløp som utgjør inntektsposten", example = "60000")
    val beløp: BigDecimal,
)
