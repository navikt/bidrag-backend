package no.nav.bidrag.bbm.exception

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.transport.behandling.beregning.felles.OppdaterBehandlingsidRequest
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException

private val objectmapper = ObjectMapper().findAndRegisterModules()

fun OppdaterBehandlingsidRequest.mismatchEksisterendeBehandlingsid(): Nothing = throw HttpClientErrorException(
    HttpStatus.BAD_REQUEST,
    "Angitt eksisterende behandlingsid stemmer ikke med lagret behandlingsid",
    objectmapper.writeValueAsBytes(this.copy()),
    null,
)

fun OppdaterBehandlingsidRequest.søknadFinnesIkke(): Nothing = throw HttpClientErrorException(
    HttpStatus.NOT_FOUND,
    "Ingen søknad med angitt søknadsid funnet",
    objectmapper.writeValueAsBytes(this.copy()),
    null,
)
