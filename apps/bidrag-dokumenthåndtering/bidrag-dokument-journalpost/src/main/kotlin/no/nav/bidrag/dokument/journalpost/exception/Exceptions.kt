package no.nav.bidrag.dokument.journalpost.exception

import no.nav.bidrag.dokument.journalpost.dto.Violation
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException

class SaksbehandlerIkkeFunnetITokenException(
    message: String,
) : RuntimeException(message)

class UgyldigBrevkodeException(
    message: String,
) : RuntimeException(message)

class AvvikException(
    message: String,
) : RuntimeException(message)

class AvvikDetaljException(
    detalj: String,
) : RuntimeException("Manglende detalj i avvik: $detalj")

class CharacterOverflowException(
    message: String,
) : RuntimeException(message)

class ResourceDiscriminatorException(
    message: String,
) : RuntimeException(message)

class JournalpostHendelseException(
    message: String,
    throwable: Throwable?,
) : RuntimeException(message, throwable) {
    constructor(message: String) : this(message = message, throwable = null)
}

class JournalpostIkkeFunnetException(
    message: String,
) : RuntimeException(message)

class DokumentIkkeFunnetException(
    message: String,
) : HttpClientErrorException(HttpStatus.NOT_FOUND, message)

class HentingAvDokumentFeiletException(
    message: String,
    e: Exception? = null,
) : RuntimeException(message, e)

class DokumentErIkkeRTFException(
    message: String,
) : HttpClientErrorException(HttpStatus.NOT_FOUND, message)

class DokumentetErIkkePdfException(
    message: String,
) : RuntimeException(message)

class KanIkkeHenteDokumentUnderProduksjon(
    journalpostId: Int,
) : RuntimeException(
    "Kan ikke hente dokument under produksjon, journalpostId=$journalpostId",
)

class JmsConsumerException(
    exception: Exception,
) : RuntimeException(exception)

class UgyldigJournalpostStatus(
    message: String,
) : HttpStatusException(message, HttpStatus.BAD_REQUEST)

abstract class HttpStatusException(
    message: String,
    var status: HttpStatus,
) : RuntimeException(message)

class ViolationException(
    val violations: List<Violation>,
) : RuntimeException("Ugyldige data: $violations")

class BehandlingAvBrevkvitteringFeilet(
    message: String,
) : RuntimeException(message)

class OppgaveIkkeOpprettetException(
    message: String,
) : RuntimeException(message)

class OppgaveException(
    message: String,
) : RuntimeException(message)

fun journalpostIkkeFunnet(message: String): Nothing = throw HttpClientErrorException(HttpStatus.NOT_FOUND, message)

fun fantIkkeSak(saksnummer: String): Nothing = throw HttpClientErrorException(
    HttpStatus.NOT_FOUND,
    "Sak med saksnummer $saksnummer finnes ikke",
)

fun ingenTilgang(message: String): Nothing = throw HttpClientErrorException(HttpStatus.FORBIDDEN, message)
