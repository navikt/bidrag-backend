package no.nav.bidrag.sak.aop

import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.HttpClientErrorException

class RestResponseExceptionResolverTest {
    @RestController
    class KasterFeilFraAnnenTjeneste {
        @GetMapping("/feil")
        fun feil(): Nothing = throw HttpClientErrorException.create(
            HttpStatus.NOT_FOUND,
            "",
            HttpHeaders().apply { add(HttpHeaders.WARNING, "Fant ikke settekontor for inhabil enhet 4815") },
            ByteArray(0),
            null,
        )
    }

    private val mockMvc = MockMvcBuilders
        .standaloneSetup(KasterFeilFraAnnenTjeneste())
        .setControllerAdvice(RestResponseExceptionResolver())
        .build()

    @Test
    fun `sender feilmeldingen fra tjenesten som ble kalt videre`() {
        mockMvc.get("/feil").andExpect {
            status { isNotFound() }
            header { string(HttpHeaders.WARNING, "Fant ikke settekontor for inhabil enhet 4815") }
        }
    }
}
