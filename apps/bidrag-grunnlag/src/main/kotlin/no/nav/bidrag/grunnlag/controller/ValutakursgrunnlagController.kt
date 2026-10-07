package no.nav.bidrag.grunnlag.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.grunnlag.ISSUER
import no.nav.bidrag.grunnlag.bo.ValutakursgrunnlagBo
import no.nav.bidrag.grunnlag.service.HentHistoriskeValutakurserService
import no.nav.bidrag.grunnlag.service.Valutaberegning
import no.nav.bidrag.grunnlag.service.ValutakursgrunnlagService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springdoc.core.annotations.ParameterObject
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.LocalDate

data class InnhentValutakursgrunnlagRequest(
    @Schema(description = "Utenlandsk valuta som kursen skal hentes for. NOK trenger ikke kursgrunnlag.", example = "USD")
    val valutakode: Valutakode,
    @Schema(description = "Halvårets startdato, 1. januar eller 1. juli. Datoen kan ikke være i fremtiden.", example = "2026-07-01")
    val gyldigFra: LocalDate,
)

data class InnhentHistoriskeRequest(
    @Schema(description = "Første halvårs startdato, inkludert. Må være 1. januar eller 1. juli.", example = "2024-01-01")
    val fra: LocalDate,
    @Schema(description = "Siste halvårs sluttdato, ekskludert. Må være 1. januar eller 1. juli og etter fra. Maksimalt fem år etter fra.", example = "2025-07-01")
    val til: LocalDate,
)

data class OverstyrValutakursgrunnlagRequest(
    @Schema(description = "NOK per én enhet av grunnlagets utenlandske valuta. Må være større enn null og kunne lagres eksakt med maksimalt 22 heltallssifre og 16 desimaler. Avsluttende nuller teller ikke som ekstra desimaler.", example = "10.5000")
    val kurs: BigDecimal,
)

data class BeregnValutaRequest(
    @Schema(description = "Beløpet i fraValuta som skal regnes om.", example = "100.00")
    val beløp: BigDecimal,
    @Schema(description = "Valutaen beløpet er oppgitt i.", example = "USD")
    val fraValuta: Valutakode,
    @Schema(description = "Valutaen resultatet skal oppgis i. Minst én av fraValuta og tilValuta må være NOK.", example = "NOK")
    val tilValuta: Valutakode,
    @Schema(description = "Dato som velger hvilket lagret halvårsgrunnlag beregningen bruker.", example = "2026-10-06")
    val dato: LocalDate,
)

private const val OPPSLAG_BESKRIVELSE =
    "Henter et lagret halvårsgrunnlag med valutakode og dato, eller en paginert liste over feilede grunnlag med status=FEILET. " +
        "Ved enkeltoppslag er periodens start inkludert og slutt ekskludert. Historiske grunnlag kan hentes selv om aktiv=false. " +
        "Feillisten har standard sidestørrelse 50 og maksimalt 100. Ingen nye kurser hentes fra eksterne kilder."

@RestController
@ProtectedWithClaims(issuer = ISSUER)
class ValutakursgrunnlagController(
    private val valutakursgrunnlagService: ValutakursgrunnlagService,
    private val historiskeValutakurserService: HentHistoriskeValutakurserService,
    private val environment: Environment,
    @param:Value("\${valutakurs.skriving-lokalt-aktivert:false}") private val skrivingLokaltAktivert: Boolean = false,
) {
    @GetMapping("/valutakursgrunnlag", params = ["valutakode", "dato"])
    @Operation(security = [SecurityRequirement(name = "bearer-key")], summary = "Hent valutakursgrunnlag", description = OPPSLAG_BESKRIVELSE)
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Kursgrunnlag for valgt valuta og dato, eller en side med feilede grunnlag"),
            ApiResponse(responseCode = "400", description = "Ugyldige parametere eller sidestørrelse over 100"),
            ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler eller er ugyldig"),
            ApiResponse(responseCode = "404", description = "Ingen lagret kurs for valgt valuta og dato"),
            ApiResponse(responseCode = "422", description = "Grunnlaget finnes, men har ingen gyldig, normalisert kurs"),
        ],
    )
    fun hent(
        @Parameter(description = "Valutaen det skal hentes kursgrunnlag for.", example = "USD")
        @RequestParam valutakode: Valutakode,
        @Parameter(description = "Dato innenfor grunnlagets gyldighetsperiode.", example = "2026-10-06")
        @RequestParam dato: LocalDate,
    ): ValutakursgrunnlagBo {
        val grunnlag = valutakursgrunnlagService.hentValutakursgrunnlag(valutakode, dato)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Valutakursgrunnlag finnes ikke")
        if (grunnlag.feiletHenting || grunnlag.kurs == null || grunnlag.kurs.signum() <= 0 || grunnlag.multiplikator != 0) {
            throw ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, "Valutakurs er ikke tilgjengelig")
        }
        return grunnlag
    }

    @GetMapping("/valutakursgrunnlag", params = ["status=FEILET"])
    @Operation(security = [SecurityRequirement(name = "bearer-key")], summary = "Hent valutakursgrunnlag", description = OPPSLAG_BESKRIVELSE)
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Kursgrunnlag for valgt valuta og dato, eller en side med feilede grunnlag"),
            ApiResponse(responseCode = "400", description = "Ugyldige parametere eller sidestørrelse over 100"),
            ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler eller er ugyldig"),
            ApiResponse(responseCode = "404", description = "Ingen lagret kurs for valgt valuta og dato"),
            ApiResponse(responseCode = "422", description = "Grunnlaget finnes, men har ingen gyldig, normalisert kurs"),
        ],
    )
    fun hentFeilede(@ParameterObject @PageableDefault(size = 50) pageable: Pageable): Page<ValutakursgrunnlagBo> {
        if (pageable.pageSize > 100) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Maksimalt 100 valutakursgrunnlag per side")
        }
        return valutakursgrunnlagService.hentFeiledeValutakursgrunnlag(pageable)
    }

    @PostMapping("/valutakursgrunnlag/innhentinger")
    @Operation(
        security = [SecurityRequirement(name = "bearer-key")],
        summary = "Hent og lagre kurs for én valuta og ett halvår",
        description = "Henter månedsmiddel fra desember for halvår som starter 1. januar, og fra juni for halvår som starter 1. juli. " +
            "ECB brukes først, med Norges Bank som reserve. Kursen lagres som NOK per én enhet utenlandsk valuta. " +
            "Et nytt kall oppdaterer samme grunnlag, men overskriver ikke en manuelt overstyrt kurs. " +
            "Hvis ingen kurs kan hentes, lagres og returneres status FEILET med kurs=null.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Lagret kursgrunnlag med status HENTET, FEILET eller OVERSTYRT"),
            ApiResponse(responseCode = "400", description = "Ugyldig valuta eller startdato"),
            ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler eller er ugyldig"),
        ],
    )
    fun innhent(@RequestBody request: InnhentValutakursgrunnlagRequest): ValutakursgrunnlagBo {
        krevLokalSkrivetilgang()
        if (request.valutakode == Valutakode.NOK || request.gyldigFra.dayOfMonth != 1 ||
            request.gyldigFra.monthValue !in listOf(1, 7) || request.gyldigFra.isAfter(LocalDate.now())
        ) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Oppgi en utenlandsk valuta og 1. januar eller 1. juli som startdato")
        }
        return valutakursgrunnlagService.innhent(request.valutakode, request.gyldigFra)
    }

    @PostMapping("/valutakursgrunnlag/historisk")
    @Operation(
        security = [SecurityRequirement(name = "bearer-key")],
        summary = "Hent og lagre historiske halvårskurser",
        description = "Henter kurser for alle utenlandske valutaer som var aktive ved hvert halvårs start. " +
            "Fra-datoen er inkludert og til-datoen ekskludert. Begge datoer må være 1. januar eller 1. juli. " +
            "Maksimalt 10 halvår kan hentes per kall. Inneværende halvår er tillatt selv om sluttdatoen er i fremtiden, men fremtidige halvår avvises. " +
            "Eksisterende grunnlag hoppes over, unntatt FEILET-rader som forsøkes hentet på nytt. " +
            "ECB brukes først, med Norges Bank som reserve. Svaret inneholder bare grunnlagene som ble behandlet i dette kallet.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Liste over behandlede grunnlag, inkludert FEILET-rader. Tom liste dersom ingen grunnlag måtte hentes"),
            ApiResponse(responseCode = "400", description = "Ugyldige halvårsgrenser, datorekkefølge, fremtidige halvår eller mer enn 10 perioder"),
            ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler eller er ugyldig"),
        ],
    )
    fun innhentHistoriskeValutakursgrunnlag(@RequestBody request: InnhentHistoriskeRequest): List<ValutakursgrunnlagBo> {
        krevLokalSkrivetilgang()
        return historiskeValutakurserService.hentHistoriskeValutakurser(request.fra, request.til)
    }

    @PutMapping("/valutakursgrunnlag/{id}/kurs")
    @Operation(
        security = [SecurityRequirement(name = "bearer-key")],
        summary = "Overstyr en lagret valutakurs",
        description = "Erstatter kursen i et eksisterende grunnlag med en positiv kurs i NOK per én enhet utenlandsk valuta. " +
            "Setter status OVERSTYRT og kilde MANUELL, fjerner feilstatus og oppdaterer tidspunktet. " +
            "Valuta og gyldighetsperiode beholdes. Senere automatisk innhenting overskriver ikke den manuelle kursen.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Oppdatert kursgrunnlag med status OVERSTYRT"),
            ApiResponse(responseCode = "400", description = "Kursen er ikke større enn null, overskrider 22 heltallssifre eller 16 desimaler, eller forespørselen er ugyldig"),
            ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler eller er ugyldig"),
            ApiResponse(responseCode = "404", description = "Valutakursgrunnlaget finnes ikke"),
        ],
    )
    fun overstyr(
        @Parameter(description = "ID til det lagrede valutakursgrunnlaget som skal overstyres.", example = "42")
        @PathVariable id: Int,
        @RequestBody request: OverstyrValutakursgrunnlagRequest,
    ): ValutakursgrunnlagBo {
        krevLokalSkrivetilgang()
        return valutakursgrunnlagService.overstyr(id, request.kurs)
    }

    @PostMapping("/valutakursberegninger")
    @Operation(
        security = [SecurityRequirement(name = "bearer-key")],
        summary = "Regn om et beløp til eller fra NOK",
        description = "Bruker lagret kursgrunnlag for valgt dato uten å hente eller lagre nye kurser. " +
            "Støtter utenlandsk valuta til NOK, NOK til utenlandsk valuta og NOK til NOK, men ikke to utenlandske valutaer. " +
            "Resultatet avrundes til fire desimaler med HALF_UP og returneres sammen med kurs og brukt grunnlag. " +
            "For NOK til NOK er kursen 1 og kursgrunnlag=null. Historiske grunnlag kan brukes selv om aktiv=false.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Omregnet beløp, kurs og eventuelt brukt kursgrunnlag"),
            ApiResponse(responseCode = "400", description = "Omregning mellom to utenlandske valutaer støttes ikke, eller forespørselen er ugyldig"),
            ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler eller er ugyldig"),
            ApiResponse(responseCode = "404", description = "Ingen lagret kurs for valgt valuta og dato"),
            ApiResponse(responseCode = "422", description = "Grunnlaget finnes, men har ingen gyldig, normalisert kurs"),
        ],
    )
    fun beregn(@RequestBody request: BeregnValutaRequest): Valutaberegning = valutakursgrunnlagService.beregn(request.beløp, request.fraValuta, request.tilValuta, request.dato)

    private fun krevLokalSkrivetilgang() {
        if (!skrivingLokaltAktivert || !environment.acceptsProfiles(Profiles.of("local"))) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Skriveoperasjonen er ikke aktivert")
        }
    }
}
