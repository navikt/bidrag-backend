package no.nav.bidrag.transport.behandling.felles.grunnlag

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.privatavtale.PrivatAvtaleType
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import java.math.BigDecimal
import java.time.LocalDate

@Deprecated(
    "Skal erstattes av PrivatAvtaleGrunnlagV2",
    ReplaceWith("PrivatAvtaleGrunnlagV2"),
)
data class PrivatAvtaleGrunnlag(
    val avtaleInngåttDato: LocalDate,
    val avtaleType: PrivatAvtaleType = PrivatAvtaleType.PRIVAT_AVTALE,
    val skalIndeksreguleres: Boolean,
    val utenlandskbidrag: Boolean = false,
) : GrunnlagInnhold

data class PrivatAvtalePeriodeGrunnlag(
    override val periode: ÅrMånedsperiode,
    val beløp: BigDecimal,
    val samværsklasse: Samværsklasse? = null,
    val valutakode: Valutakode? = null,
    override val manueltRegistrert: Boolean = true,
) : GrunnlagPeriodeInnhold

@Deprecated(
    "Skal erstattes av DelberegningIndeksreguleringPrivatAvtaleV2",
    ReplaceWith("DelberegningIndeksreguleringPrivatAvtaleV2"),
)
data class DelberegningPrivatAvtale(
    val nesteIndeksreguleringsår: BigDecimal? = null,
    val perioder: List<DelberegningPrivatAvtalePeriode>,
) : DelberegningUtenPeriode

@Deprecated(
    "Skal erstattes av DelberegningIndeksreguleringPrivatAvtaleV2",
    ReplaceWith("DelberegningIndeksreguleringPrivatAvtaleV2"),
)
data class DelberegningPrivatAvtalePeriode(
    override val periode: ÅrMånedsperiode,
    val indeksreguleringFaktor: BigDecimal? = null,
    val beløp: BigDecimal,
) : Delberegning

@Schema(description = "Privat avtale i bidragssaken")
data class PrivatAvtaleGrunnlagV2(
    val avtaleInngåttDato: LocalDate,
    val avtaleType: PrivatAvtaleType = PrivatAvtaleType.PRIVAT_AVTALE,
    val stønadstype: Stønadstype = Stønadstype.BIDRAG,
    val skalIndeksreguleres: Boolean,
    val sakskategori: Sakskategori = Sakskategori.NASJONAL,
    @Schema(description = "Vedtaksid når vedtak fra NAV er valgt som avtaletype")
    val vedtaksid: Int? = null,
) : GrunnlagInnhold

data class DelberegningIndeksreguleringPrivatAvtale(
    override val periode: ÅrMånedsperiode,
    val nesteIndeksreguleringsår: BigDecimal? = null,
    val indeksreguleringFaktor: BigDecimal? = null,
    val valutakode: Valutakode = Valutakode.NOK,
    val indeksregulertBeløp: BigDecimal,
) : Delberegning

data class DelberegningBidragTilFordelingPrivatAvtale(
    override val periode: ÅrMånedsperiode,
    val valutakode: Valutakode = Valutakode.NOK,
    @Schema(description = "Indeksregulert beløp i valuta")
    val indeksregulertBeløp: BigDecimal,
    @Schema(description = "Samværsfradrag i valuta")
    val samværsfradrag: BigDecimal? = null,
    @Schema(description = "Bidrag til fordeling i valuta")
    val bidragTilFordeling: BigDecimal,
    @Schema(description = "Bidrag til fordeling i NOK")
    val bidragTilFordelingNOK: BigDecimal = bidragTilFordeling,
    @Schema(description = "Er dette et norsk bidrag?")
    val erNorskBidrag: Boolean = true,
) : Delberegning
