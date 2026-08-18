package no.nav.bidrag.sak.util

import jakarta.transaction.Transactional
import org.springframework.stereotype.Component

@Component
class TransactionHelper {
    @Transactional
    fun <T> transactional(block: () -> T): T = block()
}
