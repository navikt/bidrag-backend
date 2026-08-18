package no.nav.bidrag.person.query

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.person.Kjønn
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.person.model.PdlException
import no.nav.bidrag.transport.person.Identgruppe
import no.nav.bidrag.transport.person.PersonDto
import org.springframework.http.HttpStatus
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

data class PersonQuery(val personIdent: Personident) : GraphQuery() {
    private val query = """
       query PersonQuery(${"$"}personId: ID!) 
        {
            hentPerson( ident:  ${"$"}personId) {
                adressebeskyttelse {
                   gradering
               }
               oppholdsadresse {
                   oppholdAnnetSted
               }
               foedselsdato {
                    foedselsdato
                    foedselsaar
               }
                doedsfall {
                    doedsdato
                }
               kjoenn(historikk: false){
                    kjoenn
               }
               navn(historikk: false) {
                    fornavn
                    mellomnavn
                    forkortetNavn
                    etternavn
                    folkeregistermetadata {
                        ajourholdstidspunkt
                    }
                    metadata {
                        historisk
                        master
                        endringer {
                            type
                            registrert
                        }                        
                    }
                }
            }
            hentIdenter(ident: ${"$"}personId, historikk:false) {
                identer {
                    ident,
                    historisk,
                    gruppe
                }
             }        
      }
        """

    override fun getQuery(): String = query

    override fun getVariables(): HashMap<String, Any> = hashMapOf("personId" to personIdent)
}

data class PersonResponse(var hentPerson: HentPerson, var hentIdenter: HentIdenter) {
    fun mapToPersonDto(): PersonDto {
        val personNavn = getNavn()
        val ident = hentIdenter.getIdent()
        val kortnavn = "${personNavn?.fornavn ?: ""} ${personNavn?.etternavn ?: ""}".trim().formaterNavnMedStorForbokstav()
        val fulltNavn =
            if (personNavn == null) null else "${personNavn.etternavn}, ${personNavn.fornavn} ${personNavn.mellomnavn ?: ""}".trim()
        return PersonDto(
            ident,
            aktørId = hentIdenter.getAktorId(),
            navn = fulltNavn,
            fornavn = personNavn?.fornavn.formaterNavnMedStorForbokstav(),
            mellomnavn = personNavn?.mellomnavn.formaterNavnMedStorForbokstav(),
            etternavn = personNavn?.etternavn.formaterNavnMedStorForbokstav(),
            kortnavn = kortnavn,
            kjønn = getKjønn(),
            dødsdato = getDødsDato(),
            fødselsdato = getFødselsDato(),
            diskresjonskode = getDisreksjonsKode(),
            visningsnavn = getNavn()?.visningsnavn ?: "",
        )
    }

    private fun getDisreksjonsKode() = toDisreksjonsKode(hentPerson.oppholdsadresse, hentPerson.adressebeskyttelse)

    private fun getDødsDato() = hentPerson.doedsfall.firstOrNull()?.doedsdato

    private fun getFødselsDato() = hentPerson.foedselsdato.firstOrNull()?.foedselsdato

    private fun getKjønn() = hentPerson.kjoenn.firstOrNull()?.kjoenn

    private fun getNavn() = hentPerson.navn.maxByOrNull { finneTidspunktNyesteInnslag(it)!! }

    private fun finneSisteEndringstidspunkt(metadata: Metadata) = metadata.endringer.maxByOrNull { it.registrert }?.registrert

    private fun finneTidspunktNyesteInnslag(navn: Navn): LocalDateTime? = when (navn.metadata?.kilde) {
        Kilde.PDL -> finneSisteEndringstidspunkt(navn.metadata)

        Kilde.FREG -> navn.folkeregistermetadata?.ajourholdstidspunkt ?: finneSisteEndringstidspunkt(navn.metadata)

        else -> {
            log.error { "Mottok ukjent datakilde: ${navn.metadata?.master} fra PDL" }
            secureLogger.error { "Mottok ukjent datakilde for opplysningstype Navn fra PDL: $navn" }
            throw PdlException(
                "Mottok ukjent datakilde: ${navn.metadata?.master} fra PDL",
                HttpStatus.INTERNAL_SERVER_ERROR,
            )
        }
    }

    data class HentIdenter(val identer: List<HentIdent> = emptyList()) {
        private fun getIdentByType(type: Identgruppe): String? {
            val ident = identer.filter { !it.historisk }.firstOrNull { ident -> ident.gruppe == type.name }
            return ident?.ident
        }

        fun getIdent() = Personident(getIdentByType(Identgruppe.FOLKEREGISTERIDENT) ?: getIdentByType(Identgruppe.NPID) ?: "")

        fun getAktorId() = getIdentByType(Identgruppe.AKTORID)

        fun hentAlleHistoriskeIdenter(): List<Personident>? = identer.filter { it.historisk }
            .filter { it.gruppe == Identgruppe.FOLKEREGISTERIDENT.name || it.gruppe == Identgruppe.NPID.name }
            .map { Personident(it.ident) }
            .ifEmpty { null }
    }

    data class HentIdent(val ident: String, val historisk: Boolean, val gruppe: String)

    data class HentPersonNavnFødselsdatoDødsfallResponse(
        val navn: List<Navn> = emptyList(),
        val folkeregisteridentifikator: List<Folkeregisteridentifikator> = emptyList(),
        val bostedsadresse: List<Bostedsadresse> = emptyList(),
        val foedselsdato: List<Fødselsdato> = emptyList(),
        val doedsfall: List<Dødsfall> = emptyList(),
    )

    data class HentPerson(
        val navn: List<Navn> = emptyList(),
        val kjoenn: List<HentPersonKjoenn> = emptyList(),
        val adressebeskyttelse: List<Adressebeskyttelse> = emptyList(),
        val oppholdsadresse: List<OppholdsadresseCommon> = emptyList(),
        val doedsfall: List<Dødsfall> = emptyList(),
        val foedselsdato: List<Fødselsdato> = emptyList(),
    )

    data class HentPersonKjoenn(val kjoenn: Kjønn)
}
