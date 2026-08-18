package no.nav.bidrag.person.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.person.BidragPerson
import no.nav.bidrag.person.dto.HusstandsmedlemmerRequest
import no.nav.bidrag.person.model.BidragPersonFunctionalException
import no.nav.bidrag.person.service.PersonService
import no.nav.bidrag.transport.person.ForelderBarnRelasjonDto
import no.nav.bidrag.transport.person.Fødselsdatoer
import no.nav.bidrag.transport.person.GeografiskTilknytningDto
import no.nav.bidrag.transport.person.Graderingsinfo
import no.nav.bidrag.transport.person.HentePersonidenterRequest
import no.nav.bidrag.transport.person.HusstandsmedlemmerDto
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import no.nav.bidrag.transport.person.NavnFødselDødDto
import no.nav.bidrag.transport.person.PersonAdresseDto
import no.nav.bidrag.transport.person.PersonDto
import no.nav.bidrag.transport.person.PersonRequest
import no.nav.bidrag.transport.person.PersondetaljerDto
import no.nav.bidrag.transport.person.PersonidentDto
import no.nav.bidrag.transport.person.SivilstandPdlHistorikkDto
import no.nav.security.token.support.core.api.Protected
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
class PersonController(private val personService: PersonService) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/informasjon/detaljer")
    @Operation(
        description =
        "Hent informasjon om person. Dette innebærer fornavn/etternavn, " +
            "fødselsdato, adresse, gradering, språk, dødsdato, dødsbo og tidligere identer. " +
            "Dette endepunktet er ikke cached.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Person funnet"),
            ApiResponse(responseCode = "204", description = "Person ikke funnet"),
            ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
            ApiResponse(responseCode = "403", description = "Bruker mangler tilgang"),
            ApiResponse(responseCode = "500", description = "Ukjent feil"),
            ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig"),
        ],
    )
    fun hentPersoninformasjonDetaljer(@RequestBody personRequest: PersonRequest): PersondetaljerDto = personService.hentPersondetaljer(personRequest.ident)

    @PostMapping("fodselsdatoer")
    @Operation(description = "Hent fødselsdatoer for en liste med personer", security = [SecurityRequirement(name = "bearer-key")])
    fun hentFødselsdatoer(@RequestBody identer: Set<Personident>): Fødselsdatoer = personService.hentFødselsdatoer(identer)

    @PostMapping("graderingsinfo")
    @Operation(description = "Hent graderingsinfo for en liste med personer", security = [SecurityRequirement(name = "bearer-key")])
    fun hentGraderinger(@RequestBody identer: Set<Personident>): Graderingsinfo = personService.hentGraderinger(identer)

    @PostMapping("/geografisktilknytning")
    @Operation(
        description = "Hent informasjon om geografisk tilknytning for en person",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun hentGeografiskTilknytning(
        @Valid @RequestBody
        request: PersonRequest,
    ): GeografiskTilknytningDto = personService.hentGeografiskTilknytningData(request.ident)

    @PostMapping("/geografisk_tilknytning")
    @Operation(
        description = "Hent informasjon om geografisk tilknytning for en person",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun hentGeografiskTilknytning(@RequestBody personIdent: Personident): GeografiskTilknytningDto = personService.hentGeografiskTilknytningData(personIdent)

    @GetMapping("/geografisktilknytning/{ident}")
    @Operation(
        description = "Hent informasjon om geografisk tilknytning for en person",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @Deprecated(
        "På grunn av personvern - Bruk @PostMapping med @RequestBody PersonRequest i stedet",
        ReplaceWith("hentGeografiskTilknytning"),
    )
    fun getGeografiskTilknytning(@PathVariable ident: String): GeografiskTilknytningDto = hentGeografiskTilknytning(Personident(ident))

    @PostMapping("/sivilstand")
    @Operation(description = "Hent sivilstand for en person", security = [SecurityRequirement(name = "bearer-key")])
    fun hentSivilstand(
        @Valid @RequestBody
        request: PersonRequest,
    ): SivilstandPdlHistorikkDto = personService.hentSivilstand(request.ident)

    @PostMapping("/forelderbarnrelasjon")
    @Operation(description = "Hent alle forelder/barn-relasjoner for en person", security = [SecurityRequirement(name = "bearer-key")])
    fun hentForelderBarnRelasjon(
        @Valid @RequestBody
        request: PersonRequest,
    ): ForelderBarnRelasjonDto = personService.hentForelderBarnRelasjon(request.ident)

    @PostMapping("/forelderbarnrelasjoner")
    @Operation(description = "Hent alle forelder/barn-relasjoner for en person", security = [SecurityRequirement(name = "bearer-key")])
    fun hentForelderBarnRelasjon(@RequestBody personIdent: Personident): ForelderBarnRelasjonDto = personService.hentForelderBarnRelasjon(personIdent)

    @GetMapping("/forelderbarnrelasjon/{ident}")
    @Protected
    @Operation(description = "Hent alle forelder/barn-relasjoner for en person", security = [SecurityRequirement(name = "bearer-key")])
    @Deprecated(
        "På grunn av personvern - Bruk @PostMapping med @RequestBody PersonRequest i stedet",
        ReplaceWith("hentForelderBarnRelasjon"),
    )
    fun getForelderBarnRelasjon(@PathVariable ident: String): ForelderBarnRelasjonDto = hentForelderBarnRelasjon(Personident(ident))

    @PostMapping("/navnfoedseldoed")
    @Operation(
        description = "Hent informasjon om en persons navn, fødselsdata og eventuell død",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun hentNavnFoedselDoed(
        @Valid @RequestBody
        request: PersonRequest,
    ): NavnFødselDødDto = personService.hentNavnFoedselDoed(request.ident)

    @GetMapping("/navnfoedseldoed/{ident}")
    @Protected
    @Operation(
        description = "Hent informasjon om en persons navn, fødselsdata og eventuell død",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @Deprecated(
        "På grunn av personvern - Bruk @PostMapping med @RequestBody PersonRequest i stedet",
        ReplaceWith("hentNavnFoedselDoed"),
    )
    fun getNavnFoedselDoed(@PathVariable ident: String): NavnFødselDødDto = hentNavnFoedselDoed(PersonRequest(Personident(ident)))

    @PostMapping("/husstandsmedlemmer")
    @Operation(
        description = "Hent alle personer som bor i samme husstand som angitt person",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun hentHusstandsmedlemmer(
        @Valid @RequestBody
        request: HusstandsmedlemmerRequest,
    ): HusstandsmedlemmerDto = personService.hentHusstandsmedlemmer(request.personRequest.ident, request.periodeFra)

    @GetMapping("/husstandsmedlemmer/{ident}")
    @Protected
    @Operation(
        description = "Hent alle personer som bor i samme husstand som angitt person",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @Deprecated(
        "På grunn av personvern - Bruk @PostMapping med @RequestBody PersonRequest i stedet",
        ReplaceWith("""hentHusstandsmedlemmer"""),
    )
    fun getHusstandsmedlemmer(@PathVariable ident: String): HusstandsmedlemmerDto = hentHusstandsmedlemmer(HusstandsmedlemmerRequest(PersonRequest(Personident(ident)), null))

    @PostMapping("/husstandsmedlemskapbarn")
    @Operation(
        description = "Hent alle barn til angitt person som har delt bolig med personen og informasjon om de ulike periodene",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun hentHusstandsmedlemskapBarn(
        @Valid @RequestBody
        request: HusstandsmedlemmerRequest,
    ): HusstandsmedlemmerDto = personService.hentHusstandsmedlemskapEgneBarn(request.personRequest.ident, request.periodeFra)

    @GetMapping(value = [ENDPOINT_PERSON_INFO, "$ENDPOINT_PERSON_INFO/{ident}"])
    @Protected
    @Operation(description = "Hent informasjon om en person", security = [SecurityRequirement(name = "bearer-key")])
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Person funnet"),
            ApiResponse(responseCode = "204", description = "Person ikke funnet"),
            ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
            ApiResponse(responseCode = "403", description = "Bruker mangler tilgang"),
            ApiResponse(responseCode = "500", description = "Ukjent feil"),
            ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig"),
        ],
    )
    @Deprecated("På grunn av personvern - Bruk @PostMapping i med @RequestBody PersonRequest", ReplaceWith("PersonController.hentPerson"))
    fun hentPerson(@PathVariable ident: String): PersonDto = hentPersonPost(PersonRequest(Personident(ident)))

    @PostMapping(value = [ENDPOINT_PERSON_INFO, "$ENDPOINT_PERSON_INFO/"])
    @Protected
    @Operation(description = "Hent informasjon om en person", security = [SecurityRequirement(name = "bearer-key")])
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Person funnet"),
            ApiResponse(responseCode = "204", description = "Person ikke funnet"),
            ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
            ApiResponse(responseCode = "403", description = "Bruker mangler tilgang"),
            ApiResponse(responseCode = "500", description = "Ukjent feil"),
            ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig"),
        ],
    )
    fun hentPersonPost(@RequestBody request: @Valid PersonRequest): PersonDto {
        if (request.ident.verdi.isEmpty()) {
            throw BidragPersonFunctionalException(String.format("Kall til tjenesten %s mangler input personident", ENDPOINT_PERSON_INFO))
        }
        return personService.hentPersonInfo(request.ident)
    }

    @PostMapping(value = [ENDPOINT_MOTPART_BARN_INFO])
    @Protected
    @Operation(description = "Hent motpart-barn relasjon til en person", security = [SecurityRequirement(name = "bearer-key")])
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Person funnet"),
            ApiResponse(responseCode = "204", description = "Person ikke funnet"),
            ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
            ApiResponse(responseCode = "403", description = "Bruker mangler tilgang"),
            ApiResponse(responseCode = "500", description = "Ukjent feil"),
            ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig"),
        ],
    )
    fun getPersonensMotpartBarnRelasjon(@RequestBody request: @Valid PersonRequest): MotpartBarnRelasjonDto {
        if (request.ident.verdi.isEmpty()) {
            throw BidragPersonFunctionalException("Kall til tjenesten $ENDPOINT_MOTPART_BARN_INFO mangler input personident")
        }
        return personService.hentMotpartBarnRelasjon(request.ident)
    }

    @Deprecated("") // På grunn av personvern - Bruk @PostMapping i med @RequestBody PersonRequest
    @GetMapping("$ENDPOINT_PERSON_ADRESSE/{ident}")
    @Protected
    @Operation(description = "Hent postadresse for person", security = [SecurityRequirement(name = "bearer-key")])
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Postadresse for person funnet"),
            ApiResponse(responseCode = "204", description = "Person mangler adresse"),
            ApiResponse(responseCode = "404", description = "Fant ikke person"),
            ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
            ApiResponse(responseCode = "403", description = "Bruker mangler tilgang"),
            ApiResponse(responseCode = "500", description = "Ukjent feil"),
            ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig"),
        ],
    )
    fun hentPersonAdresse(
        @PathVariable ident: Personident,
    ): ResponseEntity<PersonAdresseDto> {
        BidragPerson.SECURE_LOGGER.debug("Henter adresse for ident {}", ident)
        val respons = personService.hentPersonPostadresse(ident)
        return ResponseEntity(respons, if (respons == null) HttpStatus.NO_CONTENT else HttpStatus.OK)
    }

    @PostMapping(path = [ENDPOINT_PERSON_ADRESSE])
    @Protected
    @Operation(
        description = "Henter registrerte adresser for person",
        parameters = [
            Parameter(name = "personident", required = true),
            Parameter(name = "hente-postadresse", description = "Settes til true for å hente postadresse til person. ", example = "true"),
        ],
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Adresse for person funnet"),
            ApiResponse(responseCode = "204", description = "Person mangler adresse"),
            ApiResponse(responseCode = "400", description = "Feil i requestobjekt"),
            ApiResponse(responseCode = "404", description = "Fant ikke person eller person mangler adresse"),
            ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
            ApiResponse(responseCode = "403", description = "Bruker mangler tilgang"),
            ApiResponse(responseCode = "500", description = "Ukjent feil"),
            ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig"),
        ],
    )
    fun hentPersonAdresser(@RequestBody request: @Valid PersonRequest): List<PersonAdresseDto> {
        BidragPerson.SECURE_LOGGER.debug("Henter registrerte adresser for person med ident {}", request)
        return personService.hentPersonAdresser(request.ident)
    }

    @PostMapping(path = ["$ENDPOINT_PERSON_ADRESSE/post"])
    @Protected
    @Operation(
        description = "Hent postadresse for person",
        parameters = [Parameter(name = "personident", required = true)],
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Adresse for person funnet"),
            ApiResponse(responseCode = "204", description = "Person mangler adresse"),
            ApiResponse(responseCode = "404", description = "Fant ikke person eller person mangler adresse"),
            ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler, er utøpt, eller av andre årsaker ugyldig"),
            ApiResponse(responseCode = "403", description = "Bruker mangler tilgang"),
            ApiResponse(responseCode = "500", description = "Ukjent feil"),
            ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig"),
        ],
    )
    fun hentPersonPostadresse(@RequestBody request: @Valid PersonRequest): ResponseEntity<PersonAdresseDto> {
        BidragPerson.SECURE_LOGGER.debug("Henter postadresse for person med ident {}", request)
        val respons = personService.hentPersonPostadresse(request.ident)
        return ResponseEntity(respons, if (respons == null) HttpStatus.NO_CONTENT else HttpStatus.OK)
    }

    @PostMapping(ENDPOINT_PERSON_SPRAAK)
    @Protected
    @Operation(
        description = "Henter personens språk fra Kontakt- og reservasjonsregisteret",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Fant personens språk i KRR"),
            ApiResponse(responseCode = "204", description = "Personen har ikke registrert språk i KRR"),
            ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
            ApiResponse(responseCode = "403", description = "Bruker mangler tilgang"),
            ApiResponse(responseCode = "404", description = "Person ikke funnet"),
            ApiResponse(responseCode = "500", description = "Ukjent feil"),
            ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig"),
        ],
    )
    fun hentPersonSpraak(@RequestBody request: @Valid PersonRequest): ResponseEntity<String> {
        BidragPerson.SECURE_LOGGER.debug("Henter registrert språk fra Kontakt- og reservarsjonsregisteret for person med ident {}", request)

        // Tilgangskontroll, kaster 403 hvis ikke tilgang. Denne sjekken kan fjernes når alle konsumenter bruker Azure-onBehalfOf-token
        personService.hentPersonInfo(request.ident)
        val respons = personService.hentPersonSpraak(request.ident)
        return ResponseEntity(respons, if (respons == null) HttpStatus.NO_CONTENT else HttpStatus.OK)
    }

    @PostMapping(ENDPOINT_PERSONIDENTER)
    @Operation(description = "Henter alle identer som er registrert for en person", security = [SecurityRequirement(name = "bearer-key")])
    fun hentePersonidenter(
        @Valid @RequestBody
        request: HentePersonidenterRequest,
    ): List<PersonidentDto> {
        BidragPerson.SECURE_LOGGER.debug(
            "Henter personidenter for ident ${request.ident} med identgrupper ${request.grupper} " +
                "Flagg for inkludering av historiske identer er satt til ${request.inkludereHistoriske}.",
        )
        return personService.hentePersonidenter(request.ident, request.inkludereHistoriske, request.grupper)
    }

    companion object {
        const val ENDPOINT_PERSON_INFO = "/informasjon"
        const val ENDPOINT_MOTPART_BARN_INFO = "/motpartbarnrelasjon"
        const val ENDPOINT_PERSON_ADRESSE = "/adresse"
        const val ENDPOINT_PERSON_SPRAAK = "/spraak"
        const val ENDPOINT_PERSONIDENTER = "/personidenter"
    }
}
