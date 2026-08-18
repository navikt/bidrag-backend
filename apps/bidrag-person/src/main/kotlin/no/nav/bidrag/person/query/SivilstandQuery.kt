package no.nav.bidrag.person.query

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.bidrag.domene.enums.person.SivilstandskodePDL
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.SivilstandPdlDto
import no.nav.bidrag.transport.person.SivilstandPdlHistorikkDto
import java.time.LocalDate

data class SivilstandQuery(val personId: Personident) : GraphQuery() {
    private val query = """
        query SivilstandQuery(${"$"}personId: ID!) 
        {
            hentPerson(ident: ${"$"}personId) {
                sivilstand(historikk: true) {
                   type
                   gyldigFraOgMed
                   bekreftelsesdato
                   metadata {
                      master
                      endringer {
                          type
                          registrert
                      }
                      historisk
                   }
                }
            }
        }
        """

    override fun getQuery(): String = query

    override fun getVariables(): HashMap<String, Any> = hashMapOf("personId" to personId)
}

data class SivilstandResponse(
    @JsonProperty("hentPerson")
    var hentSivilstand: HentSivilstand,
) {
    fun mapToSivilstandPdlHistorikkDto(): SivilstandPdlHistorikkDto = SivilstandPdlHistorikkDto(tilSivilstandPdlDto(hentSivilstand.sivilstand))

    private fun tilSivilstandPdlDto(sivilstand: List<Sivilstand>): List<SivilstandPdlDto> = sivilstand.map {
        SivilstandPdlDto(
            type = it.type,
            gyldigFom = it.gyldigFraOgMed,
            relatertVedSivilstand = it.relatertVedSivilstand,
            bekreftelsesdato = it.bekreftelsesdato,
            master = it.metadata?.master,
            registrert = it.metadata?.endringer?.last()?.registrert,
            historisk = it.metadata?.historisk,
        )
    }
}

data class HentSivilstand(val sivilstand: List<Sivilstand>)

data class Sivilstand(
    val type: SivilstandskodePDL?,
    val gyldigFraOgMed: LocalDate?,
    val relatertVedSivilstand: String?,
    val bekreftelsesdato: LocalDate?,
    val metadata: MetadataSivilstand?,
)

data class MetadataSivilstand(var master: String?, var endringer: List<Endring>?, var historisk: Boolean?)
