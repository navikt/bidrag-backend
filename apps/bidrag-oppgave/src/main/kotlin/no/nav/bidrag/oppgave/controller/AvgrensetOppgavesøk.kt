package no.nav.bidrag.oppgave.controller

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [AvgrensetOppgavesøkValidator::class])
annotation class AvgrensetOppgavesøk(
    val message: String = "Oppgi saksnummer, aktør-ID, saksbehandler eller enhetsnummer",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class AvgrensetOppgavesøkValidator : ConstraintValidator<AvgrensetOppgavesøk, FinnOppgaverRequest> {
    override fun isValid(request: FinnOppgaverRequest, context: ConstraintValidatorContext): Boolean = !request.saksnummer.isNullOrBlank() ||
        !request.aktoerId?.verdi.isNullOrBlank() ||
        !request.saksbehandler?.verdi.isNullOrBlank() ||
        !request.enhetsnummer?.verdi.isNullOrBlank()
}
