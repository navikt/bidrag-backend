package no.nav.bidrag.automatiskjobb.controller

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.batch.core.configuration.JobRegistry
import org.springframework.batch.core.repository.JobRepository
import org.springframework.http.HttpStatus

@ExtendWith(MockKExtension::class)
internal class BatchInfoControllerTest {
    private val jobRepository: JobRepository = mockk(relaxed = true)
    private val jobRegistry: JobRegistry = mockk(relaxed = true)

    private val batchInfoController = BatchInfoController(jobRepository, jobRegistry)

    private val jobbNavn = "opprettAldersjusteringerBidragJob"

    private fun registrerJobb() {
        every { jobRegistry.jobNames } returns setOf(jobbNavn)
    }

    @Test
    fun `hentFremgang skal avvise negativt historikkAntall`() {
        registrerJobb()

        val respons = batchInfoController.hentFremgang(jobbNavn, -1)

        respons.statusCode shouldBe HttpStatus.BAD_REQUEST
        verify(exactly = 0) { jobRepository.findRunningJobExecutions(any()) }
        verify(exactly = 0) { jobRepository.getJobInstances(any(), any(), any()) }
    }

    @Test
    fun `hentFremgang skal avvise historikkAntall lik null og over maksgrensen`() {
        registrerJobb()

        batchInfoController.hentFremgang(jobbNavn, 0).statusCode shouldBe HttpStatus.BAD_REQUEST
        batchInfoController.hentFremgang(jobbNavn, 1001).statusCode shouldBe HttpStatus.BAD_REQUEST
        batchInfoController.hentFremgang(jobbNavn, Int.MAX_VALUE).statusCode shouldBe HttpStatus.BAD_REQUEST
        batchInfoController.hentFremgang(jobbNavn, Int.MIN_VALUE).statusCode shouldBe HttpStatus.BAD_REQUEST

        verify(exactly = 0) { jobRepository.findRunningJobExecutions(any()) }
    }

    @Test
    fun `hentFremgang skal godta gyldig historikkAntall og slå opp jobben i registeret`() {
        registrerJobb()
        every { jobRepository.findRunningJobExecutions(jobbNavn) } returns emptySet()

        val respons = batchInfoController.hentFremgang(jobbNavn, 10)

        // Ingen aktiv kjøring gir 404, men validering av parameterne har passert
        respons.statusCode shouldBe HttpStatus.NOT_FOUND
        verify(exactly = 1) { jobRepository.findRunningJobExecutions(jobbNavn) }
    }

    @Test
    fun `hentFremgang skal avvise ukjent jobbnavn uten å spørre databasen`() {
        registrerJobb()

        listOf("../../etc/passwd", "ukjentJobb", "", "opprettAldersjusteringerBidragJob ").forEach { ugyldig ->
            batchInfoController.hentFremgang(ugyldig, 10).statusCode shouldBe HttpStatus.NOT_FOUND
        }

        verify(exactly = 0) { jobRepository.findRunningJobExecutions(any()) }
    }

    @Test
    fun `hentJobbDetaljer skal avvise negativt antall`() {
        registrerJobb()

        val respons = batchInfoController.hentJobbDetaljer(jobbNavn, -5)

        respons.statusCode shouldBe HttpStatus.BAD_REQUEST
        verify(exactly = 0) { jobRepository.getJobInstances(any(), any(), any()) }
    }

    @Test
    fun `hentJobbDetaljer skal bruke jobbnavn fra registeret og gyldig antall`() {
        registrerJobb()
        every { jobRepository.getJobInstances(jobbNavn, 0, 5) } returns emptyList()
        every { jobRepository.getJobInstanceCount(jobbNavn) } returns 0L

        val respons = batchInfoController.hentJobbDetaljer(jobbNavn, 5)

        respons.statusCode shouldBe HttpStatus.OK
        respons.body?.jobNavn shouldBe jobbNavn
        verify(exactly = 1) { jobRepository.getJobInstances(jobbNavn, 0, 5) }
    }

    @Test
    fun `hentJobbDetaljer skal avvise ukjent jobbnavn`() {
        registrerJobb()

        val respons = batchInfoController.hentJobbDetaljer("../../etc/passwd", 5)

        respons.statusCode shouldBe HttpStatus.NOT_FOUND
        verify(exactly = 0) { jobRepository.getJobInstances(any(), any(), any()) }
    }

    @Test
    fun `hentSisteKjøring skal avvise ukjent jobbnavn`() {
        registrerJobb()

        val respons = batchInfoController.hentSisteKjøring("ukjentJobb")

        respons.statusCode shouldBe HttpStatus.NOT_FOUND
        verify(exactly = 0) { jobRepository.getJobInstances(any(), any(), any()) }
    }

    @Test
    fun `hentStatistikk skal avvise negativt antall`() {
        registrerJobb()

        val respons = batchInfoController.hentStatistikk(-1)

        respons.statusCode shouldBe HttpStatus.BAD_REQUEST
        verify(exactly = 0) { jobRepository.getJobInstances(any(), any(), any()) }
    }

    @Test
    fun `hentStatistikk skal bruke gyldig antall`() {
        registrerJobb()
        every { jobRepository.getJobInstances(jobbNavn, 0, 50) } returns emptyList()

        val respons = batchInfoController.hentStatistikk(50)

        respons.statusCode shouldBe HttpStatus.OK
        verify(exactly = 1) { jobRepository.getJobInstances(jobbNavn, 0, 50) }
    }
}
