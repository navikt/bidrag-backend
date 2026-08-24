package no.nav.bidrag.dokument.bestilling.persistence.bucket

import com.google.api.gax.retrying.RetrySettings
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.NoCredentials
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.StorageException
import com.google.cloud.storage.StorageOptions
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.threeten.bp.Duration

private val LOGGER = KotlinLogging.logger {}

@Component
@Scope(SCOPE_SINGLETON)
class GcpCloudStorage(
    @Value($$"${BUCKET_NAME}") private val bucketNavn: String,
    @Value($$"${GCP_HOST:#{null}}") private val host: String? = null,
) {
    private val retrySetting =
        RetrySettings
            .newBuilder()
            .setMaxAttempts(3)
            .setTotalTimeout(Duration.ofMillis(3000))
            .build()
    private val storage =
        StorageOptions
            .newBuilder()
            .setHost(host)
            .setCredentials(if (host != null) NoCredentials.getInstance() else GoogleCredentials.getApplicationDefault())
            .setRetrySettings(retrySetting)
            .build()
            .service

    fun hentFil(filnavn: String): ByteArray {
        LOGGER.info { "Henter fil ${lagBlobinfo(filnavn).blobId} fra bucket $bucketNavn" }
        try {
            val blobInfo = lagBlobinfo(filnavn)
            return storage.readAllBytes(blobInfo.blobId)
        } catch (e: StorageException) {
            throw HttpClientErrorException(
                HttpStatus.NOT_FOUND,
                "Finnes ingen dokumentfil i bucket $bucketNavn med filsti ${lagBlobinfo(filnavn).blobId}",
            )
        }
    }

    private fun lagBlobinfo(filnavn: String): BlobInfo =
        BlobInfo
            .newBuilder(bucketNavn, filnavn)
            .setContentType("application/pdf")
            .build()
}
