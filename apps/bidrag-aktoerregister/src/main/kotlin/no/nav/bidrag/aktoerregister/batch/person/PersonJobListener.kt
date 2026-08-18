package no.nav.bidrag.aktoerregister.batch.person

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.batch.core.annotation.AfterJob
import org.springframework.batch.core.job.JobExecution

private val LOGGER = KotlinLogging.logger { }

class PersonJobListener {
    @AfterJob
    fun afterJob(jobExecution: JobExecution) {
        val stepExecution = jobExecution.stepExecutions.first { it.stepName == PersonBatchConfig.PERSON_OPPDATER_AKTOERER_STEP }
        val readCount = stepExecution.readCount
        val writeCount = stepExecution.writeCount

        LOGGER.info {
            "Av totalt $readCount person-aktører var $writeCount blitt endret siden forrige kjøring. " +
                "$writeCount aktører ble dermed oppdatert i databasen."
        }
    }
}
