package no.nav.bidrag.transport.behandling.grunnlag.response

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class SkattegrunnlagDto(
    @Schema(description = "Id til personen inntekten er rapportert for")
    val personId: String,
    @Schema(description = "Periode fra")
    val periodeFra: LocalDate,
    @Schema(description = "Periode frem til")
    val periodeTil: LocalDate,
    @Schema(description = "Angir om en inntektsopplysning er aktiv")
    val aktiv: Boolean,
    @Schema(description = "Tidspunkt inntekten taes i bruk")
    val brukFra: LocalDateTime,
    @Schema(description = "Tidspunkt inntekten ikke lenger er aktiv som grunnlag. Null betyr at inntekten er aktiv")
    val brukTil: LocalDateTime?,
    @Schema(description = "Hentet tidspunkt")
    val hentetTidspunkt: LocalDateTime,
    @Schema(description = "Liste over poster med skattegrunnlag")
    val skattegrunnlagListe: List<SkattegrunnlagspostDto>,
)

data class SkattegrunnlagGrunnlagDto(
    @Schema(description = "Id til personen inntekten er rapportert for")
    val personId: String,
    @Schema(description = "Periode fra")
    val periodeFra: LocalDate,
    @Schema(description = "Periode frem til")
    val periodeTil: LocalDate,
    @Schema(description = "Liste over poster med skattegrunnlag")
    val skattegrunnlagspostListe: List<SkattegrunnlagspostDto>,
)

data class SkattegrunnlagspostDto(
    @Schema(description = "Type skattegrunnlag: Ordinær eller Svalbard")
    val skattegrunnlagType: String,
    @Schema(description = "Type inntekt: Lonnsinntekt, Naeringsinntekt, Pensjon eller trygd, Ytelse fra offentlig")
    @Deprecated("", replaceWith = ReplaceWith("kode"))
    val inntektType: String,
    @Schema(description = "Beløp")
    @Deprecated("", replaceWith = ReplaceWith("beløp"))
    val belop: BigDecimal,
    @Schema(description = "Beløp på skattegrunnlagposten")
    val beløp: BigDecimal = belop,
    @Schema(
        description =
            "Tekniske navnet på inntektsposten. Er samme verdi som \"Summert skattegrunnlag\"" +
                " fra NAV kodeverk ( https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/Summert%20skattegrunnlag )",
    )
    val kode: String = inntektType,
)
