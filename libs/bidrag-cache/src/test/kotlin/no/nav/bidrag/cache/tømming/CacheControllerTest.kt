package no.nav.bidrag.cache.tømming

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class CacheControllerTest {
    private val cacheTømmer = mockk<CacheTømmer>(relaxed = true)
    private val controller = CacheController(cacheTømmer)

    @Test
    fun `skal tømme alle cacher`() {
        controller.tømAlle().statusCode shouldBe HttpStatus.NO_CONTENT

        verify { cacheTømmer.tøm(null) }
    }

    @Test
    fun `skal tømme cache med gyldig navn`() {
        controller.tøm("kodeverk-1.0").statusCode shouldBe HttpStatus.NO_CONTENT

        verify { cacheTømmer.tøm("kodeverk-1.0") }
    }

    @Test
    fun `skal avvise ugyldig cachenavn`() {
        val feil = shouldThrow<ResponseStatusException> { controller.tøm("kodeverk*") }

        feil.statusCode shouldBe HttpStatus.BAD_REQUEST
        verify(exactly = 0) { cacheTømmer.tøm(any()) }
    }
}
