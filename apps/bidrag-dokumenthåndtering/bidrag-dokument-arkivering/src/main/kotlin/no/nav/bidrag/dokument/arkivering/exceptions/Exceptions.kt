package no.nav.bidrag.dokument.arkivering.exceptions

abstract class BidragDokumentArkiveringFunctionalException(
    message: String,
) : RuntimeException(
    message,
)

abstract class BidragDokumentArkiveringTechnicalException : RuntimeException {
    constructor(e: Exception) : super(e)
    constructor(msg: String) : super(msg)
    constructor(msg: String, cause: Throwable?) : super(msg, cause)
}

class OpprettDokumentFeiletException(
    exception: RuntimeException,
) : BidragDokumentArkiveringTechnicalException(
    exception.message ?: "",
)

class JournalpostIkkeFunnetException(
    journalpostId: String?,
) : BidragDokumentArkiveringFunctionalException(
    String.format("Fant ikke journalpost med id %s", journalpostId),
)

class JournalpostHarIkkeGyldigStatusException(
    journalpostId: String?,
) : BidragDokumentArkiveringFunctionalException(
    String.format(
        "Journalpost med id %s har ikke status reservert eller klar til print",
        journalpostId,
    ),
)

class JournalpostHarFlereEnnEnSakException(
    journalpostId: String?,
    antallSaker: Number,
) : BidragDokumentArkiveringFunctionalException(
    String.format(
        "Journalpost med id %s har %s saker men kan maksimalt ha 1 sak",
        journalpostId,
        antallSaker,
    ),
)

class JournalpostHarIkkeJournalfortAvException(
    journalpostId: String?,
) : BidragDokumentArkiveringFunctionalException(
    String.format("Journalpost med id %s har ikke satt parameter journalfortAv", journalpostId),
)

class JournalpostKanIkkeArkiveres(
    journalpostId: String?,
    message: String?,
) : BidragDokumentArkiveringFunctionalException(
    String.format(
        "Journalpost med id %s kan ikke arkiveres med begrunnelse %s",
        journalpostId,
        message ?: "",
    ),
)

class JmsConsumerException(
    exception: Exception,
) : BidragDokumentArkiveringTechnicalException(
    exception,
)

class HentingAvDokumentFeiletException : BidragDokumentArkiveringTechnicalException {
    constructor() : super("En feil oppstod under henting av dokument fra midlertidig brevlager")
    constructor(e: Exception) : super(e)
    constructor(msg: String) : super(msg)
    constructor(msg: String, e: Exception) : super(msg, e)
}

class ArkiveringAvDokumentFeiletException :
    BidragDokumentArkiveringTechnicalException(
        "En feil oppstod under arkivering av journalpost",
    )
