package no.nav.bidrag.dokument.arkivering.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.bidrag.dokument.arkivering.exceptions.JournalpostHarFlereEnnEnSakException
import no.nav.bidrag.transport.dokument.JournalpostResponse
import org.apache.commons.lang3.Validate

private const val JOURNALFORT_AV_KEY = "journalfortAv"
const val DOKUMENT_VARIANT_FORMAT_ARKIV = "ARKIV"
const val DOKUMENT_FILTYPE_PDFA = "PDFA"
private val CONTROL_CHARACTERS_MIN = 0
private val CONTROL_CHARACTERS_MAX = 31

@JsonIgnoreProperties(ignoreUnknown = true, value = ["journalpostId"])
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OpprettJournalpostRequest(
    var sak: Sak? = null,
    var tittel: String? = null,
    var journalfoerendeEnhet: String? = null,
    var journalpostType: JournalpostType? = null,
    var datoRetur: String? = null,
    var behandlingstema: String? = null,
    var eksternReferanseId: String? = null,
    var tilleggsopplysninger: List<Tilleggsopplysning>? = null,
    var tema: Tema? = null,
    var kanal: String? = null,
    var datoMottatt: String? = null,
    var bruker: Bruker? = null,
    var dokumenter: List<Dokument> = emptyList(),
    var avsenderMottaker: AvsenderMottaker? = null,
) {
    constructor(journalpostResponse: JournalpostResponse, dokument: ByteArray) : this() {
        val journalpost = journalpostResponse.journalpost
        val journalpostIdWithPrefix = journalpost?.journalpostId?.replace("BID-", "BID_")
        val saksnummer: String = journalpostResponse.sakstilknytninger[0]
        val brevKode = journalpost?.brevkode?.kode?.trim()
        sak = Sak(saksnummer)
        tema = if (journalpost?.fagomrade?.equals(Tema.FAR.name, ignoreCase = true) == true) Tema.FAR else Tema.BID
        eksternReferanseId = journalpostIdWithPrefix
        journalfoerendeEnhet = journalpost?.journalforendeEnhet
        journalpostType = JournalpostType.toJournalpostType(journalpost?.dokumentType)
        behandlingstema = BrevkodeToBehandlingstemaMapper().toBehandlingstema(brevKode, journalpost?.fagomrade).kode
        tittel = journalpost?.innhold
        avsenderMottaker =
            AvsenderMottaker(
                id = journalpost?.gjelderAktor?.ident,
                idType = BrukerIdType.FNR.name,
            )
        bruker =
            Bruker(
                id = journalpost?.gjelderAktor?.ident,
                idType = BrukerIdType.FNR,
            )
        dokumenter =
            listOf(
                Dokument(
                    brevkode = brevKode,
                    tittel = journalpost?.innhold,
                    dokumentvarianter =
                    listOf(
                        DokumentVariant(
                            variantformat = DOKUMENT_VARIANT_FORMAT_ARKIV,
                            filtype = DOKUMENT_FILTYPE_PDFA,
                            fysiskDokument = dokument,
                            filnavn = "$journalpostIdWithPrefix.pdf",
                        ),
                    ),
                ),
            )
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Tilleggsopplysning(
        val nokkel: String? = null,
        val verdi: String? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class AvsenderMottaker(
        val navn: String? = null,
        val id: String? = null,
        val idType: String?,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Dokument(
        val dokumentInfoId: String? = null,
        val dokumentKategori: String? = null,
        val tittel: String? = null,
        val brevkode: String? = null,
        val dokumentvarianter: List<DokumentVariant>? = emptyList(),
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Bruker(
        val id: String? = null,
        val idType: BrukerIdType? = null,
    )

    @Suppress("unused") // properties used by jackson
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Sak(
        val fagsakId: String? = null,
    ) {
        val fagsaksystem = if (fagsakId == null) null else Fagsaksystem.BISYS
        val sakstype = if (fagsakId === null) null else Sakstype.FAGSAK
    }

    @Suppress("unused") // properties used by jackson
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class DokumentVariant(
        val filtype: String? = null,
        val variantformat: String? = null,
        val fysiskDokument: ByteArray,
        val filnavn: String? = null,
    )
}

fun removeNonPrintableCharacters(value: String?): String? = value?.replace("\\p{C}".toRegex(), "")

fun containsNonPrintableCharachters(value: String?): Boolean = value?.chars()?.anyMatch { it ->
    it in CONTROL_CHARACTERS_MAX downTo CONTROL_CHARACTERS_MIN
} == true

fun validereJournalpostResponse(
    jpId: String,
    journalpostResponse: JournalpostResponse?,
) {
    val journalpostDto = journalpostResponse?.journalpost
    val sakstilknytninger = journalpostResponse?.sakstilknytninger
    Validate.isTrue(journalpostDto != null, "Fant ingen journalpost")
    Validate.isTrue(sakstilknytninger != null, "Fant ingen saker")
    Validate.isTrue(journalpostDto?.gjelderAktor?.ident != null, "Journalpost mangler gjelder")
    Validate.isTrue(journalpostDto?.brevkode != null, "Journalpost mangler brevkode")
    Validate.isTrue(
        !containsNonPrintableCharachters(journalpostDto?.innhold),
        "Tittel inneholder ugyldig tegn",
    )
    Validate.isTrue(journalpostDto?.dokumentType != null, "Journalpost mangler dokumenttype")
    Validate.isTrue(journalpostDto?.dokumentType == "U", "Journalpost må være utgående")
    Validate.isTrue(journalpostDto?.fagomrade != null, "Journalpost mangler fagområde")
    Validate.isTrue(journalpostDto?.mottattDato != null, "Journalpost mangler mottatt dato")
    Validate.isTrue(journalpostDto?.journalfortAv != null, "Journalpost mangler journalfortAv")
    Validate.isTrue(journalpostDto?.dokumenter?.size == 1, "Journalpost mangler dokument")
    Validate.isTrue(
        journalpostDto?.dokumenter?.get(0)?.dokumentreferanse != null,
        "Journalpost dokument mangler dokumentreferanse",
    )
    if (sakstilknytninger!!.size > 1) {
        throw JournalpostHarFlereEnnEnSakException(jpId, sakstilknytninger.size)
    }
}

enum class BrukerIdType {
    FNR,
    ORGNR,
    AKTOERID,
}

enum class Fagsaksystem {
    FS38,
    FS36,
    UFM,
    OEBS,
    OB36,
    AO01,
    AO11,
    IT01,
    PP01,
    K9,
    BISYS,
    BA,
    EF,
    KONT,
}

enum class Sakstype {
    FAGSAK,
    GENERELL_SAK,
}

enum class JournalpostType {
    INNGAAENDE,
    UTGAAENDE,
    NOTAT,
    ;

    companion object {
        fun toJournalpostType(dokumentTypeString: String?): JournalpostType? = when (Dokumenttype.toDokumenttype(dokumentTypeString)) {
            Dokumenttype.INNGAAENDE -> INNGAAENDE
            Dokumenttype.UTGAAENDE -> UTGAAENDE
            Dokumenttype.NOTAT -> NOTAT
            else -> null
        }
    }
}

enum class Tema {
    BID,
    FAR,
}

enum class Fagomraade(
    val kode: String,
) {
    BIDRAG("BID"),
    FARSKAP("FAR"),
}

enum class Behandlingstema(
    val kode: String,
) {
    BIDRAG_INKLUSIV_FARSKAP("ab0322"),
    BARNEBORTFOERING("ab0323"),
    OPPFOSTRINGSBIDRAG("ab0324"),
    EKTEFELLE("ab0325"),
    FORELDREPENGER("ab0326"),
    ENGANGSSTOENAD("ab0327"),
    BIDRAG_EKSKLUSIV_FARSKAP("ab0328"),
    BIDRAG_UTLAND_EKSKLUSIV_FARSKAP("ab0329"),
}

class BrevkodeToBehandlingstemaMapper {
    private var brevkodemap: MutableMap<String, Behandlingstema> = hashMapOf()

    private fun initBrevkodemap() {
        brevkodemap["BI01H01"] = Behandlingstema.BIDRAG_INKLUSIV_FARSKAP // Farskap innkalling mor
        brevkodemap["BI01H02"] = Behandlingstema.BIDRAG_INKLUSIV_FARSKAP // Innkalling farskapssak  oppgitt far
        brevkodemap["BI01H03"] = Behandlingstema.BIDRAG_INKLUSIV_FARSKAP // Melding om blodprøver i farskapsak
        brevkodemap["BI01H04"] = Behandlingstema.BIDRAG_INKLUSIV_FARSKAP // Pålegg om å framstille barn for å gi blodprøve
        brevkodemap["BI01H05"] = Behandlingstema.BIDRAG_INKLUSIV_FARSKAP // Pålegg om blodprøve i farskapssak

        brevkodemap["BI01S46"] = Behandlingstema.OPPFOSTRINGSBIDRAG // Varsel oppfostringsbidrag forholdsmessig fordeling
        brevkodemap["BI01S47"] = Behandlingstema.OPPFOSTRINGSBIDRAG // Endring oppfostringsbidrag orientering til BP
        brevkodemap["BI01S48"] = Behandlingstema.OPPFOSTRINGSBIDRAG // Endring oppfostringsbidrag orientering kommune
        brevkodemap["BI01S49"] = Behandlingstema.OPPFOSTRINGSBIDRAG // Endring oppfostringsbidrag varsel til motparten
        brevkodemap["BI01S50"] = Behandlingstema.OPPFOSTRINGSBIDRAG // Ettergivelse oppfostringsbidrag orientering til BP
        brevkodemap["BI01S51"] = Behandlingstema.OPPFOSTRINGSBIDRAG // Ettergivelse oppfostringsbidrag varsel kommune
        brevkodemap["BI01S52"] = Behandlingstema.OPPFOSTRINGSBIDRAG // Fastsettelse oppfostringsbidrag orienter kommune
        brevkodemap["BI01S53"] = Behandlingstema.OPPFOSTRINGSBIDRAG // Fastsettelse oppfostringsbidrag varsel til BP

        brevkodemap["BI01B20"] = Behandlingstema.BIDRAG_UTLAND_EKSKLUSIV_FARSKAP // Vedtak utland skjønn fastsettelse
        brevkodemap["BI01B21"] = Behandlingstema.BIDRAG_UTLAND_EKSKLUSIV_FARSKAP // Vedtak utland skjønn endring
        brevkodemap["BI01B22"] = Behandlingstema.BIDRAG_UTLAND_EKSKLUSIV_FARSKAP // Bidrag indeksreg, BP bor i utlandet

        brevkodemap["BI01I01"] = Behandlingstema.EKTEFELLE // Vedtak ektefellebidrag
        brevkodemap["BI01I50"] = Behandlingstema.EKTEFELLE // Klage - vedtak ektefellebidrag
        brevkodemap["BI01S37"] = Behandlingstema.EKTEFELLE // Bortfall ektefellebidrag BP død orientering til BM
        brevkodemap["BI01S38"] = Behandlingstema.EKTEFELLE // Bortfall ektefellebidrag orientering til partene
        brevkodemap["BI01S39"] = Behandlingstema.EKTEFELLE // Bortfall ektefellebidrag nytt ekteskap orientering
        brevkodemap["BI01S42"] = Behandlingstema.EKTEFELLE // Endring ektefellebidrag orientering til søkeren
        brevkodemap["BI01S43"] = Behandlingstema.EKTEFELLE // Fastsettelse ektefellebidrag orientering
        brevkodemap["BI01S44"] = Behandlingstema.EKTEFELLE // Fastsettelse ektefellebidrag varsel til BP
    }

    fun toBehandlingstema(
        brevkode: String?,
        tema: String?,
    ): Behandlingstema {
        return brevkodemap[brevkode]
            ?: return if (tema != null && tema.equals(Tema.FAR.name, ignoreCase = true)) {
                Behandlingstema.BIDRAG_INKLUSIV_FARSKAP
            } else {
                Behandlingstema.BIDRAG_EKSKLUSIV_FARSKAP
            }
    }

    init {
        initBrevkodemap()
    }
}

enum class Soekestreng(
    val soekestreng: String,
) {
    BARNEBORTFOERING("arnebortf"),
    OPPFOSTRINGSBIDRAG("ppfostrings"),
    EKTEFELLE("ktefell"),
    FORELDREPENGER("oreldrepenge"),
    ENGANGSSTOENAD("ngangsst"),
    UTLAND("utland"),
    ;

    companion object {
        fun toBehandlingstema(
            innhold: String?,
            fagomraade: String?,
        ): String? {
            if (innhold == null) {
                return if (fagomraade.equals(Tema.FAR.name, ignoreCase = true)) {
                    Behandlingstema.BIDRAG_INKLUSIV_FARSKAP.kode
                } else {
                    Behandlingstema.BIDRAG_EKSKLUSIV_FARSKAP.kode
                }
            }
            if (innhold.contains(BARNEBORTFOERING.soekestreng)) {
                return Behandlingstema.BARNEBORTFOERING.kode
            } else if (innhold.contains(OPPFOSTRINGSBIDRAG.soekestreng)) {
                return Behandlingstema.OPPFOSTRINGSBIDRAG.kode
            } else if (innhold.contains(EKTEFELLE.soekestreng)) {
                return Behandlingstema.EKTEFELLE.kode
            } else if (innhold.contains(FORELDREPENGER.soekestreng)) {
                return Behandlingstema.FORELDREPENGER.kode
            } else if (innhold.contains(ENGANGSSTOENAD.soekestreng)) {
                return Behandlingstema.ENGANGSSTOENAD.kode
            }
            return if (fagomraade.equals(Tema.FAR.name, ignoreCase = true)) {
                Behandlingstema.BIDRAG_INKLUSIV_FARSKAP.kode
            } else if (innhold.contains(UTLAND.soekestreng)) {
                Behandlingstema.BIDRAG_UTLAND_EKSKLUSIV_FARSKAP.kode
            } else {
                Behandlingstema.BIDRAG_EKSKLUSIV_FARSKAP.kode
            }
        }
    }
}

enum class Dokumenttype(
    private val kode: String,
) {
    INNGAAENDE("I"),
    UTGAAENDE("U"),
    NOTAT("X"),
    ;

    fun getKode(): String = kode

    companion object {
        fun toDokumenttype(kode: String?): Dokumenttype? {
            for (type in values()) {
                if (type.kode == kode) {
                    return type
                }
            }
            return null
        }
    }
}

enum class JournalStatus(
    jpType: JournalpostType,
    medDokumentvarianter: Boolean,
) {
    INNGAAENDE_UTEN_DOKUMENTVARIANTER(JournalpostType.INNGAAENDE, false),
    INNGAAENDE_MED_DOKUMENTVARIANTER(JournalpostType.INNGAAENDE, true),
    UTGAAENDE_UTEN_DOKUMENTVARIANTER(JournalpostType.UTGAAENDE, false),
    UGAAENDE_MED_DOKUMENTVARIANTER(JournalpostType.UTGAAENDE, true),
    NOTAT_UTEN_DOKUMENTVARIANTER(JournalpostType.NOTAT, false),
    NOTAT_MED_DOKUMENTVARIANTER(JournalpostType.NOTAT, true),
    ;

    var kode: String? = null

    init {
        if (jpType.equals(JournalpostType.INNGAAENDE) && !medDokumentvarianter) {
            kode = "OD"
        } else if (jpType.equals(JournalpostType.INNGAAENDE) && medDokumentvarianter) {
            kode = "M"
        } else if (jpType.equals(JournalpostType.UTGAAENDE) && !medDokumentvarianter) {
            kode = "R"
        } else if (jpType.equals(JournalpostType.UTGAAENDE) && medDokumentvarianter) {
            kode = "D"
        } else if (jpType.equals(JournalpostType.NOTAT) && !medDokumentvarianter) {
            kode = "R"
        } else if (jpType.equals(JournalpostType.NOTAT) && medDokumentvarianter) {
            kode = "D"
        }
    }
}

data class DokumentInfo(
    val dokumentInfoId: String?,
) {
    constructor() : this(null)
}

data class OpprettJournalpostResponse(
    var journalpostId: String? = null,
    val journalstatus: String? = null,
    val melding: String? = null,
    val journalpostferdigstilt: Boolean? = null,
    val dokumenter: List<DokumentInfo>? = emptyList(),
)
