package no.nav.bidrag.transport.behandling.beregning.felles

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import no.nav.bidrag.domene.ident.Personident
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = arrayOf(PersonidentLengdeValidator::class))
annotation class GyldigPersonidentLengde(
    val message: String = "Personident må inneholde 11 tegn",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Any>> = [],
)

class PersonidentLengdeValidator : ConstraintValidator<GyldigPersonidentLengde, Personident> {
    override fun isValid(
        personident: Personident?,
        constraintValidatorContext: ConstraintValidatorContext,
    ): Boolean {
        if (personident == null) {
            return false
        }
        return personident.verdi.length == 11
    }
}
