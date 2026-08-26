package no.nav.bidrag.commons.service

import no.nav.bidrag.commons.util.LoggingRetryListener
import org.springframework.retry.backoff.ExponentialBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate

/**
 * Retry template for synkrone kall
 */
fun retryTemplateSynchronous(details: String? = null): RetryTemplate {
    val retryTemplate = RetryTemplate()
    val exponentialBackOffPolicy = ExponentialBackOffPolicy()
    exponentialBackOffPolicy.multiplier = 2.0
    exponentialBackOffPolicy.maxInterval = 1000 // 1 sekund
    exponentialBackOffPolicy.initialInterval = 200
    retryTemplate.setBackOffPolicy(exponentialBackOffPolicy)
    val retryPolicy = SimpleRetryPolicy()
    retryPolicy.maxAttempts = 3
    retryTemplate.setRetryPolicy(retryPolicy)
    retryTemplate.setThrowLastExceptionOnExhausted(true)
    retryTemplate.registerListener(LoggingRetryListener(details))
    return retryTemplate
}
