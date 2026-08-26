package no.nav.bidrag.commons.web

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import java.util.Optional

/**
 * Redirecting [ResponseEntity] from one service to another
 *
 * @param <T> type of http payload
</T> */
class HttpResponse<T : Any>(
    val responseEntity: ResponseEntity<T>,
) {
    fun fetchBody(): Optional<T> = Optional.ofNullable(responseEntity.body)

    fun is2xxSuccessful(): Boolean = responseEntity.statusCode.is2xxSuccessful

    fun fetchHeaders(): HttpHeaders = responseEntity.headers

    fun clearContentHeaders(): HttpResponse<T> {
        val headers = HttpHeaders(responseEntity.headers)
        headers.clearContentHeaders()
        return from(responseEntity.body, headers, responseEntity.statusCode)
    }

    companion object {
        fun <E : Any> from(httpStatus: HttpStatusCode): HttpResponse<E> {
            val responseEntity = ResponseEntity<E>(httpStatus)
            return HttpResponse(responseEntity)
        }

        fun <E : Any> from(
            httpStatus: HttpStatusCode,
            body: E?,
        ): HttpResponse<E> {
            val responseEntity = ResponseEntity(body, httpStatus)
            return HttpResponse(responseEntity)
        }

        fun <E : Any> from(
            httpHeaders: HttpHeaders,
            httpStatus: HttpStatusCode,
        ): HttpResponse<E> {
            val responseEntity = ResponseEntity<E>(httpHeaders, httpStatus)
            return HttpResponse(responseEntity)
        }

        fun <E : Any> from(
            body: E?,
            httpHeaders: HttpHeaders?,
            httpStatus: HttpStatusCode,
        ): HttpResponse<E> {
            val responseEntity = ResponseEntity(body, httpHeaders, httpStatus)
            return HttpResponse(responseEntity)
        }
    }
}
