package no.nav.bidrag.person.consumer

import com.netflix.graphql.dgs.client.CustomGraphQLClient
import com.netflix.graphql.dgs.client.DgsCustomGraphQLClient
import com.netflix.graphql.dgs.client.DgsGraphQLClient
import com.netflix.graphql.dgs.client.DgsGraphQLResponse
import com.netflix.graphql.dgs.client.GraphQLClient
import com.netflix.graphql.dgs.client.GraphQLResponse
import com.netflix.graphql.dgs.client.HttpResponse
import no.nav.bidrag.commons.cache.BrukerCacheable
import no.nav.bidrag.commons.security.SikkerhetsKontekst.medApplikasjonKontekst
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.person.BidragPerson.Companion.SECURE_LOGGER
import no.nav.bidrag.person.dto.ReasonToHttpStatus
import no.nav.bidrag.person.model.PdlException
import no.nav.bidrag.person.model.PersonIkkeFunnetException
import no.nav.bidrag.person.query.Bostedsadresse
import no.nav.bidrag.person.query.Criteria
import no.nav.bidrag.person.query.ForelderBarnRelasjonQuery
import no.nav.bidrag.person.query.ForelderBarnRelasjonResponse
import no.nav.bidrag.person.query.GeografiskTilknytningResponse
import no.nav.bidrag.person.query.GraphQuery
import no.nav.bidrag.person.query.HentIdenterQuery
import no.nav.bidrag.person.query.HentIdenterResponse
import no.nav.bidrag.person.query.HentPersonBostedsadresseResponse
import no.nav.bidrag.person.query.HusstandsmedlemmerQuery
import no.nav.bidrag.person.query.HusstandsmedlemmerResponseData
import no.nav.bidrag.person.query.NavnFødselsdatoDødsfallQuery
import no.nav.bidrag.person.query.NavnFødselsdatoDødsfallResponse
import no.nav.bidrag.person.query.Paging
import no.nav.bidrag.person.query.PersonAdresseQuery
import no.nav.bidrag.person.query.PersonAdresseResponse
import no.nav.bidrag.person.query.PersonBolkResponse
import no.nav.bidrag.person.query.PersonBostedsadresseQuery
import no.nav.bidrag.person.query.PersonFødsel
import no.nav.bidrag.person.query.PersonFødselBolkResponse
import no.nav.bidrag.person.query.PersonFødselsdatoerQuery
import no.nav.bidrag.person.query.PersonGeografiskTilknytningQuery
import no.nav.bidrag.person.query.PersonGradering
import no.nav.bidrag.person.query.PersonGraderingBolkResponse
import no.nav.bidrag.person.query.PersonGraderingerQuery
import no.nav.bidrag.person.query.PersonQuery
import no.nav.bidrag.person.query.PersonResponse
import no.nav.bidrag.person.query.PersondetaljerQuery
import no.nav.bidrag.person.query.PersondetaljerResponse
import no.nav.bidrag.person.query.SearchRule
import no.nav.bidrag.person.query.SivilstandQuery
import no.nav.bidrag.person.query.SivilstandResponse
import no.nav.bidrag.transport.person.ForelderBarnRelasjon
import no.nav.bidrag.transport.person.Husstandsmedlem
import no.nav.bidrag.transport.person.Identgruppe
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.exchange

@Component
class PDLConsumer(@param:Qualifier("pdl") restTemplate: HttpHeaderRestTemplate) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val graphQLClient: DgsGraphQLClient =
        DgsCustomGraphQLClient("") { _, _, body ->
            val exchange: ResponseEntity<String> = restTemplate.exchange("/graphql", HttpMethod.POST, HttpEntity(body))
            HttpResponse(exchange.statusCode.value(), exchange.body)
        }

    @Retryable(value = [Exception::class], backoff = Backoff(delay = 500))
    @BrukerCacheable(cacheNames = ["geografisk-tilknytning"], cacheManager = "cacheManagerPdl")
    fun hentGeografiskTilknytning(ident: Personident): GeografiskTilknytningResponse = consumeQuery(PersonGeografiskTilknytningQuery(ident))

    @Retryable(value = [Exception::class], backoff = Backoff(delay = 500))
    @BrukerCacheable(cacheNames = ["sivilstand"], cacheManager = "cacheManagerPdl")
    fun hentSivilstand(ident: Personident): SivilstandResponse = consumeQuery(SivilstandQuery(ident))

    @Retryable(value = [Exception::class], backoff = Backoff(delay = 500))
    @BrukerCacheable(cacheNames = ["personinfo"], cacheManager = "cacheManagerPdl")
    fun hentPersonInfo(ident: Personident): PersonResponse = consumeQuery(PersonQuery(ident))

    /* Denne metoden er med vilje ikke cached.
    Den brukes av bidrag-aktoerregister for å hente oppdateringer på personer fra PDL
    og må alltid returnere siste data fra PDL. */
    @Retryable(value = [Exception::class], backoff = Backoff(delay = 500))
    fun hentPersonDetaljer(ident: Personident): PersondetaljerResponse = consumeQuery(PersondetaljerQuery(ident.verdi))

    @Retryable(value = [Exception::class], backoff = Backoff(delay = 500))
    @BrukerCacheable(cacheNames = ["adresse"], cacheManager = "cacheManagerPdl")
    fun hentPersonAdresse(ident: Personident): PersonAdresseResponse = consumeQuery(PersonAdresseQuery(ident))

    @Retryable(value = [Exception::class], backoff = Backoff(delay = 500))
    @BrukerCacheable(cacheNames = ["forelder-barn-relasjon"], cacheManager = "cacheManagerPdl")
    fun hentForelderBarnRelasjoner(ident: Personident): List<ForelderBarnRelasjon> {
        secureLogger.debug { "Henter forelder barn relasjoner for person ${ident.verdi}" }
//        return consumeQuery<ForelderBarnRelasjonResponse>(ForelderBarnRelasjonQuery(ident)).hentForelderBarnRelasjon.forelderBarnRelasjon
        val respons = consumeQuery<ForelderBarnRelasjonResponse>(ForelderBarnRelasjonQuery(ident))
        secureLogger.debug { "Respons forelder barn-relasjoner for person ${ident.verdi}: $respons." }
        return respons.hentForelderBarnRelasjon.forelderBarnRelasjon
    }

    @Retryable(value = [Exception::class], backoff = Backoff(delay = 500))
    @BrukerCacheable(cacheNames = ["navn-foedsel-doed"], cacheManager = "cacheManagerPdl")
    fun hentNavnFødselsdatoDødsfall(ident: Personident): NavnFødselsdatoDødsfallResponse = consumeQuery(NavnFødselsdatoDødsfallQuery(ident))

    @Retryable(value = [Exception::class], backoff = Backoff(delay = 500))
    @BrukerCacheable(cacheNames = ["bostedsadresse"], cacheManager = "cacheManagerPdl")
    fun hentPersonBostedsadresse(ident: Personident): HentPersonBostedsadresseResponse = consumeQuery(PersonBostedsadresseQuery(ident))

    @Retryable(value = [Exception::class], backoff = Backoff(delay = 500))
    @BrukerCacheable(cacheNames = ["husstandsmedlemmer"], cacheManager = "cacheManagerPdl")
    fun hentHusstandsmedlemmer(pageNumber: Int, resultsPerPage: Int, bostedsadresse: Bostedsadresse): List<Husstandsmedlem> {
        val paging = Paging(pageNumber, resultsPerPage)
        val kriterier = ArrayList<Criteria>()

        if (bostedsadresse.vegadresse != null) {
            bostedsadresse.vegadresse.adressenavn?.let {
                kriterier.add(Criteria("person.bostedsadresse.vegadresse.adressenavn", SearchRule("equals", it)))
            }
            bostedsadresse.vegadresse.husnummer?.let {
                kriterier.add(Criteria("person.bostedsadresse.vegadresse.husnummer", SearchRule("equals", it)))
            }
            bostedsadresse.vegadresse.husbokstav?.let {
                kriterier.add(Criteria("person.bostedsadresse.vegadresse.husbokstav", SearchRule("equals", it)))
            }
            bostedsadresse.vegadresse.bruksenhetsnummer?.let {
                kriterier.add(Criteria("person.bostedsadresse.vegadresse.bruksenhetsnummer", SearchRule("equals", it)))
            }
            bostedsadresse.vegadresse.postnummer?.let {
                kriterier.add(Criteria("person.bostedsadresse.vegadresse.postnummer", SearchRule("equals", it)))
            }
            bostedsadresse.vegadresse.bydelsnummer?.let {
                kriterier.add(Criteria("person.bostedsadresse.vegadresse.bydelsnummer", SearchRule("equals", it)))
            }
            bostedsadresse.vegadresse.kommunenummer?.let {
                kriterier.add(Criteria("person.bostedsadresse.vegadresse.kommunenummer", SearchRule("equals", it)))
            }
            bostedsadresse.vegadresse.matrikkelId?.let {
                kriterier.add(Criteria("person.bostedsadresse.vegadresse.matrikkelId", SearchRule("equals", it.toString())))
            }
            bostedsadresse.vegadresse.tilleggsnavn?.let {
                kriterier.add(Criteria("person.bostedsadresse.vegadresse.tilleggsnavn", SearchRule("equals", it)))
            }
        }

        SECURE_LOGGER.debug("Husstandsmedlem pageNumber: {} kriterier: {}", pageNumber, kriterier)

        val response: HusstandsmedlemmerResponseData =
            consumeQuery(HusstandsmedlemmerQuery(paging, kriterier))

        return response.sokPerson.mapToHusstandsmedlemmer(bostedsadresse)
    }

    @Retryable(value = [Exception::class], backoff = Backoff(delay = 500))
    @BrukerCacheable(cacheNames = ["fødselsdatoer"], cacheManager = "cacheManagerPdl")
    fun hentFødselsdatoer(identer: Set<Personident>): Map<Personident, PersonFødsel> {
        val response: PersonFødselBolkResponse =
            medApplikasjonKontekst {
                consumeQuery(PersonFødselsdatoerQuery(identer))
            }
        return feilsjekkOgLagMap(response)
    }

    @Retryable(value = [Exception::class], backoff = Backoff(delay = 500))
    @BrukerCacheable(cacheNames = ["graderinger"], cacheManager = "cacheManagerPdl")
    fun hentGraderinger(identer: Set<Personident>): Map<Personident, PersonGradering> {
        val response: PersonGraderingBolkResponse = consumeQuery(PersonGraderingerQuery(identer))
        return feilsjekkOgLagMap(response)
    }

    @Retryable(value = [Exception::class], backoff = Backoff(delay = 500))
    @Cacheable(cacheNames = ["personidenter"], cacheManager = "cacheManagerPdl")
    fun hentePersonidenter(ident: String, identgrupper: Set<Identgruppe>, inkludereHistoriske: Boolean): HentIdenterResponse = consumeQuery(HentIdenterQuery(ident, identgrupper, inkludereHistoriske))

    private inline fun <reified T> feilsjekkOgLagMap(personBolkResponse: PersonBolkResponse<T>): Map<Personident, T> {
        val feil = personBolkResponse.personBolk.filter { it.code != "ok" }.associate { it.ident to it.code }
        if (feil.isNotEmpty()) {
            SECURE_LOGGER.error("Feil ved henting av ${T::class} fra PDL: $feil")
            val reasonToHttpStatus = ReasonToHttpStatus(feil.values.first())
            throw PdlException(
                "Feil ved henting av ${T::class} fra PDL: $feil. Se secure logg for detaljer.",
                reasonToHttpStatus.status,
            )
        }
        return personBolkResponse.personBolk.associateBy({ it.ident }, { it.person!! })
    }

    private inline fun <reified T> consumeQuery(query: GraphQuery): T {
        val response = executeQuery(query)
        if (response.hasErrors()) {
            val message = response.errors.first().message
            val errorReason = response.parsed.read<Any>("errors[0].extensions.code")
            val reasonToHttpStatus = ReasonToHttpStatus(errorReason)
            if (reasonToHttpStatus.status == HttpStatus.NOT_FOUND) {
                SECURE_LOGGER.debug("Fant ikke person med ident {} i PDL", query)
                throw PersonIkkeFunnetException(message)
            }
            SECURE_LOGGER.error("Feil ved henting av ${T::class} fra PDL: ${response.errors}. Query: $query")
            throw PdlException(message, reasonToHttpStatus.status)
        }
        SECURE_LOGGER.debug("Returnerer data for {}", T::class)
        return response.dataAsObject(T::class.java)
    }

    private fun executeQuery(query: GraphQuery): DgsGraphQLResponse = try {
        val queryString = query.getQuery()
        logger.debug("queryString: $queryString")
        graphQLClient.executeQuery(queryString, query.getVariables())
    } catch (exception: Exception) {
        val melding = "Teknisk feil ved spørring på PDL"
        logger.error(melding, exception)
        throw PdlException(melding, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    fun maskerFeil(feil: Map<String, String>): Map<String, String> = feil.mapKeys { entry -> entry.key.substring(0, 6) }
}
