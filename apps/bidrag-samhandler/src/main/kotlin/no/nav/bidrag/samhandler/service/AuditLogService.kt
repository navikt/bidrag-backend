package no.nav.bidrag.samhandler.service

import no.nav.bidrag.samhandler.mapper.AuditLogMapper
import no.nav.bidrag.samhandler.persistence.repository.AuditLogRepository
import no.nav.bidrag.transport.felles.AuditLogDto
import org.springframework.stereotype.Service

@Service
class AuditLogService(
    private val auditLogRepository: AuditLogRepository,
) {
    fun hentAuditLogForSamhandler(samhandlerId: Int): List<AuditLogDto> {
        val auditLog = auditLogRepository.findAllByTabellNavnAndTabellId("samhandlere", samhandlerId)
        return auditLog.map { AuditLogMapper.mapTilAuditLogDto(it) }
    }
}
