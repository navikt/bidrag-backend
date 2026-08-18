package no.nav.bidrag.dokument.journalpost.dto

import no.nav.bidrag.commons.CorrelationId.Companion.fetchCorrelationIdForThread
import no.nav.bidrag.dokument.journalpost.entity.Journalpost
import no.nav.bidrag.dokument.journalpost.entity.ReturDetaljerLogg
import no.nav.bidrag.dokument.journalpost.exception.ViolationException
import no.nav.bidrag.dokument.journalpost.extensions.erUtgående
import no.nav.bidrag.dokument.journalpost.extensions.hentGjelderIdent
import no.nav.bidrag.dokument.journalpost.extensions.hentJournalførendeEnhet
import no.nav.bidrag.dokument.journalpost.model.BATCH_NAVN_JOARK_15
import no.nav.bidrag.dokument.journalpost.model.DokumentType
import no.nav.bidrag.dokument.journalpost.model.Enhet
import no.nav.bidrag.dokument.journalpost.model.Fagomrade
import no.nav.bidrag.dokument.journalpost.model.Journalstatus
import no.nav.bidrag.dokument.journalpost.model.KOMMA
import no.nav.bidrag.dokument.journalpost.model.fraDatabase
import no.nav.bidrag.transport.dokument.AktorDto
import no.nav.bidrag.transport.dokument.AvsenderMottakerDto
import no.nav.bidrag.transport.dokument.AvsenderMottakerDtoIdType
import no.nav.bidrag.transport.dokument.DokumentDto
import no.nav.bidrag.transport.dokument.EndreJournalpostCommand
import no.nav.bidrag.transport.dokument.HendelseType
import no.nav.bidrag.transport.dokument.JournalpostDto
import no.nav.bidrag.transport.dokument.JournalpostHendelse
import no.nav.bidrag.transport.dokument.JournalpostResponse
import no.nav.bidrag.transport.dokument.JournalpostStatus
import no.nav.bidrag.transport.dokument.JournalpostType
import no.nav.bidrag.transport.dokument.Kanal
import no.nav.bidrag.transport.dokument.KodeDto
import no.nav.bidrag.transport.dokument.OpprettJournalpostRequest
import no.nav.bidrag.transport.dokument.ReturDetaljer
import no.nav.bidrag.transport.dokument.ReturDetaljerLog
import no.nav.bidrag.transport.dokument.Sporingsdata
import java.time.LocalDate

object BidragJournalpostType {
    val NOTAT = "X"
    val UTGAAENDE = "U"
}

fun Journalpost.initJournalpostHendelse(saksbehandlersEnhet: String?): JournalpostHendelse = JournalpostHendelse(
    "${Fagomrade.BIDRAG}-$journalpostId",
    fnr = gjelder,
    tittel = beskrivelse,
    fagomrade = fraDatabase(fagomrade),
    tema = fraDatabase(fagomrade),
    batchId = batchNavn,
    journalposttype = dokumentType,
    hendelseType = HendelseType.ENDRING,
    enhet = journalforendeEnhet,
    journalstatus = journalstatus,
    sporing =
    Sporingsdata(
        fetchCorrelationIdForThread(),
        null,
        null,
        saksbehandlersEnhet,
    ),
    sakstilknytninger = hentTilknyttedeSaksnummer().stream().toList(),
    dokumentDato = dokumentdato,
    journalfortDato = journaldato,
)

data class JournalpostIntern(
    val tilknyttedeSaksnummer: MutableSet<String> = HashSet(),
    var avsenderMottaker: AvsenderMottaker? = null,
    var batchNavn: String? = null,
    var brukerid: String? = null,
    var mottakerId: String? = null,
    var dokumenter: List<DokumentIntern> = emptyList(),
    var dokumentDato: LocalDate? = null,
    var fagomrade: String? = null,
    var gjelderAktor: AktorIntern? = null,
    var innhold: String? = null,
    var journalforendeEnhet: String? = null,
    var journalfortAv: String? = null,
    var journalfortDato: LocalDate? = null,
    var journalpostId: String? = null,
    var mottattDato: LocalDate? = null,
    var dokumentType: String? = null,
    var journalstatus: String? = null,
    var feilfort: Boolean? = null,
    var brevkode: KodeIntern? = null,
    var returDetaljerLog: List<ReturDetaljerLogg>? = null,
    var returDato: LocalDate? = null,
    var antallRetur: Int? = null,
    var joarkJournalpostId: String? = null,
) {
    internal val kanal: Kanal
        get() =
            if (journalstatus == Journalstatus.EKSPEDERT) {
                Kanal.LOKAL_UTSKRIFT
            } else if (journalstatus == Journalstatus.EKSPEDERT_JOARK) {
                Kanal.SENTRAL_UTSKRIFT
            } else if (batchNavn?.startsWith(BATCH_NAVN_JOARK_15) == true) {
                Kanal.NAV_NO_BID
            } else {
                Kanal.SKAN_BID
            }

    fun tilJournalpostDto(): JournalpostDto {
        val status = JournalpostStatus.fraKode(journalstatus)
        return JournalpostDto(
            avsenderNavn = avsenderMottaker?.avsenderNavn ?: "",
            avsenderMottaker = avsenderMottaker?.tilAvsenderMottaker(),
            dokumenter =
            dokumenter.map {
                DokumentDto(
                    dokumentreferanse = it.dokumentreferanse,
                    dokumentType = it.dokumentType,
                    tittel = it.tittel,
                )
            },
            dokumentDato = dokumentDato,
            fagomrade = fagomrade,
            gjelderAktor = gjelderAktor?.ident?.let { AktorDto(it) },
            innhold = innhold,
            journalforendeEnhet = journalforendeEnhet,
            journalfortAv = journalfortAv,
            journalfortDato = journalfortDato,
            journalpostId = journalpostId,
            kilde = kanal,
            kanal = kanal,
            mottattDato = mottattDato,
            dokumentType = dokumentType,
            journalstatus = journalstatus,
            status =
            when (status) {
                JournalpostStatus.DOKUMENT_SLETTET,
                JournalpostStatus.MOTTATT,
                JournalpostStatus.FEILREGISTRERT,
                JournalpostStatus.UNDER_PRODUKSJON,
                JournalpostStatus.UNDER_OPPRETTELSE,
                JournalpostStatus.FERDIGSTILT,
                JournalpostStatus.EKSPEDERT,
                JournalpostStatus.AVBRUTT,
                JournalpostStatus.KLAR_FOR_DISTRIBUSJON,
                JournalpostStatus.UTGÅR,
                JournalpostStatus.RETUR,
                JournalpostStatus.RESERVERT,
                JournalpostStatus.JOURNALFØRT,
                JournalpostStatus.DISTRIBUERT,
                -> status

                else -> null
            },
            joarkJournalpostId = joarkJournalpostId,
            feilfort = feilfort,
            brevkode =
            brevkode?.let {
                KodeDto(
                    brevkode!!.kode,
                    brevkode!!.dekode,
                    brevkode!!.erGyldig,
                )
            },
            returDetaljer = hentReturDetaljer(),
        )
    }

    fun hentReturDetaljer(): ReturDetaljer? {
        if (returDato == null) {
            return null
        }

        return ReturDetaljer(
            returDato,
            antallRetur,
            returDetaljerLog?.map {
                ReturDetaljerLog(
                    it.dato,
                    it.beskrivelse,
                )
            } ?: emptyList(),
        )
    }

    fun leggTilSaksnummer(saksnummer: String) {
        tilknyttedeSaksnummer.add(saksnummer)
    }
}

data class AktorIntern(
    var ident: String,
)

data class DokumentIntern(
    var dokumentreferanse: String? = null,
    var dokumentType: String? = null,
    var tittel: String? = null,
)

data class JournalpostResponseIntern(
    val journalpost: JournalpostIntern? = null,
    val journalsaker: List<String> = emptyList(),
) {
    fun tilJournalpostResponse() = JournalpostResponse(
        journalpost = journalpost?.tilJournalpostDto(),
        sakstilknytninger = journalsaker,
    )

    fun harIkkeFunnetJournalpost() = journalpost == null

    fun harIngenTilknyttedeSakerMenGjelderIdent() = journalsaker.isEmpty() && harGjelderAktorIdent()

    fun hentGjelderAktorIdent() = journalpost?.gjelderAktor?.ident

    private fun harGjelderAktorIdent() = journalpost?.gjelderAktor?.ident != null

    fun hentBrevkode() = journalpost?.brevkode

    fun oppdaterMedBrevkode(brevkode: KodeIntern) {
        journalpost?.brevkode = brevkode
    }
}

data class AvsenderMottaker(
    var avsenderNavn: String,
    var avsenderMottakerId: String? = null,
) {
    private val avsenderMedFornavn: Array<String> = avsenderNavn.split(KOMMA).toTypedArray()

    constructor(
        etternavn: String?,
        fornavn: String?,
        avsenderMottakerId: String?,
    ) : this("${hentEtternavn(etternavn)}${hentFornavn(fornavn)}", avsenderMottakerId)

    fun hentEtternavn(): String? = avsenderMedFornavn[0].trim().ifBlank { null }

    fun hentFornavn(): String? = if (avsenderMedFornavn.size == 1 || avsenderMedFornavn[1].trim().isBlank()) {
        null
    } else {
        avsenderMedFornavn[1].trim()
    }

    fun tilAvsenderMottaker(): AvsenderMottakerDto = AvsenderMottakerDto(
        avsenderNavn,
        avsenderMottakerId,
        AvsenderMottakerDtoIdType.UKJENT,
    )
}

private fun hentEtternavn(etternavn: String?) = etternavn?.trim() ?: ""

private fun hentFornavn(fornavn: String?) = if (fornavn == null || fornavn.trim().isBlank()) "" else ", $fornavn"

data class EndreJournalpostCommandIntern(
    var journalpostId: Int,
    var avsenderMottaker: AvsenderMottaker? = null,
    var brukerId: String? = null,
    var journalfortAv: String? = null,
    private var behandlingstema: String? = null,
    var beskrivelse: String? = null,
    var brevkode: String? = null,
    var dokumentDato: LocalDate? = null,
    private var dokumentId: Long? = null,
    private var dokumentTittel: String? = null,
    var fagomrade: String? = null,
    var gjelder: String? = null,
    private var gjelderType: String? = null,
    var journaldato: LocalDate? = null,
    var journalforendeEnhet: String? = null,
    var skalJournalfores: Boolean,
    var tilknyttSaker: List<String>,
    var endreReturDetaljer: List<EndreReturDetaljerIntern>? = null,
    private var tittel: String? = null,
) {
    private var antallDokumenterForEndring: Int = 0

    constructor(
        journalpostId: Int,
        xEnhet: String,
        endreJournalpostCommand: EndreJournalpostCommand,
    ) : this(
        journalpostId = journalpostId,
        avsenderMottaker =
        if (endreJournalpostCommand.avsenderNavn != null) {
            AvsenderMottaker(
                endreJournalpostCommand.avsenderNavn!!,
            )
        } else {
            null
        },
        behandlingstema = endreJournalpostCommand.behandlingstema,
        beskrivelse = endreJournalpostCommand.beskrivelse,
        dokumentDato = endreJournalpostCommand.dokumentDato,
        fagomrade = endreJournalpostCommand.fagomrade,
        gjelder = endreJournalpostCommand.gjelder,
        gjelderType = endreJournalpostCommand.gjelderType?.name,
        journaldato = endreJournalpostCommand.journaldato,
        journalforendeEnhet = xEnhet,
        skalJournalfores = endreJournalpostCommand.skalJournalfores,
        tilknyttSaker = endreJournalpostCommand.tilknyttSaker,
        tittel = endreJournalpostCommand.tittel,
        endreReturDetaljer =
        endreJournalpostCommand.endreReturDetaljer?.map { it ->
            EndreReturDetaljerIntern(
                it.beskrivelse,
                it.nyDato,
                it.originalDato!!,
            )
        },
    ) {
        if (endreJournalpostCommand.endreDokumenter.isNotEmpty()) {
            antallDokumenterForEndring = endreJournalpostCommand.endreDokumenter.size
            brevkode = endreJournalpostCommand.endreDokumenter[0].brevkode
            dokumentId = endreJournalpostCommand.endreDokumenter[0].dokId?.toLong()
            dokumentTittel = endreJournalpostCommand.endreDokumenter[0].tittel
        }
    }

    fun hasEndreReturDetaljer(): Boolean = endreReturDetaljer?.isNotEmpty() ?: false

    fun hentBeskrivelse(): String? {
        if (beskrivelse != null) {
            return beskrivelse
        }

        if (tittel != null) {
            return tittel
        }

        return dokumentTittel
    }

    fun sjekkGyldigEndring(journalpost: Journalpost) {
        val violations = mutableListOf<Violation>()

        if (antallDokumenterForEndring > 1) {
            violations.add(
                Violation(
                    "endreDokumenter",
                    "Midlertidig brevlager støtter bare et dokument per journalpost",
                ),
            )
        }

        if (journalpost.kanIkkeEndre(journalpostId)) {
            violations.add(Violation("journalpostId", "Kan ikke endre journalpost med ugyldig id"))
        }

        if (skalJournalfores) {
            if (journalpost.erJournalstatusIkkeMottaksregistrert()) {
                violations.add(
                    Violation(
                        "journalstatus",
                        "Journalpost med journalstatus ${journalpost.journalstatus} kan ikke journalføres",
                    ),
                )
            }

            if (tilknyttSaker.isEmpty() && journalpost.harIngenJournalsaker()) {
                violations.add(
                    Violation(
                        "tilknyttSaker",
                        "Kan ikke registrere journalpost uten sak",
                    ),
                )
            }

            if (journalpost.manglerGjelder(gjelder)) {
                violations.add(
                    Violation(
                        "gjelder",
                        "Kan ikke registrere journalpost når det mangler gjelder for sak",
                    ),
                )
            }
        }

        if (violations.isNotEmpty()) {
            throw ViolationException(violations)
        }
    }

    fun harAvsender() = avsenderMottaker != null

    fun hentEtternavn() = avsenderMottaker?.hentEtternavn()

    fun hentFornavn() = avsenderMottaker?.hentFornavn()
}

data class OpprettUtgaaendeJournalpostIntern(
    var mottaker: AvsenderMottaker? = null,
    var gjelderId: String? = null,
    var journalfortAv: String? = null,
    var journalstatus: String? = null,
    var behandlingstema: String? = null,
    var dokumenttittel: String? = null,
    var brevkode: String? = null,
    var dokumentreferanse: String? = null,
    var fagomrade: String? = null,
    var journalforendeEnhet: String? = null,
    var journalforendeEnhetNavn: String? = null,
    var dokumentType: String? = null,
    var tilknyttSaker: List<String>,
    var tittel: String? = null,
    val referanseId: String? = null,
    var opprettetAvId: String? = null,
    val opprettetAvNavn: String? = null,
    val kravtype: String? = null,
) {
    companion object {
        fun sjekkKanOppretteJournalpost(request: OpprettJournalpostRequest) {
            val violations = mutableListOf<Violation>()

            if (!listOf(
                    JournalpostType.UTGÅENDE,
                    JournalpostType.UTGAAENDE,
                    JournalpostType.NOTAT,
                ).contains(request.journalposttype)
            ) {
                violations.add(
                    Violation(
                        "journalposttype",
                        "Kan bare opprette journalpost med type notat eller utgående",
                    ),
                )
            }

            if (request.hentJournalførendeEnhet().isNullOrEmpty()) {
                violations.add(
                    Violation(
                        "journalfoerendeEnhet",
                        "JournalfoerendeEnhet kan ikke være tom",
                    ),
                )
            }

            if (request.dokumenter.isEmpty()) {
                violations.add(Violation("dokumenter", "Journalpost må knyttes til et dokument"))
            }

            if (request.dokumenter.size > 1) {
                violations.add(
                    Violation(
                        "dokumenter",
                        "Midlertidig brevlager støtter bare et dokument per journalpost",
                    ),
                )
            }

            if (request.dokumenter.isNotEmpty() && request.dokumenter[0].tittel.isEmpty()) {
                violations.add(
                    Violation(
                        "dokumenter",
                        "Dokumentet journalpost knyttes må ha satt tittel",
                    ),
                )
            }

            if (request.tilknyttSaker.isEmpty()) {
                violations.add(
                    Violation(
                        "tilknyttSaker",
                        "Journalpost må knyttes til minst en sak",
                    ),
                )
            }

            if (request.hentGjelderIdent().isNullOrEmpty()) {
                violations.add(Violation("gjelder", "Gjelder ident kan ikke være tom"))
            }

            if (request.erUtgående && request.avsenderMottaker?.ident.isNullOrEmpty()) {
                violations.add(
                    Violation(
                        "avsenderMottaker",
                        "Mottaker ident kan ikke være tom for utgående journalpost",
                    ),
                )
            }

            if (violations.isNotEmpty()) {
                throw ViolationException(violations)
            }
        }
    }

    constructor(
        opprettJournalpost: OpprettJournalpostRequest,
        opprettetAvId: String?,
        opprettetAvNavn: String?,
        brevkode: KodeIntern?,
        enhetInfo: Enhet?,
    ) : this(
        mottaker =
        if (opprettJournalpost.avsenderMottaker != null) {
            AvsenderMottaker(
                opprettJournalpost.avsenderMottaker?.navn ?: "",
                opprettJournalpost.avsenderMottaker?.ident,
            )
        } else {
            null
        },
        behandlingstema = opprettJournalpost.behandlingstema,
        dokumenttittel = opprettJournalpost.dokumenter[0].tittel,
        fagomrade = Fagomrade.BIDRAG_DATABASE,
        gjelderId = opprettJournalpost.hentGjelderIdent(),
        opprettetAvId = opprettetAvId,
        opprettetAvNavn = opprettetAvNavn,
        journalstatus = Journalstatus.UNDER_PRODUKSJON,
        referanseId = opprettJournalpost.referanseId,
        journalforendeEnhet = opprettJournalpost.hentJournalførendeEnhet(),
        journalforendeEnhetNavn = enhetInfo?.navn ?: "",
        brevkode = opprettJournalpost.dokumenter[0].brevkode,
        kravtype = brevkode?.kravtype ?: "AN",
        tilknyttSaker = opprettJournalpost.tilknyttSaker,
        tittel = opprettJournalpost.tittel,
        dokumentType =
        when (opprettJournalpost.journalposttype) {
            JournalpostType.NOTAT -> BidragJournalpostType.NOTAT
            JournalpostType.UTGÅENDE, JournalpostType.UTGAAENDE -> BidragJournalpostType.UTGAAENDE
            else -> BidragJournalpostType.UTGAAENDE
        },
    )

    fun hentMottakerId() = mottaker?.avsenderMottakerId

    fun harMottaker() = mottaker != null

    fun harMottakerNavn() = !mottaker?.avsenderNavn.isNullOrEmpty()

    fun hentEtternavn() = mottaker?.hentEtternavn()

    fun hentFornavn() = mottaker?.hentFornavn()

    fun erNotat() = dokumentType == DokumentType.NOTAT
}

data class EndreReturDetaljerIntern(
    var beskrivelse: String,
    var nyDato: LocalDate?,
    var originalDato: LocalDate,
) {
    var gjeldendeDato = nyDato ?: originalDato
}

data class KodeIntern(
    var kode: String,
    var dekode: String? = null,
    var kravtype: String? = null,
    var erGyldig: Boolean,
) {
    constructor(kode: String) : this(kode, null, null, true)
    constructor(kode: String, dekode: String) : this(kode, dekode, null, true)
}
