package no.nav.bidrag.arbeidsflyt.utils

import io.mockk.every
import no.nav.bidrag.arbeidsflyt.UnleashFeatures
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveStatus
import no.nav.bidrag.arbeidsflyt.dto.Oppgavestatuskategori
import no.nav.bidrag.arbeidsflyt.hendelse.dto.OppgaveKafkaHendelse
import no.nav.bidrag.arbeidsflyt.persistence.entity.DLQKafka
import no.nav.bidrag.arbeidsflyt.persistence.entity.Journalpost
import no.nav.bidrag.arbeidsflyt.persistence.entity.Oppgave
import no.nav.bidrag.commons.unleash.UnleashFeaturesProvider
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.sak.Bidragssakstatus
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.transport.behandling.hendelse.BehandlingHendelseBarn
import no.nav.bidrag.transport.dokument.HendelseType
import no.nav.bidrag.transport.dokument.JournalpostDto
import no.nav.bidrag.transport.dokument.JournalpostHendelse
import no.nav.bidrag.transport.dokument.JournalpostResponse
import no.nav.bidrag.transport.dokument.JournalpostStatus
import no.nav.bidrag.transport.dokument.Sporingsdata
import no.nav.bidrag.transport.organisasjon.EnhetDto
import no.nav.bidrag.transport.sak.BidragssakDto
import no.nav.bidrag.transport.sak.RolleDto
import java.time.LocalDate
import java.time.LocalDateTime

var journalpostId1 = "124123"
var journalpostId2 = "142312"
var journalpostId3 = "5125125"
var journalpostId4Ny = "6125125"
var bidJournalpostId1 = "BID-8125125"
var bidJournalpostId2 = "BID-9125125"
var bidragJournalpostIdNy = "BID-19125125"

var personident1 = genererFødselsnummer()
var personident2 = genererFødselsnummer()

var oppgaveId1 = 1L
var oppgaveId2 = 2L
var oppgaveId3 = 3L
var oppgaveId4 = 4L
var oppgaveId5 = 5L

var aktørId = "55345678910"
var oppgavetypeJfr = "JFR"

var enhet4806 = "4806"

val saksbehandlerId = "Z994999"

fun enableUnleashFeature(feature: UnleashFeatures) = every {
    UnleashFeaturesProvider
        .isEnabled(feature = eq(feature.featureName), defaultValue = any())
} returns true

fun disableUnleashFeature(feature: UnleashFeatures) = every {
    UnleashFeaturesProvider
        .isEnabled(feature = eq(feature.featureName), defaultValue = any())
} returns false

fun createOppgave(
    oppgaveId: Long,
    journalpostId: String = journalpostId1,
    status: String = OppgaveStatus.OPPRETTET.name,
    oppgaveType: String = oppgavetypeJfr,
): Oppgave = Oppgave(
    oppgaveId = oppgaveId,
    journalpostId = journalpostId,
    status = status,
    oppgavetype = oppgaveType,
    frist = LocalDate.now(),
)

fun createDLQKafka(
    payload: String,
    topicName: String = "topic_journalpost",
    retry: Boolean = false,
    retryCount: Int = 0,
    messageKey: String = "JOARK-$journalpostId1",
    timestamp: LocalDateTime = LocalDateTime.now(),
): DLQKafka = DLQKafka(
    topicName = topicName,
    messageKey = messageKey,
    payload = payload,
    retry = retry,
    retryCount = retryCount,
    createdTimestamp = timestamp,
)

fun createOppgaveData(
    id: Long,
    journalpostId: String? = "123213",
    tildeltEnhetsnr: String? = "4806",
    statuskategori: Oppgavestatuskategori = Oppgavestatuskategori.AAPEN,
    status: OppgaveStatus? = null,
    oppgavetype: String = oppgavetypeJfr,
    tema: String = "BID",
    aktoerId: String = aktørId,
    beskrivelse: String? = null,
    tilordnetRessurs: String? = null,
    fristFerdigstillelse: LocalDate? = LocalDate.of(2020, 2, 1),
) = OppgaveData(
    id = id,
    versjon = 1,
    journalpostId = journalpostId,
    tildeltEnhetsnr = tildeltEnhetsnr,
    status =
    status ?: when (statuskategori) {
        Oppgavestatuskategori.AAPEN -> OppgaveStatus.OPPRETTET
        Oppgavestatuskategori.AVSLUTTET -> OppgaveStatus.FERDIGSTILT
    },
    oppgavetype = oppgavetype,
    tema = tema,
    tilordnetRessurs = tilordnetRessurs,
    fristFerdigstillelse = fristFerdigstillelse,
    beskrivelse = beskrivelse,
    aktoerId = aktoerId,
)

fun OppgaveData.toHendelse(type: OppgaveKafkaHendelse.Hendelse.Hendelsestype? = null) = OppgaveKafkaHendelse(
    hendelse =
    OppgaveKafkaHendelse.Hendelse(
        type ?: when (status) {
            OppgaveStatus.FERDIGSTILT -> OppgaveKafkaHendelse.Hendelse.Hendelsestype.OPPGAVE_FERDIGSTILT
            OppgaveStatus.OPPRETTET -> OppgaveKafkaHendelse.Hendelse.Hendelsestype.OPPGAVE_OPPRETTET
            OppgaveStatus.UNDER_BEHANDLING -> OppgaveKafkaHendelse.Hendelse.Hendelsestype.OPPGAVE_ENDRET
            OppgaveStatus.FEILREGISTRERT -> OppgaveKafkaHendelse.Hendelse.Hendelsestype.OPPGAVE_FEILREGISTRERT
            else -> OppgaveKafkaHendelse.Hendelse.Hendelsestype.OPPGAVE_ENDRET
        },
        LocalDateTime.now(),
    ),
    utfortAv = OppgaveKafkaHendelse.UtfortAv(tilordnetRessurs, tildeltEnhetsnr),
    oppgave =
    OppgaveKafkaHendelse.Oppgave(
        id,
        1,
        kategorisering =
        OppgaveKafkaHendelse.Kategorisering(
            tema ?: "BID",
            oppgavetype = oppgavetype ?: "JFR",
        ),
        bruker =
        OppgaveKafkaHendelse.Bruker(
            aktoerId,
            OppgaveKafkaHendelse.Bruker.IdentType.FOLKEREGISTERIDENT,
        ),
    ),
)

fun createJournalpostHendelse(
    journalpostId: String,
    status: JournalpostStatus = JournalpostStatus.MOTTATT,
    enhet: String? = "4833",
    fagomrade: String = "BID",
    aktorId: String = aktørId,
    sporingEnhet: String = "4833",
): JournalpostHendelse = JournalpostHendelse(
    journalpostId = journalpostId,
    aktorId = aktorId,
    fagomrade = fagomrade,
    enhet = enhet,
    status = status,
    sporing =
    Sporingsdata(
        "test",
        enhetsnummer = sporingEnhet,
        brukerident = saksbehandlerId,
        saksbehandlersNavn = "Navn Navnesen",
    ),
    hendelseType = HendelseType.ENDRING,
    journalposttype = "I",
)

fun journalpostResponse(
    journalpostId: String = journalpostId1,
    journalStatus: JournalpostStatus = JournalpostStatus.JOURNALFØRT,
    journalforendeEnhet: String = "4833",
    tema: String = "BID",
): JournalpostResponse = JournalpostResponse(
    journalpost =
    JournalpostDto(
        journalpostId = journalpostId,
        status = journalStatus,
        journalforendeEnhet = journalforendeEnhet,
        fagomrade = tema,
    ),
)

fun oppgaveDataResponse(): List<OppgaveData> = listOf(
    OppgaveData(
        id = oppgaveId1,
        versjon = 1,
        journalpostId = journalpostId1,
        aktoerId = aktørId,
        oppgavetype = "JFR",
        tema = "BID",
        tildeltEnhetsnr = "4833",
    ),
    OppgaveData(
        id = oppgaveId2,
        versjon = 1,
        journalpostId = journalpostId2,
        aktoerId = aktørId,
        oppgavetype = "JFR",
        tema = "BID",
        tildeltEnhetsnr = "4833",
    ),
    OppgaveData(
        id = oppgaveId4,
        journalpostId = bidJournalpostId1,
        versjon = 1,
        aktoerId = aktørId,
        oppgavetype = "JFR",
        tema = "BID",
        tildeltEnhetsnr = "4833",
    ),
    OppgaveData(
        id = oppgaveId5,
        versjon = 1,
        journalpostId = bidJournalpostId2,
        aktoerId = aktørId,
        oppgavetype = "JFR",
        tema = "BID",
        tildeltEnhetsnr = "4833",
    ),
)

fun createJournalforendeEnheterResponse(): List<EnhetDto> = arrayListOf<EnhetDto>(
    EnhetDto(Enhetsnummer("2103"), "Nav vikafossen"),
    EnhetDto(Enhetsnummer("4817"), "NAV Familie- og pensjonsytelser Steinkjer"),
    EnhetDto(Enhetsnummer("4833"), "NAV Familie- og pensjonsytelser Oslo 1"),
    EnhetDto(Enhetsnummer("4806"), "NAV Familie- og pensjonsytelser Drammen"),
    EnhetDto(Enhetsnummer("4812"), "NAV Familie- og pensjonsytelser Bergen"),
)

fun createJournalpost(
    journalpostId: String,
    status: String = "M",
    enhet: String = "4833",
    tema: String = "BID",
): Journalpost = Journalpost(
    journalpostId = journalpostId,
    status = status,
    enhet = enhet,
    tema = tema,
)

fun opprettSakForBehandling(
    barn: BehandlingHendelseBarn,
): BidragssakDto = BidragssakDto(
    eierfogd = Enhetsnummer("4806"),
    saksnummer = Saksnummer(barn.saksnummer),
    saksstatus = Bidragssakstatus.IN,
    kategori = Sakskategori.NASJONAL,
    opprettetDato = LocalDate.now(),
    levdeAdskilt = false,
    ukjentPart = false,
    roller =
    listOf(
        RolleDto(
            fødselsnummer = Personident(barn.ident!!),
            type = Rolletype.BARN,
        ),
        RolleDto(
            fødselsnummer = Personident("123123"),
            type = Rolletype.BIDRAGSMOTTAKER,
        ),
        RolleDto(
            fødselsnummer = Personident("123123213"),
            type = Rolletype.BIDRAGSPLIKTIG,
        ),
    ),
)
