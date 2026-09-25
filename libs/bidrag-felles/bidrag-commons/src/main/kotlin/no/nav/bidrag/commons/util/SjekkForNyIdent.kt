package no.nav.bidrag.commons.util

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.CorrelationId
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.ident.Ident
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.HentePersonidenterRequest
import no.nav.bidrag.transport.person.Identgruppe
import no.nav.bidrag.transport.person.PersonDto
import no.nav.bidrag.transport.person.PersonidentDto
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.CodeSignature
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.client.postForEntity

private val LOGGER = KotlinLogging.logger {}

@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
annotation class SjekkForNyIdent(
    vararg val parameterNavn: String,
)

@Aspect
@Component
class SjekkForNyIdentAspect(
    private val identConsumer: IdentConsumer,
) {
    /**
     * Denne metoden prosesserer de tilfellene hvor @SjekkForNyIdent brukes på en funksjon.
     * @SjekkForNyIdent benyttes i disse tilfellene med parameter tilhørende navnet på verdien som ønskes å sjekkes.
     * F.eks.
     * @SjekkForNyIdent("ident1", "ident2")
     * fun fus(ident1: String, ident2: String, ident2: String) {}
     */
    @Around("@annotation(sjekkForNyIdent)")
    fun prosseserNyIdent(
        joinPoint: ProceedingJoinPoint,
        sjekkForNyIdent: SjekkForNyIdent,
    ): Any? {
        val parametere = joinPoint.args
        val codeSignature = joinPoint.signature as CodeSignature
        val parametermap: Map<String, Any> = codeSignature.parameterNames.zip(parametere).toMap()

        for (parameterNavn in sjekkForNyIdent.parameterNavn) {
            when (val ident = parametermap[parameterNavn]) {
                is Personident -> {
                    if (ident.gyldig()) {
                        val parameterIndex = parametere.indexOf(ident)
                        parametere[parameterIndex] = Personident(identConsumer.sjekkIdent(ident.verdi))
                    }
                }

                is Ident -> {
                    if (ident.erPersonIdent()) {
                        val parameterIndex = parametere.indexOf(ident)
                        parametere[parameterIndex] = Ident(identConsumer.sjekkIdent(ident.verdi))
                    }
                }

                is String -> {
                    if (Personident(ident).gyldig()) {
                        val parameterIndex = parametere.indexOf(ident)
                        parametere[parameterIndex] = identConsumer.sjekkIdent(ident)
                    }
                }
            }
        }
        return joinPoint.proceed(parametere)
    }

    /**
     * Denne metoden prosesserer de tilfellene hvor @SjekkForNyIdent brukes på parameteret i en funksjon.
     * @SjekkForNyIdent benyttes i disse tilfellene uten parameter.
     * F.eks.
     * fun fus(@SjekkForNyIdent ident1: String, ident2: String) {}
     */
    @Around("execution(* *(.., @SjekkForNyIdent (*), ..))")
    fun prosseserNyIdent(joinPoint: ProceedingJoinPoint): Any? {
        val parametere = joinPoint.args
        val methodSignature = joinPoint.signature as MethodSignature

        for (i in parametere.indices) {
            when (val ident = parametere[i]) {
                is Personident -> {
                    if (harSjekkForNyIdentAnnotation(methodSignature.method.parameterAnnotations[i]) &&
                        ident.gyldig()
                    ) {
                        parametere[i] = Personident(identConsumer.sjekkIdent(ident.verdi))
                    }
                }

                is Ident -> {
                    if (harSjekkForNyIdentAnnotation(methodSignature.method.parameterAnnotations[i]) &&
                        ident.erPersonIdent()
                    ) {
                        parametere[i] = Ident(identConsumer.sjekkIdent(ident.verdi))
                    }
                }

                is String -> {
                    if (harSjekkForNyIdentAnnotation(methodSignature.method.parameterAnnotations[i]) &&
                        Personident(ident).gyldig()
                    ) {
                        parametere[i] = identConsumer.sjekkIdent(ident)
                    }
                }
            }
        }
        return joinPoint.proceed(parametere)
    }

    private fun harSjekkForNyIdentAnnotation(annotations: Array<Annotation>): Boolean = annotations.any { it is SjekkForNyIdent }
}

@Component
class IdentConsumer(
    @Value($$"${PERSON_URL:${BIDRAG_PERSON_URL}}") private val personUrl: String,
    @Qualifier("azure") private val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, $$"${NAIS_APP_NAME}") {
    companion object {
        const val PERSON_PATH = "/personidenter"
        const val INFORMASJON_PATH = "/informasjon"
    }

    @Cacheable(value = ["bidrag-commons_hentFødselsdato_cache"], key = "#ident")
    fun hentPersonInformasjon(ident: Personident): PersonDto? = try {
        restTemplate
            .postForEntity<PersonDto>(
                "$personUrl$INFORMASJON_PATH",
                PersonDto(ident),
            ).body
    } catch (e: NoSuchElementException) {
        LOGGER.warn(e) {
            "Bidrag-person fant ingen person på kalt ident. CallId: ${CorrelationId.fetchCorrelationIdForThread().sanitizeForLog()}."
        }
        null
    } catch (e: Exception) {
        LOGGER.error(e) {
            "Noe gikk galt i kall mot bidrag-person. CallId: ${CorrelationId.fetchCorrelationIdForThread().sanitizeForLog()}."
        }
        null
    }

    @Cacheable(value = ["bidrag-commons_hentAlleIdenter_cache"], key = "#ident")
    fun hentAlleIdenter(ident: String): List<String> {
        if (Ident(ident).erPersonIdent()) {
            return try {
                restTemplate
                    .postForEntity<Array<PersonidentDto>>(
                        "$personUrl$PERSON_PATH",
                        HentePersonidenterRequest(ident, setOf(Identgruppe.FOLKEREGISTERIDENT, Identgruppe.NPID), true),
                    ).body
                    ?.map { it.ident } ?: listOf(ident)
            } catch (e: NoSuchElementException) {
                LOGGER.warn(e) {
                    "Bidrag-person fant ingen person på kalt ident. CallId: ${CorrelationId.fetchCorrelationIdForThread().sanitizeForLog()}."
                }
                listOf(ident)
            } catch (e: Exception) {
                LOGGER.error(e) {
                    "Noe gikk galt i kall mot bidrag-person. CallId: ${CorrelationId.fetchCorrelationIdForThread().sanitizeForLog()}."
                }
                listOf(ident)
            }
        }
        return listOf(ident)
    }

    @Cacheable(value = ["bidrag-commons_sjekkIdent_cache"], key = "#ident")
    fun sjekkIdent(ident: String): String {
        if (Ident(ident).erPersonIdent()) {
            return try {
                restTemplate
                    .postForEntity<Array<PersonidentDto>>(
                        "$personUrl$PERSON_PATH",
                        HentePersonidenterRequest(ident, setOf(Identgruppe.FOLKEREGISTERIDENT), false),
                    ).body
                    ?.first()
                    ?.ident ?: ident
            } catch (e: NoSuchElementException) {
                LOGGER.warn(e) {
                    "Bidrag-person fant ingen person på kalt ident. CallId: ${CorrelationId.fetchCorrelationIdForThread().sanitizeForLog()}."
                }
                ident
            } catch (e: Exception) {
                LOGGER.error(e) {
                    "Noe gikk galt i kall mot bidrag-person. CallId: ${CorrelationId.fetchCorrelationIdForThread().sanitizeForLog()}."
                }
                ident
            }
        }
        return ident
    }
}
