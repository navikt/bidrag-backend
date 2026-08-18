package no.nav.bidrag.samhandler.controller

import no.nav.bidrag.commons.service.LANDKODER
import no.nav.bidrag.commons.service.hentKodeverkKodeBeskrivelseMap
import no.nav.bidrag.domene.enums.diverse.Språk
import no.nav.bidrag.domene.enums.samhandler.Kreditortype
import no.nav.bidrag.domene.enums.samhandler.OffentligIdType
import no.nav.bidrag.domene.enums.samhandler.Områdekode
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.util.visningsnavn
import no.nav.security.token.support.core.api.Protected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
class VisningsnavnController {
    @GetMapping("/visningsnavn")
    fun hentVisningsnavn(): Map<String, String> = OffentligIdType.entries.associate { it.name to it.visningsnavn.intern } +
        Kreditortype.entries.associate { it.name to it.visningsnavn } +
        Områdekode.entries.associate { it.name to it.visningsnavn } +
        Valutakode.entries.associate { it.name to it.visningsnavn } +
        Språk.entries.associate { it.name to it.visningsnavn } +
        hentKodeverkKodeBeskrivelseMap(LANDKODER).entries.associate { it.key to it.value }
}
