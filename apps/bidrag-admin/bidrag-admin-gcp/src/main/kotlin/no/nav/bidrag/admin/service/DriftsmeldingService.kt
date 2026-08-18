package no.nav.bidrag.admin.service

import no.nav.bidrag.admin.dto.LeggTilDriftsmeldingHistorikkRequest
import no.nav.bidrag.admin.dto.OppdaterDriftsmeldingHistorikkRequest
import no.nav.bidrag.admin.dto.OppdaterDriftsmeldingRequest
import no.nav.bidrag.admin.dto.OpprettDriftsmeldingRequest
import no.nav.bidrag.admin.persistence.entity.Driftsmelding
import no.nav.bidrag.admin.persistence.entity.DriftsmeldingHistorikk
import no.nav.bidrag.admin.persistence.entity.LestAvBruker
import no.nav.bidrag.admin.persistence.entity.Person
import no.nav.bidrag.admin.persistence.repository.DriftsmeldingRepository
import no.nav.bidrag.admin.persistence.repository.LestAvBrukerRepository
import no.nav.bidrag.admin.persistence.repository.Personrepository
import no.nav.bidrag.admin.utils.hentBrukerIdent
import no.nav.bidrag.admin.utils.hentBrukerNavn
import no.nav.bidrag.admin.utils.ugyldigForespørsel
import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.bidrag.commons.service.organisasjon.SaksbehandlernavnProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class DriftsmeldingService(
    private val driftsmeldingRepository: DriftsmeldingRepository,
    private val personrepository: Personrepository,
    private val lestAvBrukerRepository: LestAvBrukerRepository,
) {
    @Transactional
    fun hentAlleAktiv(): List<Driftsmelding> = driftsmeldingRepository.hentAlleAktiv()

    fun Driftsmelding.hentDriftsmeldingHistorikk(driftsmeldingHistorikkId: Long) = historikk.find { it.id == driftsmeldingHistorikkId }
        ?: ugyldigForespørsel(
            "DriftsmeldingHistorikk med id $driftsmeldingHistorikkId finnes ikke i driftsmelding $id",
        )

    fun hentDriftsmelding(driftsmeldingId: Long): Driftsmelding = driftsmeldingRepository
        .findById(
            driftsmeldingId,
        ).orElseThrow { ugyldigForespørsel("Driftsmelding med id $driftsmeldingId finnes ikke") }

    @Transactional
    fun leggTilDriftsmeldingHistorikk(
        endringsloggId: Long,
        request: LeggTilDriftsmeldingHistorikkRequest,
    ): Driftsmelding {
        val driftsmelding = hentDriftsmelding(endringsloggId)

        driftsmelding.historikk.add(
            DriftsmeldingHistorikk(
                innhold = request.innhold,
                driftsmelding = driftsmelding,
                aktivTilTidspunkt = request.aktivTilTidspunkt,
                aktivFraTidspunkt = request.aktivFraTidspunkt,
                status = request.status,
                opprettetAv = hentBrukerIdent(),
                opprettetAvNavn = hentBrukerNavn(),
            ),
        )
        return driftsmelding
    }

    @Transactional
    fun slettDriftsmeldingHistorikk(
        driftsmeldingId: Long,
        driftsmeldingHistorikkId: Long,
    ): Driftsmelding {
        val driftsmelding = hentDriftsmelding(driftsmeldingId)

        val historikk = driftsmelding.hentDriftsmeldingHistorikk(driftsmeldingHistorikkId)
        historikk.aktivTilTidspunkt = LocalDateTime.now().minusMinutes(1)
        return driftsmelding
    }

    @Transactional
    fun oppdaterLestAvBruker(
        driftsmeldingId: Long,
        driftsmeldingHistorikkId: Long,
    ) {
        val driftsmelding = hentDriftsmelding(driftsmeldingId)
        val drifsmeldingHistorikk = driftsmelding.hentDriftsmeldingHistorikk(driftsmeldingHistorikkId)
        val saksbehandlerIdent = TokenUtils.hentSaksbehandlerIdent()!!
        val person =
            personrepository.findByNavIdent(saksbehandlerIdent)
                ?: run {
                    personrepository.save(
                        Person(
                            navIdent = saksbehandlerIdent,
                            navn = SaksbehandlernavnProvider.hentSaksbehandlernavn(saksbehandlerIdent) ?: "",
                        ),
                    )
                }
        person.navn = SaksbehandlernavnProvider.hentSaksbehandlernavn(saksbehandlerIdent) ?: person.navn
        val lestAvBruker = lestAvBrukerRepository.findByPersonAndDriftsmeldingHistorikk(person, drifsmeldingHistorikk)
        if (lestAvBruker == null) {
            val lestAvBrukerNy =
                LestAvBruker(
                    person = person,
                    driftsmeldingHistorikk = drifsmeldingHistorikk,
                )
            drifsmeldingHistorikk.brukerLesinger.add(lestAvBrukerRepository.save(lestAvBrukerNy))
            driftsmeldingRepository.save(driftsmelding)
        }
    }

    @Transactional
    fun oppdaterDriftsmeldingHistorikk(
        driftsmeldingId: Long,
        driftsmeldingHistorikkId: Long,
        request: OppdaterDriftsmeldingHistorikkRequest,
    ): Driftsmelding {
        val driftsmelding = hentDriftsmelding(driftsmeldingId)
        val drifsmeldingHistorikk = driftsmelding.hentDriftsmeldingHistorikk(driftsmeldingHistorikkId)

        request.status?.let { drifsmeldingHistorikk.status = it }
        request.innhold?.let { drifsmeldingHistorikk.innhold = it }
        request.innhold?.let { drifsmeldingHistorikk.innhold = it }
        request.aktivFraTidspunkt?.let { driftsmelding.aktivFraTidspunkt = it }
        request.aktivTilTidspunkt?.let { driftsmelding.aktivTilTidspunkt = it }

        return driftsmelding
    }

    @Transactional
    fun oppdaterDriftsmelding(
        driftsmeldingId: Long,
        request: OppdaterDriftsmeldingRequest,
    ): Driftsmelding {
        val driftsmelding = hentDriftsmelding(driftsmeldingId)

        request.tittel?.let { driftsmelding.tittel = it }
        request.aktivFraTidspunkt?.let { driftsmelding.aktivFraTidspunkt = it }
        request.aktivTilTidspunkt?.let { driftsmelding.aktivTilTidspunkt = it }

        return driftsmelding
    }

    @Transactional
    fun opprettDriftsmelding(request: OpprettDriftsmeldingRequest): Driftsmelding {
        val driftsmelding =
            Driftsmelding(
                tittel = request.tittel,
                aktivFraTidspunkt = request.aktivFraTidspunkt,
                aktivTilTidspunkt = request.aktivTilTidspunkt,
                opprettetAv = hentBrukerIdent(),
                opprettetAvNavn = hentBrukerNavn(),
            )
        driftsmelding.historikk
            .filter { it.aktivFraTidspunkt != null && it.aktivFraTidspunkt!! < request.aktivFraTidspunkt }
            .maxByOrNull { it.aktivFraTidspunkt!! }
            ?.let {
                it.aktivTilTidspunkt = request.aktivFraTidspunkt
            }
        driftsmelding.historikk.add(
            DriftsmeldingHistorikk(
                innhold = request.innhold,
                status = request.status,
                driftsmelding = driftsmelding,
                aktivFraTidspunkt = request.aktivFraTidspunkt,
                aktivTilTidspunkt = request.aktivTilTidspunkt,
                opprettetAv = hentBrukerIdent(),
                opprettetAvNavn = hentBrukerNavn(),
            ),
        )
        return driftsmeldingRepository.save(driftsmelding)
    }

    @Transactional
    fun aktiverDriftsmelding(driftsmeldingId: Long): Driftsmelding {
        val driftsmelding = hentDriftsmelding(driftsmeldingId)

        driftsmelding.aktivFraTidspunkt = LocalDateTime.now()
        driftsmelding.aktivTilTidspunkt = null
        return driftsmelding
    }

    @Transactional
    fun deaktiverDriftsmelding(driftsmeldingId: Long): Driftsmelding {
        val driftsmelding = hentDriftsmelding(driftsmeldingId)

        driftsmelding.aktivTilTidspunkt = LocalDateTime.now()
        return driftsmelding
    }
}
