package no.nav.bidrag.transport.behandling.belopshistorikk.request

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import java.math.BigDecimal
import java.time.LocalDateTime

@Schema(description = "Egenskaper ved et engangsbeløp")
data class OpprettEngangsbeløpRequestDto(
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
    @Schema(description = "Periode gjort ugyldig av vedtak-id")
    val gjortUgyldigAvVedtaksid: Int?,
    @Schema(description = "Beregnet stønadsbeløp")
    val beløp: BigDecimal?,
    @Schema(description = "Allerede innbetalt beløp")
    val betaltBeløp: BigDecimal?,
    @Schema(description = "Valutakoden tilhørende stønadsbeløpet")
    val valutakode: String?,
    @Schema(description = "Resultatkoden tilhørende engangsbeløpet")
    val resultatkode: String,
    @Schema(description = "Angir om engangsbeløpet skal innkreves")
    val innkreving: Innkrevingstype,
    @Schema(
        description =
            "Referanse til engangsbeløp, brukes for å kunne omgjøre engangsbeløp senere i et klagevedtak. Unik innenfor et vedtak. " +
                "Unik referanse blir generert av bidrag-vedtak hvis den ikke er angitt i requesten.",
    )
    val referanse: String? = null,
    @Schema(description = "Opprettet av")
    val opprettetAv: String,
)
