package no.nav.bidrag.admin.service

import no.nav.bidrag.admin.consumer.BisysConsumer
import org.springframework.stereotype.Service

@Service
class BatchService(
    private val bisysConsumer: BisysConsumer,
) {
    fun getJobNames(): List<String> = bisysConsumer.getJobNames()

    fun launchJob(jobName: String): Long? = bisysConsumer.launchJob(jobName)

    fun runningExecutions(jobName: String): List<Long> = bisysConsumer.getRunningExecutions(jobName)

    fun stopExecution(executionId: String): Boolean? = bisysConsumer.stopExecution(executionId)

    fun getExecutionParameters(executionId: String): String? = bisysConsumer.exectionParameters(executionId)

    fun getExecutionSummary(executionId: String): String? = bisysConsumer.executionSummary(executionId)

    fun getExecutionSummaries(executionId: String): Map<Long, String> = bisysConsumer.executionSummaries(executionId)
}
