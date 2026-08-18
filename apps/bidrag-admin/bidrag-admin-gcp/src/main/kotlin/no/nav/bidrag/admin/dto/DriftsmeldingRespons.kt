package no.nav.bidrag.admin.dto

import no.nav.bidrag.admin.persistence.entity.Driftsmelding
import no.nav.bidrag.admin.persistence.entity.DriftsmeldingStatus
import no.nav.bidrag.commons.security.utils.TokenUtils
import java.time.LocalDateTime

data class DriftsmeldingDto(
    val id: Long,
    val tidspunkt: LocalDateTime,
    val tittel: String,
    val historikk: List<DriftsmeldingHistorikkDto>,
)

data class DriftsmeldingHistorikkDto(
    val id: Long,
    val tidspunkt: LocalDateTime,
    val innhold: String,
    val status: DriftsmeldingStatus,
    val erLestAvBruker: Boolean,
)

fun Driftsmelding.toDto() = DriftsmeldingDto(
    id = id ?: -1,
    tidspunkt = aktivFraTidspunkt ?: opprettetTidspunkt,
    tittel = tittel,
    historikk =
    historikk
        .sortedBy { it.aktivFraTidspunkt }
        .filter {
            it.aktivFraTidspunkt != null &&
                it.aktivFraTidspunkt!! <= LocalDateTime.now() &&
                (it.aktivTilTidspunkt == null || it.aktivTilTidspunkt!! > LocalDateTime.now())
        }.map {
            DriftsmeldingHistorikkDto(
                id = it.id ?: -1,
                tidspunkt = it.aktivFraTidspunkt ?: it.opprettetTidspunkt,
                innhold = it.innhold,
                status = it.status,
                erLestAvBruker =
                it.brukerLesinger.any { lesing -> lesing.person.navIdent == TokenUtils.hentSaksbehandlerIdent() },
            )
        },
)
