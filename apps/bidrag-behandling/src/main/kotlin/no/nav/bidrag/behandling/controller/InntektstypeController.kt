package no.nav.bidrag.behandling.controller

import io.swagger.v3.oas.annotations.Operation
import no.nav.bidrag.domene.enums.inntekt.Inntektstype
import no.nav.bidrag.domene.enums.inntekt.Inntektstype.Companion.erSkattefrittBarnetillegg
import org.springframework.web.bind.annotation.GetMapping

@BehandlingRestControllerV1
class InntektstypeController {
    @GetMapping("/inntektstype/skattefrie-barnetillegg")
    @Operation(description = "Henter barnetilleggstypene som er skattefrie og ikke har skatteprosent")
    fun hentSkattefrieBarnetilleggstyper(): Set<Inntektstype> = Inntektstype.entries.filter { it.erSkattefrittBarnetillegg }.toSet()
}
