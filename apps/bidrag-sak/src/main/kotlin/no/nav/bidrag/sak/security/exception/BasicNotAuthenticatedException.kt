package no.nav.bidrag.sak.security.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.UNAUTHORIZED)
class BasicNotAuthenticatedException(
    msg: String?,
) : RuntimeException(msg)
