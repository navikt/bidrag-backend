package no.nav.bidrag.henvendelse.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.henvendelse.aop.UgyldigIdentException
import no.nav.bidrag.henvendelse.dto.HenvendelserDto
import no.nav.bidrag.henvendelse.service.HenvendelseService
import no.nav.bidrag.transport.person.PersonRequest
import no.nav.security.token.support.core.api.Protected
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * ### Eksempel
 * ```
 * POST /henvendelser
 * Authorization: Bearer <on-behalf-of-token fra bidrag-frontend>
 * ```
 * ```json
 * { "ident": "17490123474" }
 * ```
 *
 * Svar (200):
 * ```json
 * {
 *   "henvendelser": [
 *     {
 *       "kjedeId": "a0J3N000004dUBJUA2",
 *       "henvendelsestype": "MELDINGSKJEDE",
 *       "tema": "BID",
 *       "temagruppe": "FMLI",
 *       "sisteMeldingSendt": "2026-06-28T09:30:00Z"
 *     }
 *   ]
 * }
 * ```
 * Tom liste er `{ "henvendelser": [] }`, aldri `null`.
 *
 * Svar når en tjeneste vi er avhengig av feiler (ProblemDetail, RFC 7807):
 * ```json
 * {
 *   "type": "about:blank",
 *   "title": "Feil ved kall mot tjeneste",
 *   "status": 502,
 *   "detail": "Kunne ikke hente henvendelser fordi en tjeneste vi er avhengig av svarte med feil."
 * }
 * ```
 * Detaljer om den underliggende feilen står i loggen, ikke i responsen - meldingene inneholder
 * både URL-er med aktørid og verdier fra request-body.
 */
@RestController
@Protected
@Tag(
    name = "Henvendelse",
    description = "Endepunkt for oppslag av henvendelser (chat, meldingskjeder og samtalereferater) på en person.",
)
class HenvendelseController(
    private val henvendelseService: HenvendelseService,
) {
    /**
     * Identen sendes i request-body, ikke som path- eller query-parameter. Det er konvensjonen i
     * monorepoet av personvernhensyn - se de utgåtte GET-variantene i bidrag-person.
     *
     * `PersonRequest` har ingen valideringsannotasjoner, og `Personident` godtar enhver streng,
     * så identen valideres eksplisitt her framfor å sende søppel videre til bidrag-person.
     */
    @PostMapping("/henvendelser")
    @Operation(
        summary = "Hent henvendelser for person",
        description = "Henter henvendelsene som er registrert på personen i henvendelsesløsningen (Salesforce via " +
            "sf-henvendelse-api-proxy). Kodeverdier for tema og temagruppe dekodes ikke, og lenken videre til Modia " +
            "bygges av frontend ut fra kjedeId. Personer uten aktørid gir tom liste.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Henvendelsene til personen, tom liste om ingen finnes"),
            ApiResponse(responseCode = "400", description = "Identen er ikke et gyldig fødselsnummer eller d-nummer"),
            ApiResponse(responseCode = "401", description = "Ugyldig eller manglende sikkerhetstoken"),
            ApiResponse(responseCode = "502", description = "En tjeneste vi er avhengig av feilet eller svarte ikke"),
        ],
    )
    fun hentHenvendelser(
        @RequestBody request: PersonRequest,
    ): HenvendelserDto {
        // Sifferkravet må stå først: Personident.gyldig() gjør substring(0, 1).toInt() etter et
        // toLongOrNull()-sjekk som godtar fortegn, og kaster derfor på "+1749011234".
        if (!request.ident.verdi.all(Char::isDigit) || !request.ident.gyldig()) throw UgyldigIdentException()
        return henvendelseService.hentHenvendelser(request.ident)
    }
}
