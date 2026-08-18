package no.nav.bidrag.person.query

import no.nav.bidrag.domene.enums.person.Skifteform
import no.nav.bidrag.domene.land.Landkode3
import no.nav.bidrag.transport.person.DødsboDto
import no.nav.bidrag.transport.person.DødsboKontaktadresse
import no.nav.bidrag.transport.person.PersonAdresseDto
import no.nav.bidrag.transport.person.PersonDto

class PersondetaljerQuery(val personIdent: String) : GraphQuery() {
    private val query = """
    query PersonQuery(${"$"}personId: ID!) {
      hentIdenter(ident: ${"$"}personId, historikk: true) {
        identer {
          ident
          historisk
          gruppe
        }
      }
      hentPerson(ident: ${"$"}personId) {
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
          oppholdAnnetSted
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
        doedsfall {
          doedsdato
        }
        kjoenn(historikk: false) {
          kjoenn
        }
        foedselsdato {
          foedselsdato
          foedselsaar
        }
        adressebeskyttelse {
          gradering
        }
        kontaktinformasjonForDoedsbo {
          skifteform
          attestutstedelsesdato
          adresse {
            adresselinje1
            adresselinje2
            poststedsnavn
            postnummer
            landkode
          }
          personSomKontakt {
            foedselsdato
            personnavn {
              fornavn
              mellomnavn
              etternavn
            }
            identifikasjonsnummer
          }
          advokatSomKontakt {
            personnavn {
              fornavn
              mellomnavn
              etternavn
            }
            organisasjonsnavn
            organisasjonsnummer
          }
          organisasjonSomKontakt {
            kontaktperson {
              fornavn
              mellomnavn
              etternavn
            }
            organisasjonsnavn
            organisasjonsnummer
          }
        }
      }
    }
    """

    override fun getQuery(): String = query

    override fun getVariables(): Map<String, Any> = hashMapOf("personId" to personIdent)
}

data class PersondetaljerResponse(val hentIdenter: PersonResponse.HentIdenter, val hentPerson: HentPersondetaljer) {
    fun mapTilDødsboDto(): DødsboDto? {
        hentPerson.kontaktinformasjonForDoedsbo.firstOrNull()?.let {
            return DødsboDto(
                skifteform = Skifteform.valueOf(it.skifteform),
                attestutstedelsesdato = it.attestutstedelsesdato,
                kontaktadresse =
                DødsboKontaktadresse(
                    adresselinje1 = it.adresse.adresselinje1,
                    adresselinje2 = it.adresse.adresselinje2?.let { adresselinje2 -> adresselinje2 },
                    postnummer = it.adresse.postnummer,
                    poststed = it.adresse.poststedsnavn,
                    land3 = it.adresse.landkode?.let { landkode -> Landkode3(landkode) },
                ),
                kontaktperson = hentDødsboKontaktperson(it),
            )
        }
        return null
    }

    fun mapTilPersonAdresseDto(): PersonAdresseDto? = PersonAdresseResponse(
        hentPerson =
        HentPersonAdresse(
            navn = hentPerson.navn,
            kontaktadresse = hentPerson.kontaktadresse,
            oppholdsadresse = hentPerson.oppholdsadresse,
            bostedsadresse = hentPerson.bostedsadresse,
            doedsfall = hentPerson.doedsfall,
            kontaktinformasjonForDoedsbo = hentPerson.kontaktinformasjonForDoedsbo,
        ),
    ).hentPostadresse()

    fun mapTilPersonDto(): PersonDto = PersonResponse(
        hentPerson =
        PersonResponse.HentPerson(
            navn = hentPerson.navn,
            kjoenn = hentPerson.kjoenn,
            adressebeskyttelse = hentPerson.adressebeskyttelse,
            oppholdsadresse = hentPerson.oppholdsadresse,
            doedsfall = hentPerson.doedsfall,
            foedselsdato = hentPerson.foedselsdato,
        ),
        hentIdenter = hentIdenter,
    ).mapToPersonDto()

    private fun hentDødsboKontaktperson(dødsbo: KontaktinformasjonForDødsbo): String {
        if (dødsbo.personSomKontakt != null) {
            return if (dødsbo.personSomKontakt.personnavn != null) {
                "${dødsbo.personSomKontakt.personnavn.fornavn} " +
                    dødsbo.personSomKontakt.personnavn.etternavn.trim()
            } else {
                "${dødsbo.personSomKontakt.identifikasjonsnummer}"
            }
        } else if (dødsbo.advokatSomKontakt != null) {
            return "${dødsbo.advokatSomKontakt.personnavn.fornavn} " +
                dødsbo.advokatSomKontakt.personnavn.etternavn.trim()
        } else if (dødsbo.organisasjonSomKontakt != null) {
            return if (dødsbo.organisasjonSomKontakt.kontaktperson != null) {
                "${dødsbo.organisasjonSomKontakt.kontaktperson.fornavn} " +
                    dødsbo.organisasjonSomKontakt.kontaktperson.etternavn.trim()
            } else {
                dødsbo.organisasjonSomKontakt.organisasjonsnavn
            }
        } else {
            error("Svar på dødsbo fra PDL mangler informasjon om kontaktperson.")
        }
    }
}

data class HentPersondetaljer(
    val navn: List<Navn> = emptyList(),
    val kontaktadresse: List<Kontaktadresse> = emptyList(),
    val oppholdsadresse: List<Oppholdsadresse> = emptyList(),
    val bostedsadresse: List<Bostedsadresse> = emptyList(),
    val doedsfall: List<Dødsfall> = emptyList(),
    val kjoenn: List<PersonResponse.HentPersonKjoenn> = emptyList(),
    val foedselsdato: List<Fødselsdato> = emptyList(),
    val adressebeskyttelse: List<Adressebeskyttelse> = emptyList(),
    val kontaktinformasjonForDoedsbo: List<KontaktinformasjonForDødsbo> = emptyList(),
)
