package no.nav.bidrag.transport.behandling.felles.grunnlag

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.bidrag.domene.enums.sjablon.SjablonTallNavn
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import java.math.BigDecimal

data class SjablonSjablontallPeriode(
    override val periode: ÅrMånedsperiode,
    @JsonIgnore
    override val manueltRegistrert: Boolean = false,
    val sjablon: SjablonTallNavn,
    val verdi: BigDecimal,
) : GrunnlagPeriodeInnhold

data class SjablonSamværsfradragPeriode(
    override val periode: ÅrMånedsperiode,
    @JsonIgnore
    override val manueltRegistrert: Boolean = false,
    val samværsklasse: String,
    val alderTom: Int,
    val antallDagerTom: Int,
    val antallNetterTom: Int,
    val beløpFradrag: BigDecimal,
) : GrunnlagPeriodeInnhold

data class SjablonBidragsevnePeriode(
    override val periode: ÅrMånedsperiode,
    @JsonIgnore
    override val manueltRegistrert: Boolean = false,
    val bostatus: String = "",
    val boutgiftBeløp: BigDecimal,
    val underholdBeløp: BigDecimal,
) : GrunnlagPeriodeInnhold

data class SjablonTrinnvisSkattesatsPeriode(
    override val periode: ÅrMånedsperiode,
    @JsonIgnore
    override val manueltRegistrert: Boolean = false,
    val trinnliste: List<SjablonTrinnvisSkattesats>,
) : GrunnlagPeriodeInnhold

data class SjablonTrinnvisSkattesats(
    val inntektsgrense: Int,
    val sats: BigDecimal,
)

data class SjablonBarnetilsynPeriode(
    override val periode: ÅrMånedsperiode,
    @JsonIgnore
    override val manueltRegistrert: Boolean = false,
    val typeStønad: String,
    val typeTilsyn: String,
    val beløpBarnetilsyn: BigDecimal,
) : GrunnlagPeriodeInnhold

data class SjablonForbruksutgifterPeriode(
    override val periode: ÅrMånedsperiode,
    @JsonIgnore
    override val manueltRegistrert: Boolean = false,
    val alderTom: Int,
    val beløpForbruk: BigDecimal,
) : GrunnlagPeriodeInnhold

data class SjablonMaksFradragPeriode(
    override val periode: ÅrMånedsperiode,
    @JsonIgnore
    override val manueltRegistrert: Boolean = false,
    val antallBarnTom: Int,
    val maksBeløpFradrag: BigDecimal,
) : GrunnlagPeriodeInnhold

data class SjablonMaksTilsynPeriode(
    override val periode: ÅrMånedsperiode,
    @JsonIgnore
    override val manueltRegistrert: Boolean = false,
    val antallBarnTom: Int,
    val maksBeløpTilsyn: BigDecimal,
) : GrunnlagPeriodeInnhold
