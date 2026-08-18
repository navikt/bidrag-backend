package no.nav.bidrag.aktoerregister.batch.samhandler

import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobOperator
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class SamhandlerBatch(
    private val jobOperator: JobOperator,
    @param:Qualifier("samhandlerJob") private val job: Job,
) {

    fun startSamhandlerBatch() {
        val jobParameters = JobParametersBuilder()
            .addString("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
            .toJobParameters()
        jobOperator.start(job, jobParameters)
    }
}
