package no.nav.bidrag.samhandler.util

import no.nav.bidrag.transport.felles.commonObjectmapper
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpStatusCodeException
import java.nio.charset.Charset

class ConflictException(
    message: String,
    body: Any,
) : HttpStatusCodeException(
    HttpStatus.CONFLICT,
    message,
    commonObjectmapper.writeValueAsBytes(body),
    Charset.defaultCharset(),
)
