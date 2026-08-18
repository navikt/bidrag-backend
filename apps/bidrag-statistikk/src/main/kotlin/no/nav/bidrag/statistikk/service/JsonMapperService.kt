package no.nav.bidrag.statistikk.service

import no.nav.bidrag.statistikk.SECURE_LOGGER
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.springframework.stereotype.Service

@Service
class JsonMapperService {
    fun mapHendelse(hendelse: String): VedtakHendelse = try {
        commonObjectmapper.readValue(hendelse, VedtakHendelse::class.java)
    } finally {
        SECURE_LOGGER.debug("Leser hendelse: {}", hendelse)
    }

    fun readTree(hendelse: String) = commonObjectmapper.readTree(hendelse)
}
