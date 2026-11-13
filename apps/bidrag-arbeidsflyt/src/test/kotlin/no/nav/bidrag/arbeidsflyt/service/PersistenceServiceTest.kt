package no.nav.bidrag.arbeidsflyt.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.bidrag.arbeidsflyt.persistence.repository.DLQKafkaRepository
import org.junit.jupiter.api.Test
import org.springframework.dao.DataAccessResourceFailureException

internal class PersistenceServiceTest {
    @Test
    fun `skal kaste videre lagringsfeil til Kafka-feilhåndtereren`() {
        val repository = mockk<DLQKafkaRepository>()
        val feil = DataAccessResourceFailureException("Databasen er utilgjengelig")
        every { repository.save(any()) } throws feil
        val service = PersistenceService(mockk(), repository, mockk())

        val kastet = shouldThrow<DataAccessResourceFailureException> {
            service.lagreDLQKafka("topic_sak", "1234567", "hendelse", true)
        }

        kastet shouldBe feil
    }
}
