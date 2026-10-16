package no.nav.bidrag.regnskap.service

import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.ident.Ident
import no.nav.bidrag.regnskap.consumer.BidragPersonConsumer
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.transport.felles.ifFalse
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
            val person = bidragPersonConsumer.hentPerson(Ident(identifikasjonsnummer))
            if (person == null) {
                secureLogger.warn { "Person med ident $identifikasjonsnummer ble ikke funnet i bidrag-person" }
                return@forEach
            }
            val nyeIdent = person.person.ident.verdi

            val tidligereIdenter = person.tidligereIdenter.orEmpty()
                .map { it.verdi }
                .filter { it != nyeIdent && it !in endringsmelding.personidenter }
                .distinct()

            // Oppdater mottaker
            endringsmelding.personidenter
                .map { ident -> oppdaterMottaker(ident, nyeIdent) }
                .any { it }
                .ifFalse {
                    tidligereIdenter.forEach { ident -> oppdaterMottaker(ident, nyeIdent) }
                }

            // Oppdater kravhaver
            endringsmelding.personidenter
                .map { ident -> oppdaterKravhaver(ident, nyeIdent) }
                .any { it }
                .ifFalse {
                    tidligereIdenter.forEach { ident -> oppdaterKravhaver(ident, nyeIdent) }
                }

            // Oppdater skyldner
            endringsmelding.personidenter
                .map { ident -> oppdaterSkyldner(ident, nyeIdent) }
                .any { it }
                .ifFalse {
                    tidligereIdenter.forEach { ident -> oppdaterSkyldner(ident, nyeIdent) }
                }

            // Oppdater gjelder
            endringsmelding.personidenter
                .map { ident -> oppdaterGjelder(ident, nyeIdent) }
                .any { it }
                .ifFalse {
                    tidligereIdenter.forEach { ident -> oppdaterGjelder(ident, nyeIdent) }
                }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun oppdaterGjelder(gamleIdent: String, nyeIdent: String): Boolean {
        val oppdrag = persistenceService.hentAlleGjelderMedIdent(gamleIdent)
        oppdrag
            .filter { gjelder -> gjelder.gjelderIdent != nyeIdent }
            .forEach { gjelder ->
                gjelder.gjelderIdent = nyeIdent
                logIdentendring(gamleIdent, nyeIdent, gjelder, "gjelder")
            }
        return oppdrag.isNotEmpty()
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun oppdaterSkyldner(gamleIdent: String, nyeIdent: String): Boolean {
        val oppdrag = persistenceService.hentAlleSkyldnereMedIdent(gamleIdent)
        oppdrag
            .filter { skyldner -> skyldner.skyldnerIdent != nyeIdent }
            .forEach { skyldner ->
                skyldner.skyldnerIdent = nyeIdent
                logIdentendring(gamleIdent, nyeIdent, skyldner, "skyldner")
            }
        return oppdrag.isNotEmpty()
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun oppdaterKravhaver(gamleIdent: String, nyeIdent: String): Boolean {
        val oppdrag = persistenceService.hentAlleKravhavereMedIdent(gamleIdent)
        oppdrag
            .filter { kravhaver -> kravhaver.kravhaverIdent != nyeIdent }
            .forEach { kravhaver ->
                kravhaver.kravhaverIdent = nyeIdent
                logIdentendring(gamleIdent, nyeIdent, kravhaver, "kravhaver")
            }
        return oppdrag.isNotEmpty()
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun oppdaterMottaker(gamleIdent: String, nyeIdent: String): Boolean {
        val oppdrag = persistenceService.hentAlleMottakereMedIdent(gamleIdent)
        oppdrag
            .filter { mottaker -> mottaker.mottakerIdent != nyeIdent }
            .forEach { mottaker ->
                mottaker.mottakerIdent = nyeIdent
                logIdentendring(gamleIdent, nyeIdent, mottaker, "mottaker")
            }
        return oppdrag.isNotEmpty()
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
