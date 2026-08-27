package no.nav.bidrag.transport.behandling.beregning.felles

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningSumInntekt
import no.nav.bidrag.transport.person.PersonStønad
import java.math.BigDecimal
import java.time.YearMonth

@Schema(description = "Grunnlag for kategorisering, gruppering og beregning av inntekter")
data class BeregnValgteInntekterGrunnlag(
    @get:Schema(description = "Beregningsperiode") val periode: ÅrMånedsperiode,
    @get:Schema(description = "Dato hvis siste periode skal opphøres") val opphørsdato: YearMonth? = null,
    @get:Schema(description = "Liste over barn som det skal beregnes for") val barnListe: List<PersonStønad>,
    @get:Schema(description = "Ident til person som inntekter skal beregnes for") val gjelderIdent: Personident,
    @get:Schema(description = "Periodisert liste over inntekter") val grunnlagListe: List<InntektsgrunnlagPeriode> = emptyList(),
) {
    val opphørSistePeriode: Boolean get() = opphørsdato != null
}

data class InntektsgrunnlagPeriode(
    @get:Schema(description = "Perioden inntekten gjelder for") val periode: ÅrMånedsperiode,
    @get:Schema(description = "Type inntektsrapportering") val inntektsrapportering: Inntektsrapportering,
    @get:Schema(description = "Barnet inntekten gjelder for") val inntektGjelderBarn: PersonStønad? = null,
    @get:Schema(description = "Inntekt beløp") val beløp: BigDecimal,
    @get:Schema(description = "Ident til den som eier inntekten") val inntektEiesAvIdent: Personident,
)

@Schema(description = "Resultatet av kategorisering, gruppering og beregning av inntekter")
data class BeregnValgteInntekterResultat(
    @get:Schema(description = "Liste over inntekter per barn") val inntektPerBarnListe: List<InntektPerBarn> = emptyList(),
)

@Schema(description = "Periodisert inntekt per barn")
data class InntektPerBarn(
    @get:Schema(description = "Barnet inntekten gjelder for") val inntektGjelderBarn: PersonStønad? = null,
    @get:Schema(description = "Liste over summerte inntektsperioder") var summertInntektListe: List<DelberegningSumInntekt> = emptyList(),
)
