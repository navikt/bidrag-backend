package no.nav.bidrag.reskontro.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.domene.enums.regnskap.Transaksjonskode
import no.nav.bidrag.reskontro.service.ReskontroService
import no.nav.bidrag.transport.person.PersonRequest
import no.nav.bidrag.transport.reskontro.TransaksjonskodeDto
import no.nav.bidrag.transport.reskontro.request.EndreRmForSakRequest
import no.nav.bidrag.transport.reskontro.request.SaksnummerRequest
import no.nav.bidrag.transport.reskontro.response.innkrevingssak.BidragssakDto
import no.nav.bidrag.transport.reskontro.response.innkrevingssak.BidragssakMedSkyldnerDto
import no.nav.bidrag.transport.reskontro.response.innkrevingssaksinformasjon.InnkrevingssaksinformasjonDto
import no.nav.bidrag.transport.reskontro.response.transaksjoner.TransaksjonerDto
import no.nav.bidrag.transport.reskontro.tilDto
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
@Tag(
    name = "Reskontro",
    description = "Endepunkter for oppslag og administrasjon av innkrevingssaker og transaksjoner mot Skatteetatens reskontro (ELIN).",
)
class ReskontroController(
    private val reskontroService: ReskontroService,
) {
    @PostMapping("/innkrevningssak/bidragssak")
    @Operation(
        summary = "Hent innkrevingssak på bidragssak",
        description = "Henter innkrevingssaksinformasjon fra ELIN for én enkelt bidragssak identifisert med saksnummer. " +
            "Returnerer gjeldsinformasjon per barn i saken, inkludert rest gjeld offentlig og privat.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Innkrevingssaksinformasjon ble funnet og returneres.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = BidragssakDto::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Ugyldig forespørsel – f.eks. manglende eller feil format på saksnummer.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Ingen innkrevingssak funnet for oppgitt saksnummer.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "500",
                description = "Uventet feil på server.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "504",
                description = "Tidsavbrudd (Gateway Timeout) mot Skatteetatens system (ELIN).",
                content = [Content()],
            ),
        ],
    )
    fun hentInnkrevingssakPåBidragssak(@RequestBody saksnummerRequest: SaksnummerRequest): ResponseEntity<BidragssakDto> {
        val innkrevingssakPåSak = reskontroService.hentInnkrevingssakPåSak(saksnummerRequest)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(innkrevingssakPåSak)
    }

    @PostMapping("/innkrevningssak/person")
    @Operation(
        summary = "Hent innkrevingssaker på person",
        description = "Henter alle innkrevingssaker fra ELIN som er knyttet til en persons fødselsnummer eller D-nummer. " +
            "Returnerer skyldnerinformasjon samt gjeldsinformasjon per bidragssak og per barn i saken.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Innkrevingssaker med skyldnerinformasjon ble funnet og returneres.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = BidragssakMedSkyldnerDto::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Ugyldig forespørsel – f.eks. manglende eller feil format på personident.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Ingen innkrevingssaker funnet for oppgitt personident.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "500",
                description = "Uventet feil på server.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "504",
                description = "Tidsavbrudd (Gateway Timeout) mot Skatteetatens system (ELIN).",
                content = [Content()],
            ),
        ],
    )
    fun hentInnkrevingssakPåPerson(@RequestBody personRequest: PersonRequest): ResponseEntity<BidragssakMedSkyldnerDto> {
        val innkrevingssakPåPerson = reskontroService.hentInnkrevingssakPåPerson(personRequest)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(innkrevingssakPåPerson)
    }

    @PostMapping("/transaksjoner/bidragssak")
    @Operation(
        summary = "Hent transaksjoner på bidragssak",
        description = "Henter alle transaksjoner fra ELIN som er registrert på en bidragssak identifisert med saksnummer. " +
            "Transaksjonene inkluderer beløp, transaksjonskode, periode og involvert skyldner/mottaker.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Transaksjoner for bidragssaken ble funnet og returneres.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = TransaksjonerDto::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Ugyldig forespørsel – f.eks. manglende eller feil format på saksnummer.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Ingen transaksjoner funnet for oppgitt saksnummer.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "500",
                description = "Uventet feil på server.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "504",
                description = "Tidsavbrudd (Gateway Timeout) mot Skatteetatens system (ELIN).",
                content = [Content()],
            ),
        ],
    )
    fun hentTransaksjonerPåBidragssak(@RequestBody saksnummerRequest: SaksnummerRequest): ResponseEntity<TransaksjonerDto> {
        val transaksjonerPåBidragssak = reskontroService.hentTransaksjonerPåBidragssak(saksnummerRequest)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(transaksjonerPåBidragssak)
    }

    @PostMapping("/transaksjoner/person")
    @Operation(
        summary = "Hent transaksjoner på person",
        description = "Henter alle transaksjoner fra ELIN som er knyttet til en persons fødselsnummer eller D-nummer. " +
            "Transaksjonene inkluderer beløp, transaksjonskode, periode og involvert skyldner/mottaker.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Transaksjoner for personen ble funnet og returneres.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = TransaksjonerDto::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Ugyldig forespørsel – f.eks. manglende eller feil format på personident.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Ingen transaksjoner funnet for oppgitt personident.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "500",
                description = "Uventet feil på server.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "504",
                description = "Tidsavbrudd (Gateway Timeout) mot Skatteetatens system (ELIN).",
                content = [Content()],
            ),
        ],
    )
    fun hentTransaksjonerPåPerson(@RequestBody personRequest: PersonRequest): ResponseEntity<TransaksjonerDto> {
        val transaksjonerPåPerson = reskontroService.hentTransaksjonerPåPerson(personRequest)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(transaksjonerPåPerson)
    }

    @GetMapping("/transaksjoner/transaksjonsid")
    @Operation(
        summary = "Hent transaksjoner på transaksjons-ID",
        description = "Henter én eller flere transaksjoner fra ELIN basert på en unik transaksjons-ID. " +
            "Brukes typisk for å slå opp detaljer om en kjent transaksjon.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Transaksjon(er) for oppgitt transaksjons-ID ble funnet og returneres.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = TransaksjonerDto::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Ugyldig forespørsel – f.eks. manglende eller ikke-numerisk transaksjons-ID.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Ingen transaksjoner funnet for oppgitt transaksjons-ID.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "500",
                description = "Uventet feil på server.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "504",
                description = "Tidsavbrudd (Gateway Timeout) mot Skatteetatens system (ELIN).",
                content = [Content()],
            ),
        ],
    )
    fun hentTransaksjonerPåTransaksjonsid(
        @Parameter(description = "Unik numerisk ID for transaksjonen som skal slås opp.", required = true, example = "123456789")
        @RequestParam transaksjonsid: Long,
    ): ResponseEntity<TransaksjonerDto> {
        val transaksjonerPåTransaksjonsid = reskontroService.hentTransaksjonerPåTransaksjonsid(transaksjonsid)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(transaksjonerPåTransaksjonsid)
    }

    @PostMapping("/innkrevingsinformasjon")
    @Operation(
        summary = "Hent innkrevingssaksinformasjon på person",
        description = "Henter detaljert informasjon om innkrevingssaken knyttet til en persons fødselsnummer eller D-nummer. " +
            "Inkluderer skyldnerinformasjon, gjeldende betalingsordning, eventuell ny betalingsordning og sakshistorikk.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Innkrevingssaksinformasjon for personen ble funnet og returneres.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = InnkrevingssaksinformasjonDto::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Ugyldig forespørsel – f.eks. manglende eller feil format på personident.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Ingen innkrevingsinformasjon funnet for oppgitt personident.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "500",
                description = "Uventet feil på server.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "504",
                description = "Tidsavbrudd (Gateway Timeout) mot Skatteetatens system (ELIN).",
                content = [Content()],
            ),
        ],
    )
    fun hentInformasjonOmInnkrevingssaken(@RequestBody personRequest: PersonRequest): ResponseEntity<InnkrevingssaksinformasjonDto> {
        val informasjonOmInnkrevingssaken = reskontroService.hentInformasjonOmInnkrevingssaken(personRequest)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(informasjonOmInnkrevingssaken)
    }

    @PatchMapping("/endreRmForSak")
    @Operation(
        summary = "Endre regnskapsmottaker (RM) for sak",
        description = "Oppdaterer regnskapsmottaker (RM) for et barn i en bidragssak i ELIN. " +
            "Brukes når et barns fødselsnummer skal byttes ut, f.eks. ved tildeling av nytt D-nummer.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Regnskapsmottaker ble endret.",
            ),
            ApiResponse(
                responseCode = "400",
                description = "Ugyldig forespørsel – f.eks. manglende felter eller ugyldig fødselsnummer.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "500",
                description = "Uventet feil på server.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "504",
                description = "Tidsavbrudd (Gateway Timeout) mot Skatteetatens system (ELIN).",
                content = [Content()],
            ),
        ],
    )
    fun endreRmForSak(@RequestBody endreRmForSak: EndreRmForSakRequest) {
        reskontroService.endreRmForSak(endreRmForSak.saksnummer, endreRmForSak.barn, endreRmForSak.nyttFødselsnummer)
    }

    @GetMapping("/transaksjonskoder")
    @Operation(
        summary = "Hent alle gyldige transaksjonskoder",
        description = "Returnerer en komplett liste over alle gyldige transaksjonskoder med tilhørende beskrivelse. " +
            "Kan brukes som oppslagsverk for å tolke transaksjonskoder i andre responser.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Liste over alle gyldige transaksjonskoder returneres.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = ArraySchema(schema = Schema(implementation = TransaksjonskodeDto::class)))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
        ],
    )
    fun hentTransaksjonskoder(): ResponseEntity<List<TransaksjonskodeDto>> = ResponseEntity.ok(Transaksjonskode.entries.tilDto())
}
