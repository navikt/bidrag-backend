package no.nav.bidrag.dokument.bestilling.tjenester

import no.nav.bidrag.dokument.bestilling.consumer.KodeverkConsumer
import org.springframework.stereotype.Service

@Service
class KodeverkService(
    val kodeverkConsumer: KodeverkConsumer,
) {
    fun hentLandFullnavnForKode(landkode: String): String? = hentLandFullnavnForKodeISO3(landkode) ?: hentLandFullnavnForKodeISO2(landkode)

    fun hentLandFullnavnForKodeISO3(landkode: String): String? {
        val landkoder = kodeverkConsumer.hentLandkoder()
        return landkoder?.hentFraKode(landkode)?.hentNorskNavn()
    }

    fun hentLandFullnavnForKodeISO2(landkodeISO2: String): String? {
        val landkoder = kodeverkConsumer.hentLandkoderISO2()
        return landkoder?.hentFraKode(landkodeISO2)?.hentNorskNavn()
    }
}
