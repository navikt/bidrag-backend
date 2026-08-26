package no.nav.bidrag.transport.behandling.belopshistorikk.response

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.Column
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import java.math.BigDecimal
import java.time.LocalDateTime

data class EngangsbeløpDto(
    @Schema(description = "Engangsbeløpsid")
    val engangsbeløpsid: Int,
    @Schema(description = "Engangsbeløptype")
    val type: Engangsbeløptype,
    @Schema(description = "Referanse til sak")
    val sak: Saksnummer,
    @Schema(description = "Personidenten til den som skal betale engangsbeløpet")
    val skyldner: Personident,
    @Schema(description = "Personidenten til den som krever engangsbeløpet")
    val kravhaver: Personident,
    @Schema(description = "Personidenten til den som mottar engangsbeløpet")
    val mottaker: Personident,
    @Schema(description = "Vedtaksid")
    val vedtaksid: Int,
    @Schema(description = "Perioden er gyldig fra angitt tidspunkt (vedtakstidspunkt)")
    val gyldigFra: LocalDateTime,
    @Schema(description = "Angir tidspunkt perioden eventuelt er ugyldig fra (tidspunkt for vedtak med periode som erstattet denne)")
    val gyldigTil: LocalDateTime?,
    @Schema(description = "Periode-gjort-ugyldig-av-vedtaksid")
    val gjortUgyldigAvVedtaksid: Int?,
    @Schema(description = "Beregnet engangsbeløp")
    @Min(0)
    val beløp: BigDecimal?,
    @Schema(description = "Beløp BP allerede har betalt")
    @Min(0)
    val betaltBeløp: BigDecimal? = null,
    @Schema(description = "Valutakoden tilhørende engangsbeløpet")
    @NotBlank
    val valutakode: String?,
    @Schema(description = "Resultatkoden tilhørende engangsbeløpet")
    @NotBlank
    val resultatkode: String,
    @Schema(description = "Angir om engangsbeløpet skal innkreves")
    val innkreving: Innkrevingstype,
    @Schema(
        description =
            "Referanse til engangsbeløp, brukes for å kunne omgjøre engangsbeløp senere i et klagevedtak. Unik innenfor et vedtak." +
                "Referansen er enten angitt i requesten for opprettelse av vedtak " +
                "eller generert av bidrag-vedtak hvis den ikke var angitt i requesten.",
    )
    val referanse: String,
    @Column(nullable = false, name = "opprettet_av")
    val opprettetAv: String = "",
    @Column(nullable = false, name = "opprettet_tidspunkt")
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
    @Column(nullable = true, name = "endret_av")
    val endretAv: String? = null,
    @Column(nullable = true, name = "endret_tidspunkt")
    val endretTidspunkt: LocalDateTime? = null,
)
