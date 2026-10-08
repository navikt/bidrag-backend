package no.nav.bidrag.rest

import no.nav.bidrag.rest.exceptions.RessursIkkeFunnetException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class GlobalRestControllerAdviceTest {
    @Test
    fun `skal returnere 404 når ressurs ikke finnes`() {
        val problem = GlobalRestControllerAdvice().handleRessursIkkeFunnet(RessursIkkeFunnetException("Stønad"))

        assertThat(problem.status).isEqualTo(HttpStatus.NOT_FOUND.value())
        assertThat(problem.detail).isEqualTo("Stønad ikke funnet")
    }
}
