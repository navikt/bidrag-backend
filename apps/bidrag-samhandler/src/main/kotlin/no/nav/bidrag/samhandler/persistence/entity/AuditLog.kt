package no.nav.bidrag.samhandler.persistence.entity

import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import org.hibernate.annotations.Type
import java.time.LocalDateTime

@Entity(name = "audit_log")
data class AuditLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,
    val tabellNavn: String = "",
    val tabellId: Int = 0,
    val operasjon: String = "",
    val endretTidspunkt: LocalDateTime = LocalDateTime.now(),
    val endretAv: String = "",
    @Type(JsonType::class)
    val gamleVerdier: String = "",
    @Type(JsonType::class)
    val nyeVerdier: String = "",
)
