package no.nav.bidrag.texas

import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

/** Request interceptor adding bearer tokens from nais **/
class NaisTokenClientRequestInterceptor(private val tokenService: NaisTokenService, private val target: String) : ClientHttpRequestInterceptor {

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        val token = tokenService.oboToken(target)
        request.headers.setBearerAuth(token)
        return execution.execute(request, body)
    }
}
