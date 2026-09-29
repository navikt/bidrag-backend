package no.nav.bidrag.grunnlag.controller

import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.grunnlag.ISSUER
import no.nav.bidrag.grunnlag.bo.ValutakursgrunnlagBo
import no.nav.bidrag.grunnlag.service.Valutaberegning
import no.nav.bidrag.grunnlag.service.ValutakursgrunnlagService
import no.nav.security.token.support.core.api.ProtectedWithClaims
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
    val valutakode: Valutakode,
    val gyldigFra: LocalDate,
)

data class OverstyrValutakursgrunnlagRequest(val kurs: BigDecimal)

data class BeregnValutaRequest(
    val beløp: BigDecimal,
    val fraValuta: Valutakode,
    val tilValuta: Valutakode,
    val dato: LocalDate,
)

@RestController
@ProtectedWithClaims(issuer = ISSUER)
class ValutakursgrunnlagController(
    private val valutakursgrunnlagService: ValutakursgrunnlagService,
    private val environment: Environment,
    @param:Value("\${valutakurs.skriving-lokalt-aktivert:false}") private val skrivingLokaltAktivert: Boolean = false,
) {
    @GetMapping("/valutakursgrunnlag", params = ["valutakode", "dato"])
    fun hent(
        @RequestParam valutakode: Valutakode,
        @RequestParam dato: LocalDate,
    ): ValutakursgrunnlagBo {
        val grunnlag = valutakursgrunnlagService.hentValutakursgrunnlag(valutakode, dato)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Valutakursgrunnlag finnes ikke")
        if (grunnlag.feiletHenting || grunnlag.kurs == null || grunnlag.kurs.signum() <= 0 || grunnlag.multiplikator != 0) {
            throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Valutakurs er ikke tilgjengelig")
        }
        return grunnlag
    }

    @GetMapping("/valutakursgrunnlag", params = ["status=FEILET"])
    fun hentFeilede(@PageableDefault(size = 50) pageable: Pageable): Page<ValutakursgrunnlagBo> {
        if (pageable.pageSize > 100) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Maksimalt 100 valutakursgrunnlag per side")
        }
        return valutakursgrunnlagService.hentFeiledeValutakursgrunnlag(pageable)
    }

    @PostMapping("/valutakursgrunnlag/innhentinger")
    fun innhent(@RequestBody request: InnhentValutakursgrunnlagRequest): ValutakursgrunnlagBo {
        krevLokalSkrivetilgang()
        if (request.valutakode == Valutakode.NOK || request.gyldigFra.dayOfMonth != 1 ||
            request.gyldigFra.monthValue !in listOf(1, 7) || request.gyldigFra.isAfter(LocalDate.now())
        ) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Oppgi en utenlandsk valuta og 1. januar eller 1. juli som startdato")
        }
        return valutakursgrunnlagService.innhent(request.valutakode, request.gyldigFra)
    }

    @PutMapping("/valutakursgrunnlag/{id}/kurs")
    fun overstyr(
        @PathVariable id: Int,
        @RequestBody request: OverstyrValutakursgrunnlagRequest,
    ): ValutakursgrunnlagBo {
        krevLokalSkrivetilgang()
        return valutakursgrunnlagService.overstyr(id, request.kurs)
    }

    @PostMapping("/valutakursberegninger")
    fun beregn(@RequestBody request: BeregnValutaRequest): Valutaberegning = valutakursgrunnlagService.beregn(request.beløp, request.fraValuta, request.tilValuta, request.dato)

    private fun krevLokalSkrivetilgang() {
        return
        if (!skrivingLokaltAktivert || !environment.acceptsProfiles(Profiles.of("local"))) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Skriveoperasjonen er ikke aktivert")
        }
    }
}
