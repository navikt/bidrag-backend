package no.nav.bidrag.dokument.journalpost.model

object BidragPerson {
    const val HENT_PERSON_INFO_URL = "/bidrag-person/informasjon"
}

object AvvikDetaljer {
    const val BEKREFTET_SENDT_SCANNING = "bekreftetSendtScanning"
    const val ENHETSNUMMER = "enhetsnummer"
    const val ENHETSNUMMER_GAMMELT = "gammeltEnhetsnummer"
    const val ENHETSNUMMER_NYTT = "nyttEnhetsnummer"
    const val RETUR_DATO = "returDato"
    const val FAGOMRADE = "fagomrade"
    const val JOARK_JOURNALPOST_ID = "joarkJournalpostId"
    const val JOARK_ARKIVERING_STATUS = "joarkArkiveringStatus"
}

object JournalpostHendelser {
    const val JOURNALFORING = "JOURNALFOR_JOURNALPOST"
    const val REGISTRER_JOURNALPOST = "REGISTRER_JOURNALPOST"
}

object DokumentType {
    const val NOTAT = "X"
    const val INNGAENDE_DOKUMENT = "I"
    const val UTGAAENDE_DOKUMENT = "U"
}

object Fagomrade {
    const val BIDRAG = "BID"
    const val BIDRAG_DATABASE = "BNR"
    const val FARSKAP = "FAR"
}

const val JP_ARKIVDEL = "BIDRNR"
const val JP_SYSTEMID_BISYS = "BI12"
const val SYSTEM_SAKSBEHANDLER = "RTV9999"
const val SYSTEM_SAKSBEHANDLER_NAVN = "Bidrag - Automatisk jobb"

object Dokstatus {
    const val DOKBESKRIVELSE_STATUS = "B"
}

object Journalstatus {
    const val AVVIK_BESTILL_RESKANNING = "AR"
    const val AVVIK_BESTILL_SPLITTING = "AS"
    const val AVVIK_ENDRE_FAGOMRADE = "AF"
    const val EKSPEDERT = "E"
    const val EKSPEDERT_JOARK = "EJ"
    const val KLAR_TIL_PRINT = "KP"
    const val JOURNALFORT = "J"
    const val MOTTAKSREGISTRERT = "M"
    const val RESERVERT = "R"
    const val FEILFORT = "F"
    const val SLETTET = "S"
    const val UNDER_PRODUKSJON = "D"
    const val DOKUMENT_SLETTET = "DS"
    const val UTGAR = "U"
    const val OPPRETTET = "O"
    const val TIL_LAGRING = "T"
}

object Oppgave {
    const val ENHET_SCANNING = "4803"
    const val TEMA_POSTMOTTAK = "MOT"
    const val TYPE_FOR_AVVIK = "SR"
    const val TYPE_FOR_JOURNALFORING = "JFR"
    const val TYPE_FOR_BEHANDLING = "BEH_SAK"
}

const val BATCH_NAVN_JOARK = "BJOARK"
const val BATCH_NAVN_JOARK_15 = "${BATCH_NAVN_JOARK}015_"
const val KOMMA = ","
const val MAX_LENGDE_JORNALHENDELSE_BESKRIVELSE = 1000
