package no.nav.bidrag.regnskap.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.regnskap.consumer.SkattConsumer
import no.nav.bidrag.regnskap.hendelse.schedule.krav.ResendingAvKravScheduler
import no.nav.bidrag.regnskap.hendelse.schedule.krav.SendKravScheduler
import no.nav.bidrag.regnskap.hendelse.schedule.krav.SjekkAvBehandlingsstatusScheduler
import no.nav.bidrag.transport.regnskap.behandlingsstatus.BehandlingsstatusResponse
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
@Tag(name = "Behandlingsstatus", description = "Sjekk av behandlingsstatus for konteringer sendt til ELIN, og manuell styring av krav-oversending og resending.")
class BehandlingsstatusController(
    private val skattConsumer: SkattConsumer,
    private val sjekkAvBehandlingsstatusScheduler: SjekkAvBehandlingsstatusScheduler,
    private val resendingAvKravScheduler: ResendingAvKravScheduler,
    private val sendKravScheduler: SendKravScheduler,
) {

    @GetMapping("/behandlingsstatus")
    @Operation(
        summary = "Sjekker behandlingsstatus for en Batch-uid",
        description = "Sjekker behandlingsstatus for en batch-oversending til ELIN identifisert av en batch-UID.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Behandlingsstatus returnert.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = BehandlingsstatusResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Ugyldig forespørsel – f.eks. manglende eller feil format på batch-UID.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(responseCode = "404", description = "Ingen behandlingsstatus funnet for oppgitt batch-UID.", content = [Content()]),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
            ApiResponse(responseCode = "504", description = "Tidsavbrudd (Gateway Timeout) mot eksternt system.", content = [Content()]),
        ],
    )
    fun hentBehandlingsstatus(
        @Parameter(description = "Unik ID for batch-oversendingen som skal sjekkes.", required = true, example = "abc-123")
        @RequestParam(required = true)
        batchUid: String,
    ): ResponseEntity<BehandlingsstatusResponse> = skattConsumer.sjekkBehandlingsstatus(batchUid)

    @GetMapping("/behandlingsstatusScheduled")
    @Operation(
        summary = "Manuelt starter skedulert kjøring av sjekk på behandlingsstatus",
        description = "Starter den skedulerte jobben for sjekk av behandlingsstatus manuelt. Jobben kjøres normalt automatisk.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Startet skedulert kjøring av sjekk på behandlingsstatus."),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
        ],
    )
    fun startSkedulertSjekkAvBehandlingsstatus(): ResponseEntity<Any> {
        sjekkAvBehandlingsstatusScheduler.kjørSjekkAvBehandlingsstatus()
        return ResponseEntity.ok().build()
    }

    @GetMapping("/sendKrav")
    @Operation(
        summary = "Start manuell oversending av ikke-oversendte krav",
        description = "Starter oversending av konteringer som ikke har blitt oversendt til ELIN. Kjøres normalt automatisk hvert 10. minutt.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Startet oversending av krav."),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
        ],
    )
    fun sendKrav(): ResponseEntity<Any> {
        sendKravScheduler.skedulertOverforingAvKrav()
        return ResponseEntity.ok().build()
    }

    @GetMapping("/resendKravForSak")
    @Operation(
        summary = "Start manuell resending av feilede krav for en sak",
        description = "Starter resending av alle feilede konteringer for en enkelt sak. Tilsvarende jobb for alle saker kjører automatisk hver morgen kl 04:02.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Startet resending av ikke godkjente krav for sak."),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
        ],
    )
    fun resendKravForSak(
        @Parameter(description = "Saksnummeret det skal kjøres resending for.", required = true, example = "2200001")
        @RequestParam(required = true)
        saksnummer: String,
    ): ResponseEntity<Any> {
        resendingAvKravScheduler.resendingAvKravForSak(saksnummer)
        return ResponseEntity.ok().build()
    }
}
