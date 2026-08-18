package no.nav.bidrag.person.query

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.neovisionaries.i18n.CountryCode.getByAlpha3Code
import no.nav.bidrag.commons.service.finnPoststedForPostnummer
import no.nav.bidrag.domene.enums.adresse.Adressetype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.land.Landkode2
import no.nav.bidrag.domene.land.Landkode3
import no.nav.bidrag.domene.util.trimToNull
import no.nav.bidrag.transport.person.PersonAdresseDto
import org.apache.commons.lang3.StringUtils
import java.time.LocalDate
import java.time.LocalDateTime

private val ukjentLandkode2 = Landkode2("UJ")
private val ukjentLandkode3 = Landkode3("UKJ")
private val landkodeNorge2 = Landkode2("NO")
private val landkodeNorge3 = Landkode3("NOR")

data class PersonAdresseQuery(val personIdent: Personident) : GraphQuery() {
    private val query = """
       query PersonQuery(${"$"}personId: ID!) 
        {
            hentPerson( ident:  ${"$"}personId) {
                  doedsfall {
                      doedsdato
                  }
                  navn(historikk: false) {
                        fornavn
                        mellomnavn
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
                  kontaktadresse(historikk: false) {
                        gyldigFraOgMed
                        gyldigTilOgMed
                        type
                        metadata {
                            master
                        }
                        coAdressenavn
                        postboksadresse {
                            postbokseier
                            postboks
                            postnummer
                        }
                        postadresseIFrittFormat {
                            adresselinje1
                            adresselinje2
                            adresselinje3
                            postnummer
                        }
                        utenlandskAdresseIFrittFormat {
                            adresselinje1
                            adresselinje2
                            adresselinje3
                            postkode
                            byEllerStedsnavn
                            landkode
                        }
                       vegadresse {
                           postnummer
                           adressenavn
                           husnummer
                           husbokstav
                           bruksenhetsnummer
                           tilleggsnavn
                           kommunenummer
                           bydelsnummer
                       }
                       utenlandskAdresse {
                           adressenavnNummer
                           bygningEtasjeLeilighet
                           postboksNummerNavn
                           postkode
                           bySted
                           regionDistriktOmraade
                           landkode
                       }
                }
                oppholdsadresse(historikk: false) {
                   gyldigFraOgMed
                   gyldigTilOgMed
                   metadata {
                      master
                   }
                   matrikkeladresse {
                    matrikkelId
                    bruksenhetsnummer
                    tilleggsnavn
                    postnummer
                    kommunenummer
                   }
                   vegadresse {
                       postnummer
                       adressenavn
                       husnummer
                       husbokstav
                        bruksenhetsnummer
                        tilleggsnavn
                        kommunenummer
                        bydelsnummer
                   }
                   utenlandskAdresse {
                       adressenavnNummer
                       bygningEtasjeLeilighet
                        postboksNummerNavn
                        postkode
                        bySted
                        regionDistriktOmraade
                        landkode
                   }
               }
               bostedsadresse(historikk: false) {
                   angittFlyttedato
                   gyldigFraOgMed
                   gyldigTilOgMed
                   coAdressenavn
                   ukjentBosted {
                       bostedskommune
                   }
                   metadata {
                       master
                   }
                   matrikkeladresse {
                        matrikkelId
                        bruksenhetsnummer
                        tilleggsnavn
                        postnummer
                        kommunenummer
                   }
                   vegadresse {
                       postnummer
                       adressenavn
                       husnummer
                       husbokstav
                       bruksenhetsnummer
                       tilleggsnavn
                       kommunenummer
                       bydelsnummer
                   }
                   utenlandskAdresse {
                       adressenavnNummer
                       bygningEtasjeLeilighet
                       postboksNummerNavn
                       postkode
                       bySted
                       regionDistriktOmraade
                       landkode
                   }
               }
        }
      }
        """

    override fun getQuery(): String = query

    override fun getVariables(): HashMap<String, Any> = hashMapOf("personId" to personIdent)
}

data class KontaktinformasjonForDødsbo(
    val skifteform: String,
    val attestutstedelsesdato: LocalDate,
    val adresse: KontaktinformasjonForDødsboAdresse,
    val personSomKontakt: PersonSomKontakt?,
    val advokatSomKontakt: AdvokatSomKontakt?,
    val organisasjonSomKontakt: OrganisasjonSomKontakt?,
)

data class KontaktinformasjonForDødsboAdresse(
    val adresselinje1: String,
    val adresselinje2: String?,
    val poststedsnavn: String,
    val postnummer: String,
    val landkode: String?,
)

data class PersonSomKontakt(val foedselsdato: LocalDate?, val personnavn: Navn?, val identifikasjonsnummer: String?)

data class AdvokatSomKontakt(val personnavn: Navn, val organisasjonsnavn: String?, val organisasjonsnummer: String?)

data class OrganisasjonSomKontakt(val kontaktperson: Navn?, val organisasjonsnavn: String, val organisasjonsnummer: String?)

data class MatrikkelAdresse(
    val matrikkelId: Long?,
    val bruksenhetsnummer: String?,
    val tilleggsnavn: String?,
    val postnummer: String?,
    val kommunenummer: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Kontaktadresse(
    val gyldigFraOgMed: LocalDateTime? = null,
    val gyldigTilOgMed: LocalDateTime? = null,
    val type: KontaktadresseType,
    var coAdressenavn: String? = null,
    val postboksadresse: Postboksadresse? = null,
    val vegadresse: Vegadresse? = null,
    val postadresseIFrittFormat: PostadresseIFrittFormat? = null,
    val utenlandskAdresse: UtenlandskAdresse? = null,
    val utenlandskAdresseIFrittFormat: UtenlandskAdresseIFrittFormat? = null,
    val metadata: Metadata,
) {
    fun isMasterPDL(): Boolean = Kilde.PDL == metadata.kilde

    init {
        coAdressenavn = if (coAdressenavn?.isEmpty() == true) null else coAdressenavn
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Oppholdsadresse(
    val gyldigFraOgMed: LocalDateTime? = null,
    val gyldigTilOgMed: LocalDateTime? = null,
    var coAdressenavn: String? = null,
    val utenlandskAdresse: UtenlandskAdresse? = null,
    val vegadresse: Vegadresse? = null,
    val matrikkeladresse: MatrikkelAdresse? = null,
    val metadata: Metadata,
    override val oppholdAnnetSted: OppholdAnnetSted? = null,
) : OppholdsadresseCommon(oppholdAnnetSted) {
    fun isMasterPDL(): Boolean = Kilde.PDL == metadata.kilde

    init {
        coAdressenavn = if (coAdressenavn?.isEmpty() == true) null else coAdressenavn
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Bostedsadresse(
    val angittFlyttedato: LocalDate? = null,
    val gyldigFraOgMed: LocalDateTime? = null,
    val gyldigTilOgMed: LocalDateTime? = null,
    var coAdressenavn: String? = null,
    val vegadresse: Vegadresse? = null,
    val utenlandskAdresse: UtenlandskAdresse? = null,
    val matrikkeladresse: MatrikkelAdresse? = null,
    val ukjentBosted: UkjentBosted? = null,
    val metadata: Metadata? = null,
) {
    fun isMasterPDL(): Boolean = Kilde.PDL == metadata?.kilde

    init {
        coAdressenavn = if (coAdressenavn?.isEmpty() == true) null else coAdressenavn
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PostadresseIFrittFormat(val adresselinje1: String?, val adresselinje2: String?, val adresselinje3: String?, val postnummer: String?)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UtenlandskAdresseIFrittFormat(
    val adresselinje1: String?,
    val adresselinje2: String?,
    val adresselinje3: String?,
    val postkode: String?,
    val byEllerStedsnavn: String?,
    val landkode: Landkode3?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Postboksadresse(val postbokseier: String?, val postboks: String?, val postnummer: String?)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UtenlandskAdresse(
    val adressenavnNummer: String?,
    val bygningEtasjeLeilighet: String?,
    val postboksNummerNavn: String?,
    val postkode: String?,
    val bySted: String?,
    val regionDistriktOmraade: String?,
    val landkode: Landkode3?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Vegadresse(
    val husnummer: String?,
    val husbokstav: String?,
    val bruksenhetsnummer: String?,
    val adressenavn: String?,
    val kommunenummer: String?,
    val bydelsnummer: String?,
    val tilleggsnavn: String?,
    val postnummer: String?,
    val matrikkelId: Long?,
)

enum class KontaktadresseType {
    Innland,
    Utland,
}

data class UkjentBosted(val bostedskommune: String?)

data class PersonAdresseResponse(var hentPerson: HentPersonAdresse) {
    fun hentPostadresse(): PersonAdresseDto? = if (isBostedsAdresseNewest()) {
        mapFromBosttedsAdresse()
    } else {
        mapFromKontaktAdresse() ?: mapFromOppholdsadresse() ?: mapFromBosttedsAdresse()
    }

    fun hentAlleAdresser(): List<PersonAdresseDto> = listOfNotNull(mapFromBosttedsAdresse(), mapFromOppholdsadresse(), mapFromKontaktAdresse())

    private fun mapAdresse(
        adressetype: Adressetype,
        vegadresse: Vegadresse?,
        matrikkeladresse: MatrikkelAdresse?,
        utenlandskAdresse: UtenlandskAdresse?,
        coAdressenavn: String?,
    ): PersonAdresseDto? = vegadresse?.let { mapVegadresse(adressetype, vegadresse, coAdressenavn) }
        ?: if (utenlandskAdresse?.landkode != null) {
            mapUtenlandskAdresse(adressetype, utenlandskAdresse, coAdressenavn)
        } else {
            matrikkeladresse?.let { mapMatrikkelAdresse(adressetype, matrikkeladresse, coAdressenavn) }
        }

    private fun mapFromBosttedsAdresse(): PersonAdresseDto? {
        val bostedsadresse = hentBostedsAdresse()
        return if (bostedsadresse != null) {
            mapAdresse(
                Adressetype.BOSTEDSADRESSE,
                bostedsadresse.vegadresse,
                bostedsadresse.matrikkeladresse,
                bostedsadresse.utenlandskAdresse,
                bostedsadresse.coAdressenavn,
            )
        } else {
            null
        }
    }

    private fun mapFromOppholdsadresse(): PersonAdresseDto? {
        val oppholdsadresse = hentOppholdsAdresse()
        return if (oppholdsadresse != null) {
            mapAdresse(
                Adressetype.OPPHOLDSADRESSE,
                oppholdsadresse.vegadresse,
                oppholdsadresse.matrikkeladresse,
                oppholdsadresse.utenlandskAdresse,
                oppholdsadresse.coAdressenavn,
            )
        } else {
            null
        }
    }

    private fun mapMatrikkelAdresse(adressetype: Adressetype, matrikkelAdresse: MatrikkelAdresse, coAdressenavn: String?): PersonAdresseDto = PersonAdresseDto(
        adressetype = adressetype,
        adresselinje1 = (coAdressenavn ?: matrikkelAdresse.tilleggsnavn),
        adresselinje2 = (if (coAdressenavn != null) matrikkelAdresse.tilleggsnavn else null),
        bruksenhetsnummer = matrikkelAdresse.bruksenhetsnummer,
        postnummer = matrikkelAdresse.postnummer,
        poststed = matrikkelAdresse.postnummer?.let { hentPoststedFraPostnummer(it) },
        land = landkodeNorge2,
        land3 = landkodeNorge3,
    )

    private fun mapUtenlandskAdresse(adressetype: Adressetype, utenlandskAdresse: UtenlandskAdresse, coAdressenavn: String?): PersonAdresseDto {
        val postboksAdressenavn = utenlandskAdresse.postboksNummerNavn ?: utenlandskAdresse.adressenavnNummer
        var adresseLinje1 = postboksAdressenavn
        if (StringUtils.isNotEmpty(utenlandskAdresse.bygningEtasjeLeilighet)) {
            adresseLinje1 =
                if (adresseLinje1 != null) "$adresseLinje1, ${utenlandskAdresse.bygningEtasjeLeilighet}" else utenlandskAdresse.bygningEtasjeLeilighet
        }
        if (StringUtils.isNotEmpty(utenlandskAdresse.regionDistriktOmraade)) {
            adresseLinje1 =
                if (adresseLinje1 != null) "$adresseLinje1, ${utenlandskAdresse.regionDistriktOmraade}" else utenlandskAdresse.regionDistriktOmraade
        }
        val postKodeOgSted = "${utenlandskAdresse.postkode ?: ""} ${utenlandskAdresse.bySted ?: ""}".trim()
        return PersonAdresseDto(
            adressetype = adressetype,
            adresselinje1 = (coAdressenavn ?: adresseLinje1),
            adresselinje2 = (if (coAdressenavn != null) adresseLinje1 else postKodeOgSted),
            adresselinje3 = (if (coAdressenavn != null) postKodeOgSted else null),
            land = convertLandkode(utenlandskAdresse.landkode) ?: ukjentLandkode2,
            land3 = convertLandkode3(utenlandskAdresse.landkode) ?: utenlandskAdresse.landkode ?: ukjentLandkode3,
        )
    }

    private fun mapVegadresse(adressetype: Adressetype, vegadresse: Vegadresse, coAdressenavn: String?): PersonAdresseDto {
        val adresseLinje = "${vegadresse.adressenavn ?: ""} ${vegadresse.husnummer ?: ""}${vegadresse.husbokstav ?: ""}".trim()
        return PersonAdresseDto(
            adressetype = adressetype,
            adresselinje1 = if (coAdressenavn != null) "c/o $coAdressenavn" else adresseLinje,
            adresselinje2 = (if (coAdressenavn != null) adresseLinje else null),
            bruksenhetsnummer = vegadresse.bruksenhetsnummer,
            postnummer = vegadresse.postnummer,
            poststed = hentPoststedFraPostnummer(vegadresse.postnummer),
            land = landkodeNorge2,
            land3 = landkodeNorge3,
        )
    }

    private fun mapFromKontaktAdresse(): PersonAdresseDto? {
        val kontaktAdresse = hentKontaktAdresse()
        return if (kontaktAdresse != null) {
            mapNorskAdresseFromKontaktAdresse(Adressetype.KONTAKTADRESSE, kontaktAdresse)
                ?: mapUtenlandskAdresseFromKontaktAdresse(Adressetype.KONTAKTADRESSE, kontaktAdresse, kontaktAdresse.coAdressenavn)
                ?: mapAdresse(
                    Adressetype.KONTAKTADRESSE,
                    kontaktAdresse.vegadresse,
                    null,
                    kontaktAdresse.utenlandskAdresse,
                    kontaktAdresse.coAdressenavn,
                )
        } else {
            null
        }
    }

    private fun mapUtenlandskAdresseFromKontaktAdresse(
        adressetype: Adressetype,
        kontaktAdresse: Kontaktadresse,
        coAdressenavn: String?,
    ): PersonAdresseDto? {
        if (kontaktAdresse.utenlandskAdresse?.landkode != null) {
            return mapUtenlandskAdresse(adressetype, kontaktAdresse.utenlandskAdresse, coAdressenavn)
        } else if (kontaktAdresse.utenlandskAdresseIFrittFormat?.landkode != null) {
            val frittFormat = kontaktAdresse.utenlandskAdresseIFrittFormat
            val adresselinjer =
                listOfNotNull(
                    frittFormat.adresselinje1.trimToNull(),
                    frittFormat.adresselinje2.trimToNull(),
                    frittFormat.adresselinje3.trimToNull(),
                )
            return PersonAdresseDto(
                adressetype = adressetype,
                adresselinje1 = adresselinjer.firstOrNull(),
                adresselinje2 = adresselinjer.getOrNull(1),
                adresselinje3 = adresselinjer.getOrNull(2),
                poststed = frittFormat.byEllerStedsnavn,
                land = convertLandkode(frittFormat.landkode) ?: ukjentLandkode2,
                land3 = frittFormat.landkode ?: ukjentLandkode3,
            )
        }
        return null
    }

    private fun mapNorskAdresseFromKontaktAdresse(adressetype: Adressetype, kontaktAdresse: Kontaktadresse): PersonAdresseDto? {
        if (kontaktAdresse.vegadresse != null) {
            return mapVegadresse(adressetype, kontaktAdresse.vegadresse, kontaktAdresse.coAdressenavn)
        } else if (kontaktAdresse.postadresseIFrittFormat != null) {
            val frittFormat = kontaktAdresse.postadresseIFrittFormat
            val coAdressenavn = kontaktAdresse.coAdressenavn
            val adresselinjer =
                listOfNotNull(coAdressenavn, frittFormat.adresselinje1, frittFormat.adresselinje2, frittFormat.adresselinje3)
            return PersonAdresseDto(
                adressetype = adressetype,
                adresselinje1 = adresselinjer.firstOrNull(),
                adresselinje2 = adresselinjer.getOrNull(1),
                adresselinje3 = adresselinjer.getOrNull(2),
                postnummer = frittFormat.postnummer,
                poststed = hentPoststedFraPostnummer(frittFormat.postnummer),
                land = landkodeNorge2,
                land3 = landkodeNorge3,
            )
        } else if (kontaktAdresse.postboksadresse != null) {
            val postboksadresse = kontaktAdresse.postboksadresse
            return PersonAdresseDto(
                adressetype = Adressetype.BOSTEDSADRESSE,
                adresselinje1 = postboksadresse.postbokseier ?: "Postboks ${postboksadresse.postboks}",
                adresselinje2 =
                (if (postboksadresse.postbokseier == null) null else "Postboks ${postboksadresse.postboks}"),
                postnummer = postboksadresse.postnummer,
                poststed = hentPoststedFraPostnummer(postboksadresse.postnummer),
                land = landkodeNorge2,
                land3 = landkodeNorge3,
            )
        }
        return null
    }

    private fun hasKontaktInformasjonDoedsbo(): Boolean = hentPerson.kontaktinformasjonForDoedsbo.isNotEmpty()

    private fun isPersonDod(): Boolean = hentPerson.doedsfall.isNotEmpty()

    private fun isBostedsAdresseNewest(): Boolean {
        val bostedsadresse = hentBostedsAdresse()
        val kontaktAdresse = hentKontaktAdresse()
        val oppholdsadresse = hentOppholdsAdresse()
        val bostedsadresseDato = bostedsadresse?.gyldigFraOgMed ?: bostedsadresse?.angittFlyttedato?.atStartOfDay() ?: return false
        return (
            (kontaktAdresse?.gyldigFraOgMed != null) &&
                (bostedsadresseDato > kontaktAdresse.gyldigFraOgMed)
            ) ||
            (
                (oppholdsadresse?.gyldigFraOgMed != null) &&
                    (bostedsadresseDato > oppholdsadresse.gyldigFraOgMed)
                )
    }

    private fun hentKontaktAdresse(): Kontaktadresse? = hentPerson.kontaktadresse.sortedWith(
        compareByDescending<Kontaktadresse> { it.isMasterPDL() }.thenByDescending { it.gyldigFraOgMed },
    )
        .firstOrNull()

    private fun hentOppholdsAdresse(): Oppholdsadresse? = hentPerson.oppholdsadresse.sortedWith(
        compareByDescending<Oppholdsadresse> { it.isMasterPDL() }.thenByDescending { it.gyldigFraOgMed },
    )
        .firstOrNull()

    private fun hentBostedsAdresse(): Bostedsadresse? = hentPerson.bostedsadresse.sortedWith(compareByDescending { it.gyldigFraOgMed ?: it.angittFlyttedato }).firstOrNull()

    private fun getLandFromLandkode(landkode: String): String? = if ("XXK" == landkode) "Kosovo" else getByAlpha3Code(landkode).getName()

    private fun convertLandkode(landkode: Landkode3?): Landkode2? = if ("XXK" == landkode?.verdi) Landkode2("XK") else getByAlpha3Code(landkode?.verdi)?.alpha2?.let { Landkode2(it) }

    private fun convertLandkode3(landkode: Landkode3?): Landkode3? = getByAlpha3Code(landkode?.verdi)?.alpha3?.let { Landkode3(it) }

    private fun hentPoststedFraPostnummer(postnummer: String?): String? = postnummer?.let { finnPoststedForPostnummer(it) }
}

data class HentPersonAdresse(
    val navn: List<Navn> = emptyList(),
    val kontaktadresse: List<Kontaktadresse> = emptyList(),
    val oppholdsadresse: List<Oppholdsadresse> = emptyList(),
    val bostedsadresse: List<Bostedsadresse> = emptyList(),
    val doedsfall: List<Dødsfall> = emptyList(),
    val kontaktinformasjonForDoedsbo: List<KontaktinformasjonForDødsbo> = emptyList(),
)
