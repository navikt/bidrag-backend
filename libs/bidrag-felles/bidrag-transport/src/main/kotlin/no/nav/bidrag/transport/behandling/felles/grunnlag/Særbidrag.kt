@file:Suppress("ktlint:standard:filename")

package no.nav.bidrag.transport.behandling.felles.grunnlag

import com.fasterxml.jackson.annotation.JsonAlias
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.særbidrag.Særbidragskategori
import java.math.BigDecimal
import java.time.LocalDate

data class SærbidragskategoriGrunnlag(
    val kategori: Særbidragskategori,
    @get:Schema(
        description =
            "Beskrivelse på kategorien. Er påkrevd hvis kategori er ANNET",
    )
    val beskrivelse: String? = null,
) : GrunnlagInnhold

data class UtgiftspostGrunnlag(
    val dato: LocalDate,
    val type: String,
    val kravbeløp: BigDecimal,
    val godkjentBeløp: BigDecimal,
    @Schema(
        description =
            "Kommentar kan brukes til å legge inn nærmere informasjon om utgiften " +
                "f.eks. fakturanr., butikk det er handlet i," +
                " informasjon om hvorfor man ikke har godkjent hele kravbeløpet",
    )
    @JsonAlias("kommentar", "begrunnelse")
    val kommentar: String? = null,
    val betaltAvBp: Boolean = false,
) : GrunnlagInnhold

data class UtgiftDirekteBetaltGrunnlag(
    @Schema(description = "Beløp som er overført direkte til BM. Kan være 0 eller høyere")
    val beløpDirekteBetalt: BigDecimal,
) : GrunnlagInnhold

data class UtgiftMaksGodkjentBeløpGrunnlag(
    @Schema(
        description =
            "Maks godkjent beløp som settes manuelt og som legger på maksimalgrense for godkjent beløp." +
                " Kan være lik eller lavere enn total godkjent beløp",
    )
    val beløp: BigDecimal,
    @Schema(description = "Begrunnelse på hvorfor maks godkjent beløp er satt")
    val begrunnelse: String,
) : GrunnlagInnhold
