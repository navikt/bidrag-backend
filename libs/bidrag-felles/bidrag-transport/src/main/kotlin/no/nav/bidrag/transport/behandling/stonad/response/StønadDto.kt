package no.nav.bidrag.transport.behandling.stonad.response

import com.fasterxml.jackson.annotation.JsonAlias
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import java.math.BigDecimal
import java.time.LocalDateTime

@Deprecated(
    "Flyttet til annen pakke",
    replaceWith = ReplaceWith("no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadDto"),
)
data class StønadDto(
    @Schema(description = "Stønadsid")
    val stønadsid: Int,
    @Schema(description = "Stønadstype")
    val type: Stønadstype,
    @Schema(description = "Referanse til sak")
    val sak: Saksnummer,
    @Schema(description = "Personidenten til den som skal betale bidraget")
    val skyldner: Personident,
    @Schema(description = "Personidenten til den som krever bidraget")
    val kravhaver: Personident,
    @Schema(description = "Personidenten til den som mottar bidraget")
    val mottaker: Personident,
    @Schema(description = "Angir første år en stønad skal indeksreguleres", deprecated = true)
    val førsteIndeksreguleringsår: Int?,
    @Schema(description = "Angir neste år siste perioden i stønaden skal indeksreguleres")
    @JsonAlias("førsteIndeksreguleringsår")
    val nesteIndeksreguleringsår: Int?,
    @Schema(description = "Angir om stønaden skal innkreves")
    val innkreving: Innkrevingstype,
    @Schema(description = "opprettet_av")
    val opprettetAv: String,
    @Schema(description = "opprettet tidspunkt")
    val opprettetTidspunkt: LocalDateTime,
    @Schema(description = "endret av")
    val endretAv: String?,
    @Schema(description = "når sist endret tidspunkt")
    val endretTidspunkt: LocalDateTime?,
    @Schema(description = "Liste over alle perioder som inngår i stønaden")
    val periodeListe: List<StønadPeriodeDto>,
)

@Deprecated(
    "Flyttet til annen pakke",
    replaceWith = ReplaceWith("no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadPeriodeDto"),
)
data class StønadPeriodeDto(
    @Schema(description = "Periodeid")
    val periodeid: Int,
    @Schema(description = "Periode med fra-og-med-dato og til-dato med format ÅÅÅÅ-MM")
    val periode: ÅrMånedsperiode,
    @Schema(description = "Stønadsid")
    val stønadsid: Int,
    @Schema(description = "Vedtaksid")
    val vedtaksid: Int,
    @Schema(description = "Perioden er gyldig fra angitt tidspunkt (vedtakstidspunkt)")
    val gyldigFra: LocalDateTime,
    @Schema(description = "Angir tidspunkt perioden eventuelt er ugyldig fra (tidspunkt for vedtak med periode som erstattet denne)")
    val gyldigTil: LocalDateTime?,
    @Schema(description = "Periode-gjort-ugyldig-av-vedtaksid")
    val periodeGjortUgyldigAvVedtaksid: Int?,
    @Schema(description = "Beregnet stønadsbeløp")
    val beløp: BigDecimal?,
    @Schema(description = "Valutakoden tilhørende stønadsbeløpet")
    val valutakode: String?,
    @Schema(description = "Resultatkode for stønaden")
    val resultatkode: String,
)
