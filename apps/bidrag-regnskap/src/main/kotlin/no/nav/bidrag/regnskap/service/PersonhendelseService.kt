package no.nav.bidrag.regnskap.service

import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.ident.Ident
import no.nav.bidrag.regnskap.consumer.BidragPersonConsumer
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.transport.person.hendelse.Endringsmelding
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class PersonhendelseService(
    private val persistenceService: PersistenceService,
    private val bidragPersonConsumer: BidragPersonConsumer,
) {

    @Transactional
    fun behandlePersonhendelse(endringsmelding: Endringsmelding) {
        endringsmelding.endringer.forEach { endring ->
            val identendring = endring.identendring ?: return@forEach
            val identifikasjonsnummer = identendring.identifikasjonsnummer
            if (identifikasjonsnummer == null) {
                secureLogger.warn { "Mangler identifikasjonsnummer i endring fra personhendelse. Endring: $endring" }
                return@forEach
            }
            val nyeIdent = bidragPersonConsumer.hentPerson(Ident(identifikasjonsnummer))?.person?.ident?.verdi

            if (nyeIdent == null) {
                secureLogger.warn { "Person med ident $identifikasjonsnummer ble ikke funnet i bidrag-person" }
                return@forEach
            }

            endringsmelding.personidenter.forEach {
                oppdaterMottaker(it, nyeIdent)
                oppdaterKravhaver(it, nyeIdent)
                oppdaterSkyldner(it, nyeIdent)
                oppdaterGjelder(it, nyeIdent)
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun oppdaterGjelder(gamleIdent: String, nyeIdent: String) {
        persistenceService.hentAlleGjelderMedIdent(gamleIdent)
            .filter { gjelder -> gjelder.gjelderIdent != nyeIdent }
            .forEach { gjelder ->
                gjelder.gjelderIdent = nyeIdent
                logIdentendring(gamleIdent, nyeIdent, gjelder, "gjelder")
            }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun oppdaterSkyldner(gamleIdent: String, nyeIdent: String) {
        persistenceService.hentAlleSkyldnereMedIdent(gamleIdent)
            .filter { skyldner -> skyldner.skyldnerIdent != nyeIdent }
            .forEach { skyldner ->
                skyldner.skyldnerIdent = nyeIdent
                logIdentendring(gamleIdent, nyeIdent, skyldner, "skyldner")
            }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun oppdaterKravhaver(gamleIdent: String, nyeIdent: String) {
        persistenceService.hentAlleKravhavereMedIdent(gamleIdent)
            .filter { kravhaver -> kravhaver.kravhaverIdent != nyeIdent }
            .forEach { kravhaver ->
                kravhaver.kravhaverIdent = nyeIdent
                logIdentendring(gamleIdent, nyeIdent, kravhaver, "kravhaver")
            }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun oppdaterMottaker(gamleIdent: String, nyeIdent: String) {
        persistenceService.hentAlleMottakereMedIdent(gamleIdent)
            .filter { mottaker -> mottaker.mottakerIdent != nyeIdent }
            .forEach { mottaker ->
                mottaker.mottakerIdent = nyeIdent
                logIdentendring(gamleIdent, nyeIdent, mottaker, "mottaker")
            }
    }

    fun logIdentendring(
        gamleIdent: String,
        nyeIdent: String,
        oppdrag: Oppdrag,
        type: String,
    ) {
        secureLogger.info { "Personhendelse: $type $gamleIdent oppdatert til $nyeIdent for oppdragId: ${oppdrag.oppdragId}" }
    }
}
