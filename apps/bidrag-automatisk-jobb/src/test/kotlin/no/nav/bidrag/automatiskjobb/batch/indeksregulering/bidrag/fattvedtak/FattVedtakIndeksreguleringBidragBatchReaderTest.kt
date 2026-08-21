package no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.fattvedtak

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.Indeksregulering
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Behandlingstype
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.automatiskjobb.persistence.repository.IndeksreguleringRepository
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.batch.core.job.JobExecution
import org.springframework.batch.core.job.JobInstance
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.step.StepExecution
import org.springframework.data.domain.Pageable
import java.time.Year

@ExtendWith(MockKExtension::class)
class FattVedtakIndeksreguleringBidragBatchReaderTest {
    @MockK
    private lateinit var indeksreguleringRepository: IndeksreguleringRepository

    private val forventedeStønadstyper = listOf(Stønadstype.BIDRAG, Stønadstype.OPPFOSTRINGSBIDRAG, Stønadstype.BIDRAG18AAR)

    private fun indeksregulering(
        id: Int,
        stønadstype: Stønadstype = Stønadstype.BIDRAG,
    ) = Indeksregulering(
        id = id,
        batchId = "batch",
        år = 2026,
        barn = Barn(saksnummer = "2600001", kravhaver = genererFødselsnummer(), skyldner = genererFødselsnummer()),
        stønadstype = stønadstype,
        status = Status.BEHANDLET,
        behandlingstype = Behandlingstype.FATTET_FORSLAG,
        vedtak = 999,
    )

    private fun stepExecution(år: Int? = 2026): StepExecution {
        val jobParametersBuilder = JobParametersBuilder()
        år?.let { jobParametersBuilder.addString("aar", it.toString()) }
        val jobExecution = JobExecution(1L, JobInstance(1L, "job"), jobParametersBuilder.toJobParameters())
        return StepExecution(1L, "step", jobExecution)
    }

    @Test
    fun `skal lese rader over flere sider ved hjelp av keyset-paginering basert på id`() {
        val side1 = listOf(indeksregulering(1), indeksregulering(2))
        val side2 = listOf(indeksregulering(3))

        every {
            indeksreguleringRepository.findAllByStatusAndBehandlingstypeAndVedtakIsNotNullAndÅrAndStønadstypeInAndIdGreaterThanOrderByIdAsc(
                Status.BEHANDLET,
                Behandlingstype.FATTET_FORSLAG,
                2026,
                forventedeStønadstyper,
                0,
                any(),
            )
        } returns side1
        every {
            indeksreguleringRepository.findAllByStatusAndBehandlingstypeAndVedtakIsNotNullAndÅrAndStønadstypeInAndIdGreaterThanOrderByIdAsc(
                Status.BEHANDLET,
                Behandlingstype.FATTET_FORSLAG,
                2026,
                forventedeStønadstyper,
                2,
                any(),
            )
        } returns side2
        every {
            indeksreguleringRepository.findAllByStatusAndBehandlingstypeAndVedtakIsNotNullAndÅrAndStønadstypeInAndIdGreaterThanOrderByIdAsc(
                Status.BEHANDLET,
                Behandlingstype.FATTET_FORSLAG,
                2026,
                forventedeStønadstyper,
                3,
                any(),
            )
        } returns emptyList()

        val reader = FattVedtakIndeksreguleringBidragBatchReader(indeksreguleringRepository, pageSize = 2)
        reader.beforeStep(stepExecution())

        val lest = generateSequence<Indeksregulering> { reader.read() }.map { it.id }.toList()

        lest shouldBe listOf(1, 2, 3)
        verify(exactly = 1) {
            indeksreguleringRepository.findAllByStatusAndBehandlingstypeAndVedtakIsNotNullAndÅrAndStønadstypeInAndIdGreaterThanOrderByIdAsc(
                Status.BEHANDLET,
                Behandlingstype.FATTET_FORSLAG,
                2026,
                forventedeStønadstyper,
                3,
                any(),
            )
        }
    }

    @Test
    fun `skal be om pageSize antall rader hver gang`() {
        every {
            indeksreguleringRepository.findAllByStatusAndBehandlingstypeAndVedtakIsNotNullAndÅrAndStønadstypeInAndIdGreaterThanOrderByIdAsc(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns emptyList()

        val reader = FattVedtakIndeksreguleringBidragBatchReader(indeksreguleringRepository, pageSize = 42)
        reader.beforeStep(stepExecution())
        reader.read()

        verify(exactly = 1) {
            indeksreguleringRepository.findAllByStatusAndBehandlingstypeAndVedtakIsNotNullAndÅrAndStønadstypeInAndIdGreaterThanOrderByIdAsc(
                Status.BEHANDLET,
                Behandlingstype.FATTET_FORSLAG,
                2026,
                forventedeStønadstyper,
                0,
                withArg<Pageable> { it.pageSize shouldBe 42 },
            )
        }
    }

    @Test
    fun `skal returnere null når det ikke finnes flere rader`() {
        every {
            indeksreguleringRepository.findAllByStatusAndBehandlingstypeAndVedtakIsNotNullAndÅrAndStønadstypeInAndIdGreaterThanOrderByIdAsc(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns emptyList()

        val reader = FattVedtakIndeksreguleringBidragBatchReader(indeksreguleringRepository, pageSize = 2)
        reader.beforeStep(stepExecution())

        reader.read().shouldBeNull()
    }

    @Test
    fun `skal bruke inneværende år når aar-jobparameter mangler`() {
        every {
            indeksreguleringRepository.findAllByStatusAndBehandlingstypeAndVedtakIsNotNullAndÅrAndStønadstypeInAndIdGreaterThanOrderByIdAsc(
                Status.BEHANDLET,
                Behandlingstype.FATTET_FORSLAG,
                Year.now().value,
                forventedeStønadstyper,
                0,
                any(),
            )
        } returns emptyList()

        val reader = FattVedtakIndeksreguleringBidragBatchReader(indeksreguleringRepository, pageSize = 2)
        reader.beforeStep(stepExecution(år = null))
        reader.read()

        verify(exactly = 1) {
            indeksreguleringRepository.findAllByStatusAndBehandlingstypeAndVedtakIsNotNullAndÅrAndStønadstypeInAndIdGreaterThanOrderByIdAsc(
                Status.BEHANDLET,
                Behandlingstype.FATTET_FORSLAG,
                Year.now().value,
                forventedeStønadstyper,
                0,
                any(),
            )
        }
    }

    @Test
    fun `regresjonstest - skal lese alle rader selv om status endres etter hver leste rad`() {
        // Simulerer den reelle databasespørringen: kun rader med status BEHANDLET, riktig stønadstype
        // og id større enn cursoren matcher. Prosessoren endrer status på hver rad før neste side blir
        // hentet - med den gamle offset-baserte pagineringen ble halvparten av radene da hoppet over.
        // Med keyset-paginering (id > sisteId) skal alle radene fortsatt bli lest.
        val alleRader = (1..5).map { indeksregulering(it) }

        every {
            indeksreguleringRepository.findAllByStatusAndBehandlingstypeAndVedtakIsNotNullAndÅrAndStønadstypeInAndIdGreaterThanOrderByIdAsc(
                Status.BEHANDLET,
                Behandlingstype.FATTET_FORSLAG,
                2026,
                forventedeStønadstyper,
                any(),
                any(),
            )
        } answers {
            val sisteId = arg<Int>(4)
            val pageable = arg<Pageable>(5)
            alleRader
                .filter { it.status == Status.BEHANDLET && (it.id ?: 0) > sisteId }
                .sortedBy { it.id }
                .take(pageable.pageSize)
        }

        val reader = FattVedtakIndeksreguleringBidragBatchReader(indeksreguleringRepository, pageSize = 2)
        reader.beforeStep(stepExecution())

        val lest = mutableListOf<Int?>()
        var neste = reader.read()
        while (neste != null) {
            lest.add(neste.id)
            // Simulerer at prosessoren fatter vedtaket og setter status til FATTET før neste side leses.
            neste.status = Status.FATTET
            neste = reader.read()
        }

        lest shouldBe listOf(1, 2, 3, 4, 5)
    }

    @Test
    fun `skal kun be repositoriet om rader med bidragsrelaterte stønadstyper`() {
        every {
            indeksreguleringRepository.findAllByStatusAndBehandlingstypeAndVedtakIsNotNullAndÅrAndStønadstypeInAndIdGreaterThanOrderByIdAsc(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns emptyList()

        val reader = FattVedtakIndeksreguleringBidragBatchReader(indeksreguleringRepository, pageSize = 2)
        reader.beforeStep(stepExecution())
        reader.read()

        verify(exactly = 1) {
            indeksreguleringRepository.findAllByStatusAndBehandlingstypeAndVedtakIsNotNullAndÅrAndStønadstypeInAndIdGreaterThanOrderByIdAsc(
                Status.BEHANDLET,
                Behandlingstype.FATTET_FORSLAG,
                2026,
                withArg<Collection<Stønadstype>> {
                    it shouldBe forventedeStønadstyper
                },
                0,
                any(),
            )
        }
    }
}
