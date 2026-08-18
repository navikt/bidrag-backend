package no.nav.bidrag.regnskap.service

import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.regnskap.behandlingsstatus.Batchstatus
import no.nav.bidrag.regnskap.consumer.SkattConsumer
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.transport.regnskap.behandlingsstatus.BehandlingsstatusResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class BehandlingsstatusService(
    private val skattConsumer: SkattConsumer,
    private val persistenceService: PersistenceService,
    private val reskontroService: ReskontroService,
) {

    fun hentKonteringerMedIkkeGodkjentBehandlingsstatus(sisteReferanser: List<String> = emptyList()): Map<String, Set<Kontering>> = hentKonteringer(sisteReferanser)
        .groupBy { it.sisteReferansekode!! }
        .mapValues { it.value.toSet() }

    @Transactional
    fun hentBehandlingsstatusForIkkeGodkjenteKonteringer(
        konteringerSomIkkeHarFåttGodkjentBehandlingsstatus: Map<String, Set<Kontering>>,
    ): Map<String, String> {
        val feilmeldinger = hashMapOf<String, String>()
        konteringerSomIkkeHarFåttGodkjentBehandlingsstatus.forEach { (batchUid, _) ->
            behandleBatchUid(batchUid)?.let { feilmeldinger[batchUid] = it }
        }
        return feilmeldinger
    }

    // Hver batchUid behandles og committes i sin egen transaksjon, slik at allerede ferdigbehandlede
    // batcher beholdes selv om jobben avbrytes (krasj eller lås-timeout) før alle er ferdige.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun behandleBatchUid(batchUid: String): String? {
        val managedKonteringer = persistenceService.konteringRepository.findAllBySisteReferansekode(batchUid).toSet()
        try {
            val behandlingsstatusResponse = hentBehandlingsstatus(batchUid)

            if (behandlingsstatusResponse.batchStatus == Batchstatus.Done) {
                val feilmeldingFraReskontro = reskontroService.sammenlignOversendteKonteringerMedReskontro(mapOf(batchUid to managedKonteringer))
                if (feilmeldingFraReskontro.isNotEmpty()) {
                    secureLogger.error { "Det finnes avvik mellom oversendte konteringer som har fått DONE status fra skatt og det som ligger i reskontro for batchUid $batchUid: $feilmeldingFraReskontro" }
                }
                behandleVellykkedeKonteringer(managedKonteringer)
                return null
            }
            settFeiledeKonteringerForAlleOppdragKnyttetTilKonteringer(managedKonteringer, true)
            return "Behandling av konteringer for batchuid $batchUid har feilet: $behandlingsstatusResponse\n"
        } catch (e: Exception) {
            settFeiledeKonteringerForAlleOppdragKnyttetTilKonteringer(managedKonteringer, true)
            return "Behandling av konteringer for batchuid $batchUid har feilet og kastet exception!: ${e.message}\n"
        }
    }

    fun behandleVellykkedeKonteringer(
        konteringer: Set<Kontering>,
    ) {
        val now = LocalDateTime.now()
        konteringer.forEach {
            it.behandlingsstatusOkTidspunkt = now
        }

        // Om alle konteringer knyttet til alle oppdragsperiodene i oppdraget er oversendt ok
        // så kan vi garantere at det ikke finnes feilede konteringer
        konteringer.firstOrNull()?.let { kontering ->
            if (erAlleKonteringerIOppdragOk(kontering)) {
                settFeiledeKonteringerForAlleOppdragKnyttetTilKonteringer(konteringer, false)
            }
        }
    }

    private fun settFeiledeKonteringerForAlleOppdragKnyttetTilKonteringer(konteringer: Set<Kontering>, harFeiledeKonteringer: Boolean) {
        konteringer.forEach { kontering -> kontering.oppdragsperiode!!.oppdrag!!.harFeiledeKonteringer = harFeiledeKonteringer }
    }

    private fun erAlleKonteringerIOppdragOk(kontering: Kontering): Boolean {
        val oppdragsperioder = kontering.oppdragsperiode!!.oppdrag!!.oppdragsperioder
        return oppdragsperioder
            .flatMap { it.konteringer }
            .none { it.behandlingsstatusOkTidspunkt == null }
    }

    @Transactional
    fun hentBehandlingsstatusForIkkeGodkjenteKonteringerForReferansekode(sisteReferanser: List<String>): Map<String, String> = hentBehandlingsstatusForIkkeGodkjenteKonteringer(hentKonteringerMedIkkeGodkjentBehandlingsstatus(sisteReferanser))

    private fun hentKonteringer(sisteReferanser: List<String>): List<Kontering> = if (sisteReferanser.isEmpty()) {
        persistenceService.hentAlleKonteringerUtenBehandlingsstatusOk()
    } else {
        persistenceService.hentKonteringerUtenBehandlingsstatusOkForReferansekode(sisteReferanser)
    }

    private fun hentBehandlingsstatus(batchUid: String): BehandlingsstatusResponse {
        val behandlingsstatus = skattConsumer.sjekkBehandlingsstatus(batchUid).body
        return behandlingsstatus
            ?: error("Sjekk av behandlingsstatus feilet for batchUid: $batchUid!")
    }
}
