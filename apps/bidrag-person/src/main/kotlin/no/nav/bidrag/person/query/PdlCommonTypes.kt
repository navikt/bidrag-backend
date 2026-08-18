package no.nav.bidrag.person.query

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.domene.enums.person.Diskresjonskode
import no.nav.bidrag.domene.enums.person.Gradering
import no.nav.bidrag.domene.ident.Personident
import java.time.LocalDate
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

interface PersonBolkResponse<U> {
    val personBolk: List<PersonForekomst<U>>
}

interface PersonForekomst<U> {
    val ident: Personident
    val code: String
    val person: U?
}

data class Fødselsdato(val foedselsdato: LocalDate? = null, val foedselsaar: Int? = null)

data class Dødsfall(val doedsdato: LocalDate? = null)

data class Adressebeskyttelse(var gradering: Gradering)

data class Folkeregisteridentifikator(val identifikasjonsnummer: Personident, val status: IdentStatus, val type: FolkreregisterIdent)

data class Folkeregistermetadata(val ajourholdstidspunkt: LocalDateTime? = null)

data class Metadata(val master: String = Kilde.FREG.name, val historisk: Boolean = false, val endringer: Set<Endring> = emptySet()) {
    var kilde: Kilde = Kilde.FREG

    init {
        try {
            kilde = Kilde.valueOf(master.uppercase())
        } catch (iae: IllegalArgumentException) {
            log.error { "Mottok ukjent master $master fra PDL." }
        }
    }
}

data class Endring(val type: Endringstype, val registrert: LocalDateTime)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val forkortetNavn: String? = null,
    val folkeregistermetadata: Folkeregistermetadata? = null,
    val metadata: Metadata? = null,
) {
    val visningsnavn
        get(): String =
            run {
                val fornavn = fornavn.formaterNavnMedStorForbokstav()
                val mellomnavn = mellomnavn.formaterNavnMedStorForbokstav()
                val etternavn = etternavn.formaterNavnMedStorForbokstav()
                val visningsnavn =
                    "$fornavn $mellomnavn $etternavn"
                        .trim()
                        .replace("  ", " ")
                return if (visningsnavn.length > 100 && mellomnavn.isNotEmpty()) {
                    "$fornavn ${mellomnavn.first()}. $etternavn"
                        .trim()
                } else {
                    visningsnavn
                }
            }
}

open class OppholdsadresseCommon(open val oppholdAnnetSted: OppholdAnnetSted?)

enum class OppholdAnnetSted {
    UTENRIKS,
    MILITAER,
    PENDLER,

    @JsonAlias("paaSvalbard")
    PAA_SVALBARD,
}

enum class IdentStatus {
    I_BRUK,
    OPPHOERT,
}

enum class FolkreregisterIdent {
    DNR,
    FNR,
}

enum class Kilde { FREG, PDL }

enum class Endringstype { ANNULLER, KORRIGER, OPPHOER, OPPRETT }

fun toDisreksjonsKode(oppholdsadresseList: List<OppholdsadresseCommon>, adressebeskyttelseList: List<Adressebeskyttelse>): Diskresjonskode? {
    val adressebeskyttelse = if (adressebeskyttelseList.isEmpty()) null else adressebeskyttelseList[0]
    val oppholdsadresse = if (oppholdsadresseList.isEmpty()) null else oppholdsadresseList[0]
    return when (adressebeskyttelse?.gradering) {
        Gradering.STRENGT_FORTROLIG_UTLAND -> Diskresjonskode.P19

        Gradering.STRENGT_FORTROLIG -> Diskresjonskode.SPSF

        Gradering.FORTROLIG -> Diskresjonskode.SPFO

        else ->
            when (oppholdsadresse?.oppholdAnnetSted) {
                OppholdAnnetSted.UTENRIKS -> Diskresjonskode.URIK
                OppholdAnnetSted.MILITAER -> Diskresjonskode.MILI
                OppholdAnnetSted.PENDLER -> Diskresjonskode.PEND
                OppholdAnnetSted.PAA_SVALBARD -> Diskresjonskode.SVAL
                else -> null
            }
    }
}
