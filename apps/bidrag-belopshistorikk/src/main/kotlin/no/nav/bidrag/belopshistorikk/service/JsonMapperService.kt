package no.nav.bidrag.belopshistorikk.service

import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.springframework.stereotype.Service

@Service
class JsonMapperService {
    fun mapHendelse(hendelse: String): VedtakHendelse = try {
        commonObjectmapper.readValue(hendelse, VedtakHendelse::class.java)
    } finally {
        secureLogger.debug { "Leser hendelse: $hendelse" }
    }
}
