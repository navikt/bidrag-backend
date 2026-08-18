package no.nav.bidrag.aktoerregister.batch.samhandler

import no.nav.bidrag.aktoerregister.batch.AktørBatchProcessorResult
import no.nav.bidrag.aktoerregister.batch.AktørBatchWriter
import no.nav.bidrag.aktoerregister.dto.enumer.Identtype
import no.nav.bidrag.aktoerregister.persistence.entities.Aktør
import no.nav.bidrag.aktoerregister.persistence.repository.AktørRepository
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.parameters.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.item.data.RepositoryItemReader
import org.springframework.batch.infrastructure.item.data.builder.RepositoryItemReaderBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class SamhandlerBatchConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val aktørBatchWriter: AktørBatchWriter,
    private val samhandlerBatchProcessor: SamhandlerBatchProcessor,
) {

    companion object {
        const val SAMHANDLER_BATCH_OPPDATERING_JOB = "SAMHANDLER_BATCH_OPPDATERING_JOB"
        const val SAMHANDLER_OPPDATER_AKTOERER_STEP = "SAMHANDLER_OPPDATER_AKTOERER_STEP"
    }

    @Bean
    fun samhandlerJob(samhandlerStep: Step): Job = JobBuilder(SAMHANDLER_BATCH_OPPDATERING_JOB, jobRepository)
        .listener(SamhandlerJobListener())
        .incrementer(RunIdIncrementer())
        .start(samhandlerStep)
        .build()

    @Bean
    fun samhandlerStep(samhandlerBatchReader: RepositoryItemReader<Aktør>): Step = StepBuilder(SAMHANDLER_OPPDATER_AKTOERER_STEP, jobRepository)
        .chunk<Aktør, AktørBatchProcessorResult>(100)
        .transactionManager(transactionManager)
        .reader(samhandlerBatchReader)
        .processor(samhandlerBatchProcessor)
        .writer(aktørBatchWriter)
        .build()

    @Bean
    fun samhandlerBatchReader(aktoerRepository: AktørRepository): RepositoryItemReader<Aktør> = RepositoryItemReaderBuilder<Aktør>()
        .name("samhandlerBatchReader")
        .repository(aktoerRepository)
        .methodName("findAllByAktørType")
        .arguments(listOf(Identtype.AKTOERNUMMER.name))
        .pageSize(100)
        .sorts(mapOf("aktørIdent" to Sort.Direction.ASC))
        .build()
}
