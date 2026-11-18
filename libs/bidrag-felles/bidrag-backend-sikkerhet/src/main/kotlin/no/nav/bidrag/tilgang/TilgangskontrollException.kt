package no.nav.bidrag.tilgang

import no.nav.bidrag.transport.tilgang.TilgangskontrollResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.ErrorResponseException

/**
 * Kastes når saksbehandler ikke har tilgang. Detaljene fra tilgangskontroll ligger i [ProblemDetail] under `detaljer`.
 */
class TilgangskontrollException(
    val response: TilgangskontrollResponse,
) : ErrorResponseException(HttpStatus.FORBIDDEN, lagProblemDetail(response), null) {

    private companion object {
        fun lagProblemDetail(response: TilgangskontrollResponse): ProblemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN,
            "Bruker har ikke tilgang til ressursen",
        ).apply {
            title = "Ingen tilgang"
            setProperty("detaljer", response.detaljer)
        }
    }
}
