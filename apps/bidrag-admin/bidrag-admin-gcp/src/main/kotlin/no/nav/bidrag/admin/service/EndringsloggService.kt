package no.nav.bidrag.admin.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.admin.dto.LeggTilEndringsloggEndring
import no.nav.bidrag.admin.dto.LestAvBrukerRequest
import no.nav.bidrag.admin.dto.OppdaterEndringsloggRequest
import no.nav.bidrag.admin.dto.OpprettEndringsloggRequest
import no.nav.bidrag.admin.persistence.entity.Endringslogg
import no.nav.bidrag.admin.persistence.entity.EndringsloggEndring
import no.nav.bidrag.admin.persistence.entity.EndringsloggTilhørerSkjermbilde
import no.nav.bidrag.admin.persistence.entity.Endringstype
import no.nav.bidrag.admin.persistence.entity.LestAvBruker
import no.nav.bidrag.admin.persistence.entity.Person
import no.nav.bidrag.admin.persistence.repository.EndringsloggRepository
import no.nav.bidrag.admin.persistence.repository.LestAvBrukerRepository
import no.nav.bidrag.admin.persistence.repository.Personrepository
import no.nav.bidrag.admin.utils.hentBrukerIdent
import no.nav.bidrag.admin.utils.hentBrukerNavn
import no.nav.bidrag.admin.utils.ugyldigForespørsel
import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.bidrag.commons.service.organisasjon.SaksbehandlernavnProvider
import no.nav.bidrag.commons.util.secureLogger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

@Service
class EndringsloggService(
    private val endringsloggRepository: EndringsloggRepository,
    private val personrepository: Personrepository,
    private val lestAvBrukerRepository: LestAvBrukerRepository,
) {
    val EndringsloggTilhørerSkjermbilde?.tilTyper
        get() =
            when (this) {
                EndringsloggTilhørerSkjermbilde.BEHANDLING_ALLE -> {
                    listOf(
                        EndringsloggTilhørerSkjermbilde.BEHANDLING_BIDRAG,
                        EndringsloggTilhørerSkjermbilde.BEHANDLING_FORSKUDD,
                        EndringsloggTilhørerSkjermbilde.BEHANDLING_SÆRBIDRAG,
                        EndringsloggTilhørerSkjermbilde.BEHANDLING_ALLE,
                        EndringsloggTilhørerSkjermbilde.ALLE,
                    )
                }

                EndringsloggTilhørerSkjermbilde.BEHANDLING_SÆRBIDRAG -> {
                    listOf(
                        EndringsloggTilhørerSkjermbilde.BEHANDLING_SÆRBIDRAG,
                        EndringsloggTilhørerSkjermbilde.BEHANDLING_ALLE,
                        EndringsloggTilhørerSkjermbilde.ALLE,
                    )
                }

                EndringsloggTilhørerSkjermbilde.BEHANDLING_FORSKUDD -> {
                    listOf(
                        EndringsloggTilhørerSkjermbilde.BEHANDLING_FORSKUDD,
                        EndringsloggTilhørerSkjermbilde.BEHANDLING_ALLE,
                        EndringsloggTilhørerSkjermbilde.ALLE,
                    )
                }

                EndringsloggTilhørerSkjermbilde.BEHANDLING_BIDRAG -> {
                    listOf(
                        EndringsloggTilhørerSkjermbilde.BEHANDLING_BIDRAG,
                        EndringsloggTilhørerSkjermbilde.BEHANDLING_ALLE,
                        EndringsloggTilhørerSkjermbilde.ALLE,
                    )
                }

                null, EndringsloggTilhørerSkjermbilde.ALLE -> {
                    emptyList()
                }

                else -> {
                    listOf(this, EndringsloggTilhørerSkjermbilde.ALLE)
                }
            }

    @Transactional
    fun hentAlleForType(
        type: EndringsloggTilhørerSkjermbilde?,
        bareAktive: Boolean,
    ): List<Endringslogg> {
        val endringer =
            if (bareAktive) {
                endringsloggRepository.findAllAktiveByTilhørerSkjermbilde(type.tilTyper)
            } else {
                endringsloggRepository.findAllByTilhørerSkjermbilde(type.tilTyper)
            }
        endringer.forEach {
            oppdaterLestAvEndringslogg(it)
        }
        secureLogger.info { "Hentet endringslogg for type $type: $endringer" }
        return endringer
    }

    fun slettEndringslogg(endringsloggId: Long) = endringsloggRepository
        .deleteById(
            endringsloggId,
        )

    @Transactional
    fun hentEndringslogg(endringsloggId: Long): Endringslogg {
        val endringslogg =
            endringsloggRepository
                .findById(
                    endringsloggId,
                ).orElseThrow { ugyldigForespørsel("Endringslogg med id $endringsloggId finnes ikke") }
        oppdaterLestAvEndringslogg(endringslogg)
        return endringslogg
    }

    private fun oppdaterLestAvEndringslogg(endringslogg: Endringslogg) {
        val innloggetPerson = hentPerson()
        endringslogg.brukerLesinger = mutableSetOf()
        lestAvBrukerRepository.findByPersonAndEndringsloggAndEndringsloggEndringIsNull(innloggetPerson, endringslogg)?.let {
            endringslogg.brukerLesinger.add(it)
        }
        endringslogg.endringer.forEach {
            it.brukerLesinger = mutableSetOf()
            lestAvBrukerRepository.findByPersonAndEndringsloggEndring(innloggetPerson, it)?.let { lestAvBruker ->
                it.brukerLesinger.add(lestAvBruker)
            }
        }
    }

    fun hentPerson(enhet: String? = null): Person {
        val saksbehandlerIdent = TokenUtils.hentSaksbehandlerIdent()!!
        val person =
            personrepository.findByNavIdent(saksbehandlerIdent)
                ?: run {
                    personrepository.save(
                        Person(
                            enhet = enhet,
                            navIdent = saksbehandlerIdent,
                            navn = SaksbehandlernavnProvider.hentSaksbehandlernavn(saksbehandlerIdent) ?: "",
                        ),
                    )
                }
        person.navn = SaksbehandlernavnProvider.hentSaksbehandlernavn(saksbehandlerIdent) ?: person.navn
        return person
    }

    @Transactional
    fun oppdaterLestAvBruker(
        endringsloggId: Long,
        request: LestAvBrukerRequest,
    ): Endringslogg {
        log.info {
            "Oppdaterer lest av bruker for endringslogg $endringsloggId og saksbehandler ${TokenUtils.hentSaksbehandlerIdent()}"
        }
        val endringslogg = hentEndringslogg(endringsloggId)
        val person = hentPerson(request.enhet)
        val eksisterendeLestAvBruker = lestAvBrukerRepository.findByPersonAndEndringsloggAndEndringsloggEndringIsNull(person, endringslogg)

        if (eksisterendeLestAvBruker != null) {
            log.info { "Bruker har allerede lest endringslogg $endringsloggId" }
            return endringslogg
        } else {
            val totalVarighet = lestAvBrukerRepository.sumLesetidVarighetMsByEndringslogg(endringslogg, person.id!!) ?: 0
            val lestAvBrukerNy =
                LestAvBruker(
                    person = person,
                    endringslogg = endringslogg,
                    lestetidVarighetMs = totalVarighet,
                )
            lestAvBrukerNy.lestTidspunkt = LocalDateTime.now()
            lestAvBrukerRepository.save(lestAvBrukerNy)
        }
        log.info {
            "Oppdaterte lest av bruker for endringslogg $endringsloggId og saksbehandler ${TokenUtils.hentSaksbehandlerIdent()}"
        }
        return endringslogg
    }

    private fun Endringslogg.hentEndring(endringsloggEndringId: Long) = endringer.find { it.id == endringsloggEndringId }
        ?: ugyldigForespørsel("Endringslogg endring med id $endringsloggEndringId finnes ikke i endringslogg med id $id")

    @Transactional
    fun oppdaterEndringslogg(
        endringsloggId: Long,
        request: OppdaterEndringsloggRequest,
    ): Endringslogg {
        log.info { "Oppdaterer endringslogg $endringsloggId med $request" }
        val endringslogg = hentEndringslogg(endringsloggId)

        endringslogg.tilhørerSkjermbilde = request.tilhørerSkjermbilde ?: endringslogg.tilhørerSkjermbilde
        endringslogg.erPåkrevd = request.erPåkrevd ?: endringslogg.erPåkrevd
        endringslogg.tittel = request.tittel ?: endringslogg.tittel
        endringslogg.sammendrag = request.sammendrag ?: endringslogg.sammendrag
        if (request.endringer != null) {
            val nyeEndringer =
                request.endringer
                    .mapIndexed { index, endringRequest ->
                        endringslogg.endringer.find { e -> e.id == endringRequest.id }?.let {
                            it.tittel = endringRequest.tittel ?: it.tittel
                            it.innhold = endringRequest.innhold ?: it.innhold
                            it.rekkefølgeIndeks = index
                            it.endringstype = endringRequest.endringstype ?: it.endringstype
                            it
                        } ?: EndringsloggEndring(
                            innhold = endringRequest.innhold ?: "",
                            tittel = endringRequest.tittel ?: "",
                            endringslogg = endringslogg,
                            endringstype = endringRequest.endringstype ?: Endringstype.ENDRING,
                            rekkefølgeIndeks = index,
                        )
                    }
            endringslogg.endringer.clear()
            endringslogg.endringer.addAll(nyeEndringer)
        }
//        }
        return endringslogg
    }

    @Transactional
    fun aktiverEndringslogg(endringsloggId: Long): Endringslogg {
        log.info { "Aktiverer enddringslogg $endringsloggId" }
        val endringslogg = hentEndringslogg(endringsloggId)

        endringslogg.aktivFraTidspunkt = LocalDateTime.now()
        endringslogg.aktivTilTidspunkt = null
        return endringslogg
    }

    @Transactional
    fun deaktiverEndringslogg(endringsloggId: Long): Endringslogg {
        log.info { "Deaktiverer enddringslogg $endringsloggId" }
        val endringslogg = hentEndringslogg(endringsloggId)

        endringslogg.aktivTilTidspunkt = LocalDateTime.now()
        return endringslogg
    }

    @Transactional
    fun opprettEndringslogg(request: OpprettEndringsloggRequest): Endringslogg {
        val endringsLogg =
            Endringslogg(
                tittel = request.tittel,
                tilhørerSkjermbilde = request.tilhørerSkjermbilde,
                sammendrag = request.sammendrag,
                erPåkrevd = request.erPåkrevd,
                opprettetAv = hentBrukerIdent(),
                opprettetAvNavn = hentBrukerNavn(),
            )
        request.endringer?.forEachIndexed { index, endringRequest ->
            endringsLogg.endringer.add(
                EndringsloggEndring(
                    endringslogg = endringsLogg,
                    tittel = endringRequest.tittel,
                    innhold = endringRequest.innhold,
                    rekkefølgeIndeks = index,
                    endringstype = endringRequest.endringstype,
                ),
            )
        }
        val endringslogg = endringsloggRepository.save(endringsLogg)
        log.info { "Opprettet enddringslogg $endringslogg" }
        return endringslogg
    }

    @Transactional
    fun oppdaterLestAvBrukerEndring(
        endringsloggId: Long,
        endringsloggEndringId: Long,
        request: LestAvBrukerRequest,
    ): Endringslogg {
        log.info {
            "Oppdaterer lest av bruker for endringslogg endring $endringsloggEndringId i endringslogg $endringsloggId og saksbehandler ${TokenUtils.hentSaksbehandlerIdent()}"
        }
        val endringslogg = hentEndringslogg(endringsloggId)
        val saksbehandlerIdent = TokenUtils.hentSaksbehandlerIdent()!!
        val person =
            personrepository.findByNavIdent(saksbehandlerIdent)
                ?: run {
                    personrepository.save(
                        Person(
                            enhet = request.enhet,
                            navIdent = saksbehandlerIdent,
                            navn = SaksbehandlernavnProvider.hentSaksbehandlernavn(saksbehandlerIdent) ?: "",
                        ),
                    )
                }
        person.navn = SaksbehandlernavnProvider.hentSaksbehandlernavn(saksbehandlerIdent) ?: person.navn
        val endringsloggEndring = endringslogg.hentEndring(endringsloggEndringId)
        val eksisterendLestAvBruker = lestAvBrukerRepository.findByPersonAndEndringsloggEndring(person, endringsloggEndring)
        if (eksisterendLestAvBruker != null) {
            log.info { "Bruker har allerede lest endringslogg endring $endringsloggEndringId i endring $endringsloggId" }
            eksisterendLestAvBruker.lestetidVarighetMs = maxOf(eksisterendLestAvBruker.lestetidVarighetMs ?: 0, request.lesetidVarighetMs)
            endringslogg
        } else {
            val lestAvBrukerNy =
                LestAvBruker(
                    person = person,
                    endringsloggEndring = endringsloggEndring,
                    endringslogg = endringslogg,
                    lestetidVarighetMs = request.lesetidVarighetMs,
                )
            endringsloggEndring.brukerLesinger.add(lestAvBrukerRepository.save(lestAvBrukerNy))
            lestAvBrukerNy.lestTidspunkt = LocalDateTime.now()
            endringsloggRepository.save(endringslogg)
            log.info {
                "Oppdaterte lest av bruker for endringslogg endring ${endringsloggEndring.id} i endringslogg $endringsloggId og saksbehandler ${TokenUtils.hentSaksbehandlerIdent()}"
            }
        }

        if (endringslogg.erAlleLestAvBruker) {
            oppdaterLestAvBruker(endringsloggId, request)
        }
        return endringslogg
    }

    @Transactional
    fun slettEndring(
        endringsloggId: Long,
        endringId: Long,
    ): Endringslogg {
        log.info { "Sletter endring $endringId fra endringslogg $endringsloggId" }

        val endringslogg = hentEndringslogg(endringsloggId)

        if (endringslogg.endringer.size == 1) ugyldigForespørsel("Kan ikke slette eneste endring i endringslogg $endringsloggId")

        val endring =
            endringslogg.endringer.find { it.id == endringId }
                ?: ugyldigForespørsel("Endringslogg endring med id $endringId finnes ikke i endringslogg med id $endringsloggId")
        endringslogg.endringer.remove(endring)
        synkroniserRekkefølgeIndeks(endringslogg)
        return endringslogg
    }

    private fun synkroniserRekkefølgeIndeks(endringslogg: Endringslogg) {
        endringslogg.endringer
            .sortedBy { it.rekkefølgeIndeks }
            .forEachIndexed { index, endringsloggEndring ->
                endringsloggEndring.rekkefølgeIndeks = index
            }
    }

    @Transactional
    fun leggTilEndringsloggEndring(
        endringsloggId: Long,
        request: LeggTilEndringsloggEndring,
    ): Endringslogg {
        log.info { "Legger til endring $request i endringslogg $endringsloggId" }

        val endringslogg = hentEndringslogg(endringsloggId)

        val sisteRekkefølgeIndeks = endringslogg.endringer.size
        endringslogg.endringer.add(
            EndringsloggEndring(
                innhold = request.innhold,
                tittel = request.tittel,
                endringslogg = endringslogg,
                rekkefølgeIndeks = sisteRekkefølgeIndeks + 1,
            ),
        )
        return endringslogg
    }
}
