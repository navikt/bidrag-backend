package no.nav.bidrag.samhandler.mapper

import no.nav.bidrag.samhandler.persistence.entity.AuditLog
import no.nav.bidrag.transport.felles.AuditLogDto

object AuditLogMapper {
    fun mapTilAuditLogDto(auditLog: AuditLog): AuditLogDto = AuditLogDto(
        tabellNavn = auditLog.tabellNavn,
        tabellId = auditLog.tabellId,
        operasjon = auditLog.operasjon,
        endretTidspunkt = auditLog.endretTidspunkt,
        endretAv = auditLog.endretAv,
        gamleVerdier = auditLog.gamleVerdier,
        nyeVerdier = auditLog.nyeVerdier,
    )
}
