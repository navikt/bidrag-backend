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

            var fantMottaker = false
            var fantKravhaver = false
            var fantSkyldner = false
            var fantGjelder = false
            endringsmelding.personidenter.forEach { ident ->
                if (oppdaterMottaker(ident, nyeIdent)) fantMottaker = true
                if (oppdaterKravhaver(ident, nyeIdent)) fantKravhaver = true
                if (oppdaterSkyldner(ident, nyeIdent)) fantSkyldner = true
                if (oppdaterGjelder(ident, nyeIdent)) fantGjelder = true
            }

            tidligereIdenter.forEach { ident ->
                if (!fantMottaker) oppdaterMottaker(ident, nyeIdent)
                if (!fantKravhaver) oppdaterKravhaver(ident, nyeIdent)
                if (!fantSkyldner) oppdaterSkyldner(ident, nyeIdent)
                if (!fantGjelder) oppdaterGjelder(ident, nyeIdent)
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun oppdaterGjelder(gamleIdent: String, nyeIdent: String): Boolean {
        val gjeldere = persistenceService.hentAlleGjelderMedIdent(gamleIdent)
        gjeldere
            .filter { gjelder -> gjelder.gjelderIdent != nyeIdent }
            .forEach { gjelder ->
                gjelder.gjelderIdent = nyeIdent
                logIdentendring(gamleIdent, nyeIdent, gjelder, "gjelder")
            }
        return gjeldere.isNotEmpty()
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun oppdaterSkyldner(gamleIdent: String, nyeIdent: String): Boolean {
        val skyldnere = persistenceService.hentAlleSkyldnereMedIdent(gamleIdent)
        skyldnere
            .filter { skyldner -> skyldner.skyldnerIdent != nyeIdent }
            .forEach { skyldner ->
                skyldner.skyldnerIdent = nyeIdent
                logIdentendring(gamleIdent, nyeIdent, skyldner, "skyldner")
            }
        return skyldnere.isNotEmpty()
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun oppdaterKravhaver(gamleIdent: String, nyeIdent: String): Boolean {
        val kravhavere = persistenceService.hentAlleKravhavereMedIdent(gamleIdent)
        kravhavere
            .filter { kravhaver -> kravhaver.kravhaverIdent != nyeIdent }
            .forEach { kravhaver ->
                kravhaver.kravhaverIdent = nyeIdent
                logIdentendring(gamleIdent, nyeIdent, kravhaver, "kravhaver")
            }
        return kravhavere.isNotEmpty()
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun oppdaterMottaker(gamleIdent: String, nyeIdent: String): Boolean {
        val mottakere = persistenceService.hentAlleMottakereMedIdent(gamleIdent)
        mottakere
            .filter { mottaker -> mottaker.mottakerIdent != nyeIdent }
            .forEach { mottaker ->
                mottaker.mottakerIdent = nyeIdent
                logIdentendring(gamleIdent, nyeIdent, mottaker, "mottaker")
            }
        return mottakere.isNotEmpty()
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
