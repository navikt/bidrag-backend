package no.nav.bidrag.cache.tømming

import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@Protected
@RequestMapping("/bidrag-cache")
class CacheController(
    private val cacheTømmer: CacheTømmer,
) {
    @DeleteMapping
    fun tømAlle(): ResponseEntity<Unit> {
        cacheTømmer.tøm(null)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{navn}")
    fun tøm(
        @PathVariable navn: String,
    ): ResponseEntity<Unit> {
        val gyldigNavn = GYLDIG_CACHENAVN.matchEntire(navn)?.value ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Ugyldig cachenavn")
        cacheTømmer.tøm(gyldigNavn)
        return ResponseEntity.noContent().build()
    }

    private companion object {
        val GYLDIG_CACHENAVN = Regex("[A-Za-z0-9_.-]{1,100}")
    }
}
