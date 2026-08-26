package no.nav.bidrag.transport.behandling.beregning.felles

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import java.time.LocalDate
import kotlin.reflect.KClass

data class BidragBeregningRequestDto(
    @field:Valid
    val hentBeregningerFor: List<HentBidragBeregning>,
) {
    @GyldigBidragBeregningRequest
    @Schema(
        description =
            "Hent bidrag beregning. Enten datoSøknad eller søknadsid må settes. " +
                "Hvis søknadsid settes så vil mottattDato på søknaden benyttes som datoSøknad.",
    )
    data class HentBidragBeregning(
        @field:NotBlank(message = "Saksnummer kan ikke være blank")
        @field:Size(max = 7, min = 7, message = "Saksnummer må bestå av 7 tegn")
        val saksnummer: String,
        @field:GyldigPersonidentLengde
        val personidentBarn: Personident,
        val datoSøknad: LocalDate? = null,
        val søknadsid: String? = null,
        val stønadstype: Stønadstype,
    )
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [GyldigBidragBeregningValidator::class])
annotation class GyldigBidragBeregningRequest(
    val message: String = "Enten datoSøknad eller søknadsid må settes",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Any>> = [],
)

class GyldigBidragBeregningValidator : ConstraintValidator<GyldigBidragBeregningRequest, BidragBeregningRequestDto.HentBidragBeregning> {
    override fun isValid(
        input: BidragBeregningRequestDto.HentBidragBeregning?,
        constraintValidatorContext: ConstraintValidatorContext,
    ): Boolean {
        if (input == null) {
            return false
        }
        return input.datoSøknad != null || !input.søknadsid.isNullOrEmpty()
    }
}
