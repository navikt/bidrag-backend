package no.nav.bidrag.henvendelse.config

import org.slf4j.MDC
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import java.util.UUID

/**
 * Setter `X-Correlation-ID` til en UUID på utgående kall.
 *
 * sf-henvendelse-api-proxy krever headeren og dokumenterer den som en UUID.
 * `MdcValuesPropagatingClientInterceptor` i bidrag-commons legger allerede på en verdi - den
 * propagerer callId-en under alle navnene NAV bruker, inkludert `X-Correlation-Id` - men den
 * verdien er på formatet `<32 hex>-<appnavn>` og altså ikke en UUID. Siden `HttpHeaders` er
 * case-insensitiv og commons bruker `add`, ville en header satt på selve forespørselen gitt
 * *to* verdier i headeren.
 *
 * Denne interceptoren registreres derfor sist, og bruker `set` framfor `add`, slik at det går
 * ut nøyaktig én verdi.
 *
 * Id-en legges også i MDC, slik at den står som felt på loggpostene for kallet. Da har vi noe
 * å oppgi til team NKS når et kall feiler. MdcFilter tømmer MDC etter hver forespørsel.
 */
class KorrelasjonsIdInterceptor : ClientHttpRequestInterceptor {
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        val korrelasjonsId = UUID.randomUUID().toString()
        request.headers.set(HEADER_CORRELATION_ID, korrelasjonsId)
        MDC.put(MDC_KORRELASJONSID, korrelasjonsId)
        return execution.execute(request, body)
    }

    companion object {
        const val HEADER_CORRELATION_ID = "X-Correlation-ID"
        const val MDC_KORRELASJONSID = "korrelasjonsIdHenvendelse"
    }
}
