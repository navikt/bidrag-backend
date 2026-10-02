package no.nav.bidrag.mdc
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

class CallIdClientRequestInterceptor(private val headerName: String = "Nav-Call-Id") : ClientHttpRequestInterceptor {

    private val idUtils = IdUtils()

    fun callIdFromMDC() = MdcHelper.callId ?: idUtils.generateId()

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        val callId: String = callIdFromMDC()
        request.headers[headerName] = callId
        return execution.execute(request, body)
    }
}
