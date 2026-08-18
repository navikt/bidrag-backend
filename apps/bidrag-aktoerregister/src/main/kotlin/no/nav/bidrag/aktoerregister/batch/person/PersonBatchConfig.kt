package no.nav.bidrag.aktoerregister.batch.person

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
class PersonBatchConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val aktørBatchWriter: AktørBatchWriter,
    private val personBatchProcessor: PersonBatchProcessor,
) {

    companion object {
        const val PERSON_BATCH_OPPDATERING_JOB = "PERSON_BATCH_OPPDATERING_JOB"
        const val PERSON_OPPDATER_AKTOERER_STEP = "PERSON_OPPDATER_AKTOERER_STEP"
    }

    @Bean
    fun personJob(personStep: Step): Job = JobBuilder(PERSON_BATCH_OPPDATERING_JOB, jobRepository)
        .listener(PersonJobListener())
        .incrementer(RunIdIncrementer())
        .start(personStep)
        .build()

    @Bean
    fun personStep(personBatchReader: RepositoryItemReader<Aktør>): Step = StepBuilder(PERSON_OPPDATER_AKTOERER_STEP, jobRepository)
        .chunk<Aktør, AktørBatchProcessorResult>(100)
        .transactionManager(transactionManager)
        .reader(personBatchReader)
        .processor(personBatchProcessor)
        .writer(aktørBatchWriter)
        .build()

    @Bean
    fun personBatchReader(aktoerRepository: AktørRepository): RepositoryItemReader<Aktør> = RepositoryItemReaderBuilder<Aktør>()
        .name("personBatchReader")
        .repository(aktoerRepository)
        .methodName("findAllByAktørType")
        .arguments(listOf(Identtype.PERSONNUMMER.name))
        .pageSize(100)
        .sorts(mapOf("aktørIdent" to Sort.Direction.ASC))
        .build()
}
