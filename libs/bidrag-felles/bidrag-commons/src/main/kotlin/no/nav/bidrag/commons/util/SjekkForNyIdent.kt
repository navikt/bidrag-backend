package no.nav.bidrag.commons.util

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.domene.ident.Ident
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.Identgruppe
import no.nav.bidrag.transport.person.PersonDto
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.CodeSignature
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestOperations

private val LOGGER = KotlinLogging.logger {}

/**
 * Erstatter innsendt personident med gjeldende folkeregisterident fra bidrag-person.
 * Ved manglende treff (404, 204 eller tom identliste) beholdes innsendt ident som standard.
 * Andre feil kastes videre med mindre [ignorerFeil] er aktivert.
 *
 * @param parameterNavn Navn på parameterne som skal sjekkes når annotasjonen står på en funksjon.
 * Utelates når annotasjonen står direkte på en parameter.
 * @param feilHvisIkkeFunnet Kast [HttpClientErrorException] med status 404 ved manglende treff,
 * i stedet for å beholde innsendt ident.
 * @param ignorerFeil Behold innsendt ident ved alle oppslagsfeil og logg på debug-nivå.
 * Overstyrer [feilHvisIkkeFunnet]. Feil fra den annoterte funksjonen kastes fortsatt videre.
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
annotation class SjekkForNyIdent(
    vararg val parameterNavn: String,
    val feilHvisIkkeFunnet: Boolean = false,
    val ignorerFeil: Boolean = false,
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
                        parametere[parameterIndex] = Personident(sjekkIdent(ident.verdi, sjekkForNyIdent, joinPoint))
                    }
                }

                is Ident -> {
                    if (ident.erPersonIdent()) {
                        val parameterIndex = parametere.indexOf(ident)
                        parametere[parameterIndex] = Ident(sjekkIdent(ident.verdi, sjekkForNyIdent, joinPoint))
                    }
                }

                is String -> {
                    if (Personident(ident).gyldig()) {
                        val parameterIndex = parametere.indexOf(ident)
                        parametere[parameterIndex] = sjekkIdent(ident, sjekkForNyIdent, joinPoint)
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
            val sjekkForNyIdent = methodSignature.method.parameterAnnotations[i]
                .filterIsInstance<SjekkForNyIdent>()
                .firstOrNull() ?: continue
            when (val ident = parametere[i]) {
                is Personident -> {
                    if (ident.gyldig()) {
                        parametere[i] = Personident(sjekkIdent(ident.verdi, sjekkForNyIdent, joinPoint))
                    }
                }

                is Ident -> {
                    if (ident.erPersonIdent()) {
                        parametere[i] = Ident(sjekkIdent(ident.verdi, sjekkForNyIdent, joinPoint))
                    }
                }

                is String -> {
                    if (Personident(ident).gyldig()) {
                        parametere[i] = sjekkIdent(ident, sjekkForNyIdent, joinPoint)
                    }
                }
            }
        }
        return joinPoint.proceed(parametere)
    }

    private fun sjekkIdent(
        ident: String,
        sjekkForNyIdent: SjekkForNyIdent,
        joinPoint: ProceedingJoinPoint,
    ): String = try {
        identConsumer.sjekkIdent(ident) ?: if (sjekkForNyIdent.feilHvisIkkeFunnet && !sjekkForNyIdent.ignorerFeil) {
            throw HttpClientErrorException(HttpStatus.NOT_FOUND, "Fant ingen gjeldende folkeregisterident")
        } else {
            ident
        }
    } catch (e: Exception) {
        if (!sjekkForNyIdent.ignorerFeil) throw e
        LOGGER.debug {
            "Oppslag etter gjeldende folkeregisterident feilet i ${joinPoint.signature.toShortString()}. Beholder innsendt ident fordi ignorerFeil er aktivert."
        }
        ident
    }
}

@Component
class IdentConsumer(
    @Value($$"${PERSON_URL:${BIDRAG_PERSON_URL}}") private val personUrl: String,
    @Qualifier("azure") private val restTemplate: RestOperations,
) {
    companion object {
        const val PERSON_PATH = "/personidenter"
        const val INFORMASJON_PATH = "/informasjon"
    }

    private val bidragPersonOppslagClient = BidragPersonOppslagClient(personUrl, restTemplate)

    /**
     * Returnerer null når bidrag-person ikke finner personen. Andre feil kastes videre.
     * Bare vellykkede oppslag caches.
     */
    @Cacheable(value = ["bidrag-commons_hentFødselsdato_cache"], key = "#ident", unless = "#result == null")
    fun hentPersonInformasjon(ident: Personident): PersonDto? = bidragPersonOppslagClient.hentPersonInformasjon(ident)

    /**
     * Henter alle identer (inkludert historiske) for en person fra bidrag-person.
     * Returnerer null når bidrag-person ikke finner personen. Andre feil kastes videre.
     * Bare vellykkede oppslag caches.
     */
    @Cacheable(value = ["bidrag-commons_hentAlleIdenter_cache"], key = "#ident", unless = "#result == null")
    fun hentAlleIdenter(ident: String): List<String>? {
        if (Ident(ident).erPersonIdent()) {
            return bidragPersonOppslagClient
                .hentPersonidenter(ident, setOf(Identgruppe.FOLKEREGISTERIDENT, Identgruppe.NPID), true)
                ?.map { it.ident }
        }
        return listOf(ident)
    }

    /**
     * Henter gjeldende folkeregisterident for en person fra bidrag-person.
     * Returnerer null når bidrag-person ikke finner personen. Andre feil kastes videre.
     * Bare vellykkede oppslag caches.
     */
    @Cacheable(value = ["bidrag-commons_sjekkIdent_cache"], key = "#ident", unless = "#result == null")
    fun sjekkIdent(ident: String): String? {
        if (Ident(ident).erPersonIdent()) {
            return bidragPersonOppslagClient
                .hentPersonidenter(ident, setOf(Identgruppe.FOLKEREGISTERIDENT), false)
                ?.firstOrNull()
                ?.ident
        }
        return ident
    }
}
