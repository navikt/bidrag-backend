package no.nav.bidrag.admin.consumer

import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.client5.http.io.HttpClientConnectionManager
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier
import org.apache.hc.core5.ssl.SSLContexts
import org.apache.hc.core5.ssl.TrustStrategy
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import javax.net.ssl.SSLContext

@Service
class BisysConsumer(
    @Value("\${BISYS_URL}") private val bisysBaseUrl: String,
    @Value("\${IS_LOCAL:false}") private val localRun: Boolean,
) {
    val urlBuilder get() =
        UriComponentsBuilder
            .fromUriString("https://$bisysBaseUrl")
            .port("9445")
            .pathSegment("rtv-bidrag-batch", "rest", "batch")

    fun getJobNames(): List<String> = getRestTemplate().getForEntity(urlBuilder.pathSegment("jobs").build().toUri(), List::class.java).body as List<String>

    fun getRunningExecutions(jobName: String): List<Long> {
        val safeJobName =
            Regex("[\\w\\-]+").matchEntire(jobName)?.value
                ?: throw IllegalArgumentException("Ugyldig jobName: $jobName")
        return getRestTemplate()
            .getForEntity(
                urlBuilder.pathSegment("running", "executions", "{jobName}").build().toUriString(),
                List::class.java,
                safeJobName,
            ).body as List<Long>
    }

    fun launchJob(jobName: String): Long? {
        val safeJobName =
            Regex("[\\w\\-]+").matchEntire(jobName)?.value
                ?: throw IllegalArgumentException("Ugyldig jobName: $jobName")
        return getRestTemplate()
            .getForEntity(
                urlBuilder.pathSegment("launch", "{jobName}").build().toUriString(),
                Long::class.java,
                safeJobName,
            ).body
    }

    fun executionStatus(executionId: String): Int? {
        // \d+ – kun siffer (0-9), minst ett tegn
        val executionIdValue = executionId.toLongOrNull() ?: throw IllegalArgumentException("Ugyldig executionId: $executionId")
        return getRestTemplate()
            .getForEntity(
                urlBuilder.pathSegment("execution", "{executionId}", "status").build().toUriString(),
                Int::class.java,
                executionIdValue,
            ).body
    }

    fun stopExecution(executionId: String): Boolean? {
        val executionIdValue = executionId.toLongOrNull() ?: throw IllegalArgumentException("Ugyldig executionId: $executionId")
        return getRestTemplate()
            .getForEntity(
                urlBuilder.pathSegment("execution", "{executionId}", "stop").build().toUriString(),
                Boolean::class.java,
                executionIdValue,
            ).body
    }

    fun exectionParameters(executionId: String): String? {
        val executionIdValue = executionId.toLongOrNull() ?: throw IllegalArgumentException("Ugyldig executionId: $executionId")
        return getRestTemplate()
            .getForEntity(
                urlBuilder.pathSegment("execution", "{executionId}", "parameters").build().toUriString(),
                String::class.java,
                executionIdValue,
            ).body
    }

    fun executionSummary(executionId: String): String? {
        val executionIdValue = executionId.toLongOrNull() ?: throw IllegalArgumentException("Ugyldig executionId: $executionId")
        return getRestTemplate()
            .getForEntity(
                urlBuilder.pathSegment("execution", "{executionId}", "summary").build().toUriString(),
                String::class.java,
                executionIdValue,
            ).body
    }

    fun executionSummaries(executionId: String): Map<Long, String> {
        val executionIdValue = executionId.toLongOrNull() ?: throw IllegalArgumentException("Ugyldig executionId: $executionId")
        return getRestTemplate()
            .getForEntity(
                urlBuilder.pathSegment("execution", "{executionId}", "summaries").build().toUriString(),
                Map::class.java,
                executionIdValue,
            ).body as Map<Long, String>
    }

    fun getRestTemplate(): RestTemplate {
        if (!localRun) {
            return RestTemplateBuilder().build()
        }
        val acceptingTrustStrategy = TrustStrategy { x509Certificates, s -> true }
        val sslContext: SSLContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build()
        val csf = DefaultClientTlsStrategy(sslContext, NoopHostnameVerifier())
        val requestFactory = HttpComponentsClientHttpRequestFactory()
        val connectionManager: HttpClientConnectionManager =
            PoolingHttpClientConnectionManagerBuilder
                .create()
                .setTlsSocketStrategy(
                    csf,
                ).build()
        val httpClient = HttpClients.custom().setConnectionManager(connectionManager).build()
        requestFactory.httpClient = httpClient
        return RestTemplate(requestFactory)
    }
}
