package no.nav.bidrag.person.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.security.SikkerhetsKontekst
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.person.Familierelasjon
import no.nav.bidrag.domene.enums.person.Gradering
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.person.BidragPerson
import no.nav.bidrag.person.bo.BarnBostedsadresserBo
import no.nav.bidrag.person.consumer.KontoregisterConsumer
import no.nav.bidrag.person.consumer.KrrConsumer
import no.nav.bidrag.person.consumer.PDLConsumer
import no.nav.bidrag.person.consumer.SkjermingConsumer
import no.nav.bidrag.person.query.Bostedsadresse
import no.nav.bidrag.person.query.GeografiskTilknytningResponse
import no.nav.bidrag.person.query.HentIdenterResponse
import no.nav.bidrag.person.query.HentPersonBostedsadresse
import no.nav.bidrag.person.query.NavnFødselsdatoDødsfallResponse
import no.nav.bidrag.person.query.PersonFødsel
import no.nav.bidrag.person.query.PersonGradering
import no.nav.bidrag.person.query.PersonResponse
import no.nav.bidrag.transport.person.ForelderBarnRelasjon
import no.nav.bidrag.transport.person.ForelderBarnRelasjonDto
import no.nav.bidrag.transport.person.Fødselsdatoer
import no.nav.bidrag.transport.person.GeografiskTilknytningDto
import no.nav.bidrag.transport.person.Graderingsinfo
import no.nav.bidrag.transport.person.Husstand
import no.nav.bidrag.transport.person.Husstandsmedlem
import no.nav.bidrag.transport.person.HusstandsmedlemmerDto
import no.nav.bidrag.transport.person.Identgruppe
import no.nav.bidrag.transport.person.KontonummerDto
import no.nav.bidrag.transport.person.MotpartBarnRelasjon
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import no.nav.bidrag.transport.person.NavnFødselDødDto
import no.nav.bidrag.transport.person.PersonAdresseDto
import no.nav.bidrag.transport.person.PersonDto
import no.nav.bidrag.transport.person.PersondetaljerDto
import no.nav.bidrag.transport.person.PersonidentDto
import no.nav.bidrag.transport.person.SivilstandPdlHistorikkDto
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

@Component
class PersonService(
    private val krrConsumer: KrrConsumer,
    private val kontoregisterConsumer: KontoregisterConsumer,
    private val pdlConsumer: PDLConsumer,
    private val skjermingConsumer: SkjermingConsumer,
) {

    fun hentFødselsdatoer(identer: Set<Personident>): Fødselsdatoer {
        val personFødselMap: Map<Personident, PersonFødsel> = pdlConsumer.hentFødselsdatoer(identer)
        val identTilFødselsdatoMap = personFødselMap.entries.associateBy({ it.key }, { it.value.tilFødselsdato() })
        return Fødselsdatoer(identTilFødselsdatoMap)
    }

    fun hentGraderinger(identer: Set<Personident>): Graderingsinfo {
        val graderingsinfo = SikkerhetsKontekst.medApplikasjonKontekst {
            val personGraderingerMap: Map<Personident, PersonGradering> = pdlConsumer.hentGraderinger(identer)

            val identTilGraderingMap: Map<Personident, Gradering?> =
                personGraderingerMap.entries.associateBy(
                    { it.key },
                    { it.value.tilGradering() },
                )
            val personSkjermingMap: Map<Personident, Boolean> = skjermingConsumer.erPersonerSkjermet(identer)
            Graderingsinfo(identTilGraderingMap, personSkjermingMap)
        }
        return graderingsinfo
    }

    fun hentGeografiskTilknytningData(personident: Personident): GeografiskTilknytningDto {
        val muligGeografiskTilknytning: GeografiskTilknytningResponse = pdlConsumer.hentGeografiskTilknytning(personident)
        return muligGeografiskTilknytning.mapToGeografiskTilknytningDto()
    }

    fun hentSivilstand(personident: Personident): SivilstandPdlHistorikkDto {
        val muligSivilstand = pdlConsumer.hentSivilstand(personident)
        return muligSivilstand.mapToSivilstandPdlHistorikkDto()
    }

    fun hentForelderBarnRelasjon(personident: Personident): ForelderBarnRelasjonDto {
        val forelderBarnRelasjoner = pdlConsumer.hentForelderBarnRelasjoner(personident)
        return ForelderBarnRelasjonDto(forelderBarnRelasjoner)
    }

    fun hentNavnFoedselDoed(personident: Personident): NavnFødselDødDto {
        val muligNavnFoedselDoed: NavnFødselsdatoDødsfallResponse = pdlConsumer.hentNavnFødselsdatoDødsfall(personident)
        return muligNavnFoedselDoed.mapToNavnFødselsdatoDødsfallDto(personident)
    }

    fun hentHusstandsmedlemmer(personident: Personident, periodeFra: LocalDate?): HusstandsmedlemmerDto {
        BidragPerson.SECURE_LOGGER.debug("Henter husstandsmedlemmer for person {} periodeFra {} - START", personident, periodeFra)
        BidragPerson.SECURE_LOGGER.debug("hentPersonBostedsadresse for person {} - START", personident)
        val hentPersonBostedsadresse: HentPersonBostedsadresse = pdlConsumer.hentPersonBostedsadresse(personident).hentPerson
        BidragPerson.SECURE_LOGGER.debug("hentPersonBostedsadresse for person {} - SLUTT", personident)
        // Sorterer liste med bosteder for personen som det skal hentes husstandsmedlemmer for.
        // Setter gyldigTilOgMed dato lik gyldigFraOgMed på neste forekomst for finne faktiske perioder personen har bodd i husstanden.
        // PDL setter kun gyldigTilDato hvis personen ikke lenger har adresse i Norge
        val sortertOgJustertBostedsadresseListe = sorterOgJusterBostedsadresser(hentPersonBostedsadresse.bostedsadresse)

        val husstandListe = mutableListOf<Husstand>()
        val husstandsmedlemBostedsadresserCache = mutableMapOf<Personident, List<Bostedsadresse>>()

        val fradato = periodeFra ?: LocalDate.now().minusYears(1)

        BidragPerson.SECURE_LOGGER.debug(
            "sortertOgJustertBostedsadresseListe for person: {} periodeFra: {} {}",
            personident,
            periodeFra,
            sortertOgJustertBostedsadresseListe,
        )

        sortertOgJustertBostedsadresseListe
            .filterNot { it.vegadresse == null }
            .filter { it.gyldigTilOgMed == null || it.gyldigTilOgMed.toLocalDate().plusDays(1).isAfter(fradato) }
            .forEach { bostedsadresse ->
                val husstandsmedlemmer = hentHusstandsmedlemmer(bostedsadresse)
                // For å kunne vite når et husstandsmedlem har flyttet ut av aktuell husstand så må alle bosteder for personen hentes og sorteres, og
                // gyldigTilOgMed settes lik gyldigFraOgMed til neste forekomst. GyldigTilOgMed har kun verdi i PDL hvis personen ikke lenger har
                // en registrert adresse i Norge.
                BidragPerson.SECURE_LOGGER.debug("Hentede husstandsmedlemmer for {}: {}", personident, husstandsmedlemmer)

                val husstandsmedlemListe = husstandsmedlemmer.flatMap { husstandsmedlem ->
                    val sortertOgJustertHusstandsmedlemBostedsadresseListe =
                        husstandsmedlemBostedsadresserCache.getOrPut(husstandsmedlem.personId) {
                            BidragPerson.SECURE_LOGGER.debug("hentPersonBostedsadresse for husstandsmedlem {} - START", husstandsmedlem.personId)
                            val husstandsmedlemBostedsadresseListe =
                                pdlConsumer.hentPersonBostedsadresse(husstandsmedlem.personId).hentPerson.bostedsadresse
                                    .filter { it.vegadresse != null }
                            BidragPerson.SECURE_LOGGER.debug("hentPersonBostedsadresse for husstandsmedlem {} - SLUTT", husstandsmedlem.personId)

                            sorterOgJusterBostedsadresser(husstandsmedlemBostedsadresseListe)
                        }

                    sortertOgJustertHusstandsmedlemBostedsadresseListe
                        .filter { it.vegadresse == bostedsadresse.vegadresse && delerHusstand(it, bostedsadresse) }
                        .map { husstandsmedlemBosted ->
                            Husstandsmedlem(
                                // Justerer slik at periode ikke begynnner før gyldigFraOgMed for bostedsadressen
                                gyldigFraOgMed = finnHusstandsmedlemGyldigFraOgMedDato(husstandsmedlemBosted, bostedsadresse)?.toLocalDate(),
                                // Justerer slik at sluttdato for husstandsmedlemskapsperiode ikke er etter gyldigTilOgMed for bostedsadressen
                                gyldigTilOgMed = finnHusstandsmedlemGyldigTilOgMedDato(
                                    husstandsmedlem.dødsdato,
                                    husstandsmedlemBosted,
                                    bostedsadresse,
                                )?.toLocalDate(),
                                personId = husstandsmedlem.personId,
                                navn = husstandsmedlem.navn,
                                fødselsdato = husstandsmedlem.fødselsdato,
                                dødsdato = husstandsmedlem.dødsdato,
                            )
                        }
                }

                husstandListe.add(
                    Husstand(
                        gyldigFraOgMed = bostedsadresse.gyldigFraOgMed?.toLocalDate(),
                        gyldigTilOgMed = bostedsadresse.gyldigTilOgMed?.toLocalDate(),
                        adressenavn = bostedsadresse.vegadresse?.adressenavn,
                        husnummer = bostedsadresse.vegadresse?.husnummer,
                        husbokstav = bostedsadresse.vegadresse?.husbokstav,
                        bruksenhetsnummer = bostedsadresse.vegadresse?.bruksenhetsnummer,
                        postnummer = bostedsadresse.vegadresse?.postnummer,
                        bydelsnummer = bostedsadresse.vegadresse?.bydelsnummer,
                        kommunenummer = bostedsadresse.vegadresse?.kommunenummer,
                        matrikkelId = bostedsadresse.vegadresse?.matrikkelId,
                        husstandsmedlemListe = husstandsmedlemListe,
                    ),
                )
            }
        BidragPerson.SECURE_LOGGER.debug("Henter husstandsmedlemmer for person {} - SLUTT", personident)
        return HusstandsmedlemmerDto(husstandListe)
    }

    fun hentHusstandsmedlemskapEgneBarn(personident: Personident, periodeFra: LocalDate?): HusstandsmedlemmerDto {
        BidragPerson.SECURE_LOGGER.debug("Henter husstandsmedlemskap for den voksnes egne barn {} periodeFra {} - START", personident, periodeFra)
        BidragPerson.SECURE_LOGGER.debug("HusstandsmedlemskapBarn - hentPersonBostedsadresse for voksen {} - START", personident)

        val voksensBostedsadresseListe: HentPersonBostedsadresse = pdlConsumer.hentPersonBostedsadresse(personident).hentPerson

        BidragPerson.SECURE_LOGGER.debug("HusstandsmedlemskapBarn -hentPersonBostedsadresse for voksen {} - SLUTT", personident)

        val fradato = periodeFra ?: LocalDate.now().minusYears(1)

        // Sorterer liste med bosteder for personen som det skal hentes husstandsmedlemskapEgneBarn for.
        // Setter gyldigTilOgMed dato lik gyldigFraOgMed på neste forekomst for finne faktiske perioder personen har bodd i husstanden.
        // PDL setter kun gyldigTilDato hvis personen ikke lenger har adresse i Norge
        val sortertOgJustertVoksensBostedsadresseListe =
            sorterOgJusterBostedsadresser(voksensBostedsadresseListe.bostedsadresse)
                .filter { it.vegadresse != null }
                .filter { it.gyldigTilOgMed == null || it.gyldigTilOgMed.toLocalDate().plusDays(1).isAfter(fradato) }

        val husstandListe = mutableListOf<Husstand>()

        BidragPerson.SECURE_LOGGER.debug(
            "HusstandsmedlemskapEgneBarn -sortertOgJustertVoksensBostedsadresseListe for voksen: {} periodeFra: {} {}",
            personident,
            periodeFra,
            sortertOgJustertVoksensBostedsadresseListe,
        )

        // Henter alle barn for angitt person
        val barnListe = pdlConsumer.hentForelderBarnRelasjoner(personident)
            .filter { it.relatertPersonsRolle == Familierelasjon.BARN && it.relatertPersonsIdent != null }
            .map { it.relatertPersonsIdent }

        // Henter nyeste ident for alle barn for å filterere bort feiloppføringer i folkeregisteret der man får samme barn flere ganger med ny og gammel ident.
/*        val barnListeKunNyesteIdent =
            barnListe.mapNotNull { it?.verdi }.mapNotNull { hentePersonidenter(it, false, setOf(Identgruppe.FOLKEREGISTERIDENT)).firstOrNull() }
                .toSet()*/
/*
        BidragPerson.SECURE_LOGGER.debug("barnListe før bortfiltrering av duplikate personer: {} ", barnListe)
        BidragPerson.SECURE_LOGGER.debug("barnListeKunNyesteIdent etter bortfiltrering av duplikate personer: {} ", barnListeKunNyesteIdent)*/

        val barnBostedsadresseListe = mutableListOf<BarnBostedsadresserBo>()

        barnListe.forEach { barn ->

            // Henter personinfo for barn
            val navnFødselsdatoDødsfall = hentNavnFoedselDoed(barn!!)

            // Henter bostedsadressehistorikk for barnet
            val barnetsBostedsadresseListe = pdlConsumer.hentPersonBostedsadresse(barn).hentPerson.bostedsadresse

            val sortertOgJustertBarnBostedsadresseListe =
                sorterOgJusterBostedsadresser(barnetsBostedsadresseListe)
                    .filter { it.vegadresse != null }
                    .filter { it.gyldigTilOgMed == null || it.gyldigTilOgMed.toLocalDate().plusDays(1).isAfter(fradato) }

            barnBostedsadresseListe.add(
                BarnBostedsadresserBo(
                    personId = barn,
                    navn = navnFødselsdatoDødsfall.navn,
                    fødselsdato = navnFødselsdatoDødsfall.fødselsdato,
                    dødsdato = navnFødselsdatoDødsfall.dødsdato,
                    bostedsadresseListe = sortertOgJustertBarnBostedsadresseListe,
                ),
            )
        }

        sortertOgJustertVoksensBostedsadresseListe
            .forEach { bostedsadresseVoksen ->
                val barnHusstandsmedlemskapListe = mutableListOf<Husstandsmedlem>()

                barnBostedsadresseListe.forEach { barn ->
                    barn.bostedsadresseListe.forEach { adresseBarn ->
                        if (adresseBarn.vegadresse == bostedsadresseVoksen.vegadresse &&
                            delerHusstand(adresseBarn, bostedsadresseVoksen)
                        ) {
                            barnHusstandsmedlemskapListe.add(
                                Husstandsmedlem(
                                    // Justerer slik at periode ikke begynnner før gyldigFraOgMed for bostedsadressen
                                    gyldigFraOgMed = finnHusstandsmedlemGyldigFraOgMedDato(adresseBarn, bostedsadresseVoksen)?.toLocalDate(),
                                    // Justerer slik at sluttdato for husstandsmedlemskapsperiode ikke er etter gyldigTilOgMed for bostedsadressen
                                    gyldigTilOgMed = finnHusstandsmedlemGyldigTilOgMedDato(
                                        barn.dødsdato,
                                        adresseBarn,
                                        bostedsadresseVoksen,
                                    )?.toLocalDate(),
                                    personId = barn.personId,
                                    navn = barn.navn ?: "",
                                    fødselsdato = barn.fødselsdato,
                                    dødsdato = barn.dødsdato,
                                ),
                            )
                        }
                    }
                }

                husstandListe.add(
                    Husstand(
                        gyldigFraOgMed = bostedsadresseVoksen.gyldigFraOgMed?.toLocalDate(),
                        gyldigTilOgMed = bostedsadresseVoksen.gyldigTilOgMed?.toLocalDate(),
                        adressenavn = bostedsadresseVoksen.vegadresse?.adressenavn,
                        husnummer = bostedsadresseVoksen.vegadresse?.husnummer,
                        husbokstav = bostedsadresseVoksen.vegadresse?.husbokstav,
                        bruksenhetsnummer = bostedsadresseVoksen.vegadresse?.bruksenhetsnummer,
                        postnummer = bostedsadresseVoksen.vegadresse?.postnummer,
                        bydelsnummer = bostedsadresseVoksen.vegadresse?.bydelsnummer,
                        kommunenummer = bostedsadresseVoksen.vegadresse?.kommunenummer,
                        matrikkelId = bostedsadresseVoksen.vegadresse?.matrikkelId,
                        husstandsmedlemListe = barnHusstandsmedlemskapListe,

                    ),
                )
            }
        BidragPerson.SECURE_LOGGER.debug("Henter husstandsmedlemskapBarn for voksen {} - SLUTT", personident)
        return HusstandsmedlemmerDto(husstandListe)
    }

    fun hentPersonInfo(ident: Personident): PersonDto {
        secureLogger.debug { "Henter personinfo for person ${ident.verdi}" }
        val personResponse: PersonResponse = pdlConsumer.hentPersonInfo(ident)
        return personResponse.mapToPersonDto()
    }

    fun hentPersonPostadresse(personident: Personident): PersonAdresseDto? {
        BidragPerson.SECURE_LOGGER.debug("Henter person postadresse for person {}", personident)
        val personAdresse = pdlConsumer.hentPersonAdresse(personident)
        return personAdresse.hentPostadresse()
    }

    fun hentPersonAdresser(personident: Personident): List<PersonAdresseDto> {
        BidragPerson.SECURE_LOGGER.debug("Henter person adresse for person {}", personident)
        val personAdresse = pdlConsumer.hentPersonAdresse(personident)
        return personAdresse.hentAlleAdresser()
    }

    fun hentPersonSpraak(personident: Personident): String? = krrConsumer.hentPersonSpraak(personident)

    fun hentMotpartBarnRelasjon(personident: Personident): MotpartBarnRelasjonDto {
        BidragPerson.SECURE_LOGGER.debug("Henter motpart-barn relasjon for person {}", personident.verdi)
        val person = hentPersonInfo(personident)
        val muligForelderBarnRelasjon = pdlConsumer.hentForelderBarnRelasjoner(personident)

        val mineForelderRoller = hentMinForelderRolle(muligForelderBarnRelasjon)
        val personensMotpartBarnRelasjon =
            mineForelderRoller.map { minForelderRolle ->
                val barnForMinForelderRolle = muligForelderBarnRelasjon.filter { it.minRolleForPerson === minForelderRolle }
                val alleFamilieenheter = lagAlleMotpartBarn(barnForMinForelderRolle, minForelderRolle, personident)
                grupperBarnMedMotpart(alleFamilieenheter)
            }.flatten()
        return MotpartBarnRelasjonDto(person, personensMotpartBarnRelasjon)
    }

    private fun grupperBarnMedMotpart(alleMotpartBarn: List<MotpartBarnRelasjon>): List<MotpartBarnRelasjon> {
        val gruppertMotpart = alleMotpartBarn.filter { it.motpart != null }.groupBy { it.motpart!! }
        val ukjentMotpart = alleMotpartBarn.filter { it.motpart == null }
        val personensFamilieenheter = java.util.ArrayList(ukjentMotpart)
        gruppertMotpart.forEach { (personMinimumInfo: PersonDto, motpartBarnRelasjoner: List<MotpartBarnRelasjon>) ->
            val alleBarnTilMotpart: List<PersonDto> = motpartBarnRelasjoner.map(MotpartBarnRelasjon::fellesBarn).flatten()
            alleMotpartBarn.firstOrNull { it.motpart != null && it.motpart!!.ident == personMinimumInfo.ident }
                ?.let {
                    personensFamilieenheter.add(MotpartBarnRelasjon(it.forelderrolleMotpart, personMinimumInfo, alleBarnTilMotpart))
                }
        }
        return personensFamilieenheter
    }

    private fun lagAlleMotpartBarn(
        muligForelderBarnRelasjon: List<ForelderBarnRelasjon>,
        minForelderRolle: Familierelasjon,
        minPersonident: Personident,
    ): List<MotpartBarnRelasjon> {
        val egneBarn = muligForelderBarnRelasjon.filter { it.erRelatertPersonsBarn() }
        return egneBarn.map { hentMotpartBarnRelasjon(it, minForelderRolle, minPersonident) }
    }

    private fun hentMotpartBarnRelasjon(
        barn: ForelderBarnRelasjon,
        minForelderRolle: Familierelasjon,
        minPersonident: Personident,
    ): MotpartBarnRelasjon {
        val barnRelasjon =
            if (barn.relatertPersonsIdent == null) {
                emptyList()
            } else {
                pdlConsumer.hentForelderBarnRelasjoner(barn.relatertPersonsIdent!!)
            }
        val muligMotpart =
            barnRelasjon.firstOrNull {
                it.relatertPersonsRolle !== minForelderRolle && it.relatertPersonsRolle != Familierelasjon.BARN
            } ?: barnRelasjon.filter { it.relatertPersonsIdent != minPersonident }.takeIf { it.size == 1 }?.firstOrNull()
        val forelderrolleMotpart = hentMotpartForelderRolle(muligMotpart, minForelderRolle)
        val muligMotpartRelatertPersonsIdent = muligMotpart?.relatertPersonsIdent
        val motpart = muligMotpartRelatertPersonsIdent?.let { hentPersonInfo(it) }
        val relatertPersonsIdent = barn.relatertPersonsIdent
        val fellesBarn = relatertPersonsIdent?.let { hentPersonInfo(it) }
        val barnInfo = listOfNotNull(fellesBarn)
        return MotpartBarnRelasjon(forelderrolleMotpart, motpart, barnInfo)
    }

    private fun hentMotpartForelderRolle(muligMotpart: ForelderBarnRelasjon?, minForelderRolle: Familierelasjon): Familierelasjon = muligMotpart?.relatertPersonsRolle
        ?: if (minForelderRolle === Familierelasjon.MOR) Familierelasjon.FAR else Familierelasjon.MOR

    private fun hentMinForelderRolle(forelderBarnRelasjon: List<ForelderBarnRelasjon>): Set<Familierelasjon> = forelderBarnRelasjon
        .filter { it.minRolleForPerson !== Familierelasjon.BARN }
        .map { it.minRolleForPerson }.toSet()

    fun hentPersondetaljer(ident: Personident): PersondetaljerDto {
        val personDetaljer = pdlConsumer.hentPersonDetaljer(ident)
        return PersondetaljerDto(
            person = personDetaljer.mapTilPersonDto(),
            adresse = personDetaljer.mapTilPersonAdresseDto(),
            kontonummer = hentKontonummer(ident),
            dødsbo = personDetaljer.mapTilDødsboDto(),
            språk = hentSpråk(ident),
            tidligereIdenter = personDetaljer.hentIdenter.hentAlleHistoriskeIdenter(),
        )
    }

    private fun hentSpråk(ident: Personident): String? = try {
        krrConsumer.hentPersonSpraak(ident)
    } catch (e: Exception) {
        BidragPerson.SECURE_LOGGER.error("Feil ved kall til KRR for ident: ${ident.verdi}! Feilmelding: ${e.message}")
        null
    }

    private fun hentKontonummer(ident: Personident): KontonummerDto? = kontoregisterConsumer.hentKontonummer(ident)

    fun hentePersonidenter(ident: String, inkludereHistoriske: Boolean, identgrupper: Set<Identgruppe>): List<PersonidentDto> {
        val hentIdenterResponse: HentIdenterResponse = pdlConsumer.hentePersonidenter(ident, identgrupper, inkludereHistoriske)
        return hentIdenterResponse.hentIdenter.identer.map {
            PersonidentDto(
                it.ident,
                it.historisk,
                Identgruppe.valueOf(it.gruppe),
            )
        }.toList()
    }

    private fun delerHusstand(bostedsadresseHusstandsmedlem: Bostedsadresse, bostedsadresse: Bostedsadresse): Boolean {
        if (bostedsadresseHusstandsmedlem.gyldigFraOgMed == null && bostedsadresseHusstandsmedlem.gyldigTilOgMed == null) {
            // Det finnes ingen datoer for husstandsmedlemmets boforhold. Regnes som husstandsmedlem.
            return true
        }

        if (bostedsadresse.gyldigFraOgMed == null && bostedsadresse.gyldigTilOgMed == null) {
            // Det finnes ingen datoer for BMs/BPs boforhold. Husstandsmedlem deler da husstand med BM/BP..
            return true
        }

        if (bostedsadresseHusstandsmedlem.gyldigTilOgMed == null && bostedsadresse.gyldigTilOgMed == null) {
            // Adresser uten gyldigTilOgMed er den adressen personen bor på nå.
            return true
        }

        if (bostedsadresseHusstandsmedlem.gyldigFraOgMed == null) {
            return if (bostedsadresse.gyldigFraOgMed != null) {
                bostedsadresseHusstandsmedlem.gyldigTilOgMed?.isAfter(bostedsadresse.gyldigFraOgMed) == true
            } else {
                false
            }
        }

        if (bostedsadresseHusstandsmedlem.gyldigTilOgMed == null) {
            return bostedsadresseHusstandsmedlem.gyldigFraOgMed.isBefore(bostedsadresse.gyldigTilOgMed)
        }

        if (bostedsadresse.gyldigTilOgMed == null) {
            return bostedsadresseHusstandsmedlem.gyldigTilOgMed.isAfter(bostedsadresse.gyldigFraOgMed)
        }

        if (bostedsadresse.gyldigFraOgMed != null &&
            bostedsadresseHusstandsmedlem.gyldigFraOgMed.isBefore(bostedsadresse.gyldigTilOgMed) &&
            bostedsadresseHusstandsmedlem.gyldigTilOgMed.isAfter(
                bostedsadresse.gyldigFraOgMed,
            )
        ) {
            return true
        }

        if (bostedsadresseHusstandsmedlem.gyldigFraOgMed.isAfter(bostedsadresse.gyldigTilOgMed)) {
            return false
        }

        return false
    }

    private fun hentHusstandsmedlemmer(bostedsadresse: Bostedsadresse): List<Husstandsmedlem> {
        val resultsPerPage = 50
        var pageNumber = 1
        val husstandsmedlemmerResponse = mutableListOf<Husstandsmedlem>()

        var husstandsmedlemmer = pdlConsumer.hentHusstandsmedlemmer(pageNumber, resultsPerPage, bostedsadresse)
        husstandsmedlemmerResponse.addAll(husstandsmedlemmer)

        while (husstandsmedlemmer.size >= resultsPerPage) {
            pageNumber++
            husstandsmedlemmer = pdlConsumer.hentHusstandsmedlemmer(pageNumber, resultsPerPage, bostedsadresse)
            husstandsmedlemmerResponse.addAll(husstandsmedlemmer)
        }

        return husstandsmedlemmerResponse
    }

    /**
     * Sorterer bostedsadresser kronologisk og justerer gyldigTilOgMed for hver adresse.
     * Sluttdato for hver adresseperiode settes til dagen før neste adresses startdato.
     * Logikken benyttes for adressehistorikk til både voksen, husstandsmedlem og barn.
     */
    private fun sorterOgJusterBostedsadresser(bostedsadresser: List<Bostedsadresse>): List<Bostedsadresse> {
        val sortertListe = bostedsadresser.sortedWith(
            compareBy { it.gyldigFraOgMed?.toLocalDate() ?: it.angittFlyttedato },
        )
        return sortertListe.mapIndexed { indeks, bostedsadresse ->
            // Siste forekomst, eller allerede satt sluttdato fra PDL, beholdes uendret
            if (bostedsadresse.gyldigTilOgMed != null || indeks == sortertListe.size - 1) {
                bostedsadresse
            } else {
                val nesteBostedsadresse = sortertListe[indeks + 1]
                bostedsadresse.copy(
                    gyldigTilOgMed =
                    nesteBostedsadresse.gyldigFraOgMed?.minusDays(1)
                        ?: nesteBostedsadresse.angittFlyttedato?.minusDays(1)?.atStartOfDay(),
                )
            }
        }
    }

    private fun finnHusstandsmedlemGyldigFraOgMedDato(husstandsmedlemBostedsadresse: Bostedsadresse, bostedsadresse: Bostedsadresse): LocalDateTime? = if (husstandsmedlemBostedsadresse.gyldigFraOgMed == null) {
        bostedsadresse.gyldigFraOgMed
    } else if (bostedsadresse.gyldigFraOgMed == null) {
        husstandsmedlemBostedsadresse.gyldigFraOgMed
    } else if (husstandsmedlemBostedsadresse.gyldigFraOgMed.isBefore(bostedsadresse.gyldigFraOgMed)) {
        bostedsadresse.gyldigFraOgMed
    } else {
        husstandsmedlemBostedsadresse.gyldigFraOgMed
    }

    private fun finnHusstandsmedlemGyldigTilOgMedDato(
        dødsdato: LocalDate?,
        husstandsmedlemBostedsadresse: Bostedsadresse,
        bostedsadresse: Bostedsadresse,
    ): LocalDateTime? {
        val gyldigTomDAto: LocalDateTime? = if (husstandsmedlemBostedsadresse.gyldigTilOgMed == null) {
            bostedsadresse.gyldigTilOgMed
        } else if (bostedsadresse.gyldigTilOgMed == null) {
            husstandsmedlemBostedsadresse.gyldigTilOgMed
        } else if (husstandsmedlemBostedsadresse.gyldigTilOgMed.isAfter(bostedsadresse.gyldigTilOgMed)) {
            bostedsadresse.gyldigTilOgMed
        } else {
            husstandsmedlemBostedsadresse.gyldigTilOgMed
        }
        return if (dødsdato == null) {
            gyldigTomDAto
        } else {
            if (gyldigTomDAto == null || gyldigTomDAto.toLocalDate().isAfter(dødsdato)) {
                dødsdato.atStartOfDay()
            } else {
                gyldigTomDAto
            }
        }
    }
}
