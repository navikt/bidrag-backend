package no.nav.bidrag.samhandler.persistence.repository

import no.nav.bidrag.samhandler.persistence.entity.AuditLog
import org.springframework.data.jpa.repository.JpaRepository

interface AuditLogRepository : JpaRepository<AuditLog, Int> {
    fun findAllByTabellNavnAndTabellId(
        tabellNavn: String,
        tabellId: Int,
    ): List<AuditLog>
}
