package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.domene.enums.person.Familierelasjon
import no.nav.bidrag.domene.enums.person.SivilstandskodePDL
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.grunnlag.SECURE_LOGGER
import no.nav.bidrag.grunnlag.bo.PersonBo
import no.nav.bidrag.grunnlag.consumer.bidragperson.BidragPersonConsumer
import no.nav.bidrag.grunnlag.consumer.bidragperson.api.HusstandsmedlemmerRequest
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.evaluerFeilmelding
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.evaluerFeiltype
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.tilJson
import no.nav.bidrag.transport.behandling.grunnlag.response.BorISammeHusstandDto
import no.nav.bidrag.transport.behandling.grunnlag.response.FeilrapporteringDto
import no.nav.bidrag.transport.behandling.grunnlag.response.RelatertPersonGrunnlagDto
import no.nav.bidrag.transport.person.ForelderBarnRelasjon
import no.nav.bidrag.transport.person.ForelderBarnRelasjonDto
import no.nav.bidrag.transport.person.Husstandsmedlem
import no.nav.bidrag.transport.person.NavnFødselDødDto
import no.nav.bidrag.transport.person.PersonRequest
import java.time.LocalDate

class HentRelatertePersonerService(private val bidragPersonConsumer: BidragPersonConsumer) {

    fun hentRelatertePersoner(relatertPersonRequestListe: List<PersonIdOgPeriodeRequest>): HentGrunnlagGenericDto<RelatertPersonGrunnlagDto> {
        val relatertPersonListe = mutableListOf<RelatertPersonGrunnlagDto>()
        val relatertPersonInternListe = mutableListOf<RelatertPersonIntern>()
        val feilrapporteringListe = mutableListOf<FeilrapporteringDto>()

        relatertPersonRequestListe.forEach { personIdOgPeriode ->
            // Henter alle husstandsmedlemmer til BM/BP
            val husstandsmedlemmerListe = hentHusstandsmedlemmer(personIdOgPeriode = personIdOgPeriode, feilrapporteringListe = feilrapporteringListe)
            SECURE_LOGGER.debug("husstandsmedlemmerListe for {} {}", personIdOgPeriode.personId, husstandsmedlemmerListe)

            // Henter alle forelderbarnrelasjoner for BM/BP
            val forelderBarnRelasjoner =
                hentForelderBarnRelasjoner(
                    personident = Personident(personIdOgPeriode.personId),
                    feilrapporteringListe = feilrapporteringListe,
                ).forelderBarnRelasjon

            SECURE_LOGGER.debug("forelderBarnRelasjoner for {} {}: ", personIdOgPeriode, forelderBarnRelasjoner)

            // Henter personid for eventuelle ektefeller
            val ektefelleListe = hentEktefelleListe(personIdOgPeriode.personId)

            // Filtrerer ut alle barn av BM/BP
            val barnListe = finnBarn(forelderBarnRelasjoner, feilrapporteringListe = feilrapporteringListe)

            val motpartFellesBarnListe = mutableListOf<Personident>()

            barnListe.forEach { barn ->
                val relasjoner = hentForelderBarnRelasjoner(Personident(barn.personId!!), feilrapporteringListe).forelderBarnRelasjon
                relasjoner.forEach { relasjon ->
                    if (
                        listOf(Familierelasjon.MOR, Familierelasjon.FAR, Familierelasjon.MEDMOR)
                            .contains(relasjon.relatertPersonsRolle) &&
                        relasjon.relatertPersonsIdent != null &&
                        relasjon.relatertPersonsIdent?.verdi != personIdOgPeriode.personId

                    ) {
                        motpartFellesBarnListe.add(relasjon.relatertPersonsIdent!!)
                    }
                }
            }

            SECURE_LOGGER.debug("barnListe for {} {}", personIdOgPeriode.personId, barnListe)
            SECURE_LOGGER.debug("relatertPersonRequestListe: {}", relatertPersonRequestListe)

            // Slår sammen listene over husstandsmedlemmer og barn. Innsendt personId lagres ikke som eget husstandsmedlem.
            // Hvis personen ligger i barnListe settes erBarnAvBmBp lik true.
            (husstandsmedlemmerListe + barnListe)
                .filterNot { it.personId == personIdOgPeriode.personId }
                .forEach { person ->
                    val relasjonEktefelle = if (ektefelleListe.firstOrNull { it.verdi == person.personId } != null) {
                        Familierelasjon.EKTEFELLE
                    } else {
                        null
                    }

                    val relasjonMotpartFellesBarn = if (motpartFellesBarnListe.firstOrNull { it.verdi == person.personId } != null) {
                        Familierelasjon.MOTPART_TIL_FELLES_BARN
                    } else {
                        null
                    }

                    val forelderBarnRelasjon =
                        forelderBarnRelasjoner.firstOrNull { it.relatertPersonsIdent?.verdi == person.personId }?.relatertPersonsRolle

                    relatertPersonInternListe.add(
                        RelatertPersonIntern(
                            partPersonId = personIdOgPeriode.personId,
                            relatertPersonPersonId = person.personId,
                            gjelderPersonId = person.personId,
                            navn = person.navn,
                            fødselsdato = person.fodselsdato,
                            erBarnAvBmBp = barnListe.any { it.personId == person.personId },
                            relasjon = relasjonEktefelle ?: relasjonMotpartFellesBarn ?: forelderBarnRelasjon ?: Familierelasjon.INGEN,
                            husstandsmedlemPeriodeFra =
                            if (husstandsmedlemmerListe.any { it.personId == person.personId }) {
                                person.husstandsmedlemPeriodeFra
                            } else {
                                null
                            },
                            husstandsmedlemPeriodeTil =
                            if (husstandsmedlemmerListe.any { it.personId == person.personId }) {
                                person.husstandsmedlemPeriodeTil
                            } else {
                                null
                            },
                        ),
                    )
                }

            // relatertPersonInternListe inneholder alle personer (partPersonId) grunnlaget husstandsmedlemmer er innhentet for.
            // For å unngå duplikate perioder i responsen må det sjekkes at periode gjelder for aktuell partPersonId.
            // En relatert person kan forekomme flere ganger i listen, én gang for hver periode personen har delt bolig med BM/BP (partPersonId).
            // I responsen skal hver person kun ligge én gang, med en liste over perioder personen har delt bolig med BM/BP (partPersonId).
            // Sjekker derfor om personen allerede ligger i responsen.
            SECURE_LOGGER.debug("relatertPersonInternListe for ${personIdOgPeriode.personId} ${tilJson(relatertPersonInternListe)}")

            relatertPersonInternListe
                .groupBy { it.partPersonId to it.relatertPersonPersonId }
                .values
                .forEach { relatertePersoner ->
                    val borISammeHusstandListe = relatertePersoner
                        .filter { it.husstandsmedlemPeriodeFra != null || it.husstandsmedlemPeriodeTil != null }
                        .map { BorISammeHusstandDto(periodeFra = it.husstandsmedlemPeriodeFra, periodeTil = it.husstandsmedlemPeriodeTil) }

                    val firstPerson = relatertePersoner.first()
                    relatertPersonListe.add(
                        RelatertPersonGrunnlagDto(
                            partPersonId = firstPerson.partPersonId,
                            relatertPersonPersonId = firstPerson.relatertPersonPersonId,
                            gjelderPersonId = firstPerson.relatertPersonPersonId,
                            navn = firstPerson.navn,
                            fødselsdato = firstPerson.fødselsdato,
                            erBarnAvBmBp = firstPerson.erBarnAvBmBp,
                            relasjon = firstPerson.relasjon,
                            borISammeHusstandDtoListe = borISammeHusstandListe,
                        ),
                    )
                }
            SECURE_LOGGER.debug("relatertPersonListe for ${personIdOgPeriode.personId}: ${tilJson(relatertPersonListe)}")
        }

        return HentGrunnlagGenericDto(grunnlagListe = relatertPersonListe, feilrapporteringListe = feilrapporteringListe)
    }

    // Henter alle husstandsmedlemmer til BM/BP
    private fun hentHusstandsmedlemmer(
        personIdOgPeriode: PersonIdOgPeriodeRequest,
        feilrapporteringListe: MutableList<FeilrapporteringDto>,
    ): List<PersonBo> {
        val husstandsmedlemListe = mutableListOf<PersonBo>()

        when (
            val restResponseHusstandsmedlemmer = bidragPersonConsumer.hentHusstandsmedlemmer(
                HusstandsmedlemmerRequest(
                    PersonRequest(Personident(personIdOgPeriode.personId)),
                    personIdOgPeriode.periodeFra,
                ),
            )
        ) {
            is RestResponse.Success -> {
                val husstandsmedlemmerResponseDto = restResponseHusstandsmedlemmer.body

                husstandsmedlemmerResponseDto.husstandListe.forEach { husstand ->
                    husstand.husstandsmedlemListe.forEach { husstandsmedlem ->
                        SECURE_LOGGER.debug(
                            "husstandsmedlemInnenforPeriode: {} {} {}",
                            personIdOgPeriode.personId,
                            husstandsmedlem.personId,
                            husstandsmedlemInnenforPeriode(personIdOgPeriode, husstandsmedlem),
                        )
                        if (husstandsmedlem.personId.toString() != personIdOgPeriode.personId &&
                            husstandsmedlemInnenforPeriode(personIdOgPeriode, husstandsmedlem)
                        ) {
                            husstandsmedlemListe.add(
                                PersonBo(
                                    personId = husstandsmedlem.personId.verdi,
                                    navn = husstandsmedlem.navn,
                                    fodselsdato = husstandsmedlem.fødselsdato,
                                    husstandsmedlemPeriodeFra = husstandsmedlem.gyldigFraOgMed,
                                    husstandsmedlemPeriodeTil = husstandsmedlem.gyldigTilOgMed,
                                ),
                            )
                        }
                    }
                }
            }

            is RestResponse.Failure -> {
                feilrapporteringListe.add(
                    FeilrapporteringDto(
                        grunnlagstype = GrunnlagRequestType.HUSSTANDSMEDLEMMER_OG_EGNE_BARN,
                        personId = personIdOgPeriode.personId,
                        periodeFra = null,
                        periodeTil = null,
                        feiltype = evaluerFeiltype(
                            melding = restResponseHusstandsmedlemmer.message,
                            httpStatuskode = restResponseHusstandsmedlemmer.statusCode,
                        ),
                        feilmelding = evaluerFeilmelding(
                            melding = restResponseHusstandsmedlemmer.message,
                            grunnlagstype = GrunnlagRequestType.HUSSTANDSMEDLEMMER_OG_EGNE_BARN,
                        ),
                    ),
                )
                return emptyList()
            }
        }
        return slåSammenSammenhengendePerioder(husstandsmedlemListe.sortedWith(compareBy({ it.personId }, { it.husstandsmedlemPeriodeFra })))
    }

    // Henter alle forelderbarnrelasjoner
    private fun hentForelderBarnRelasjoner(
        personident: Personident,
        feilrapporteringListe: MutableList<FeilrapporteringDto>,
    ): ForelderBarnRelasjonDto {
        // Henter en liste over forelderbarnrelasjoner
        when (
            val restResponseForelderBarnRelasjon = bidragPersonConsumer.hentForelderBarnRelasjon(personident)
        ) {
            is RestResponse.Success -> {
                val forelderBarnRelasjonResponse = restResponseForelderBarnRelasjon.body
                return forelderBarnRelasjonResponse
            }

            is RestResponse.Failure -> {
                feilrapporteringListe.add(
                    FeilrapporteringDto(
                        grunnlagstype = GrunnlagRequestType.HUSSTANDSMEDLEMMER_OG_EGNE_BARN,
                        personId = personident.verdi,
                        periodeFra = null,
                        periodeTil = null,
                        feiltype = evaluerFeiltype(
                            melding = restResponseForelderBarnRelasjon.message,
                            httpStatuskode = restResponseForelderBarnRelasjon.statusCode,
                        ),
                        feilmelding = evaluerFeilmelding(
                            melding = restResponseForelderBarnRelasjon.message,
                            grunnlagstype = GrunnlagRequestType.HUSSTANDSMEDLEMMER_OG_EGNE_BARN,
                        ),
                    ),
                )
                return ForelderBarnRelasjonDto(emptyList())
            }
        }
    }

    // Henter alle forelderbarnrelasjoner
    private fun finnBarn(
        forelderBarnRelasjonListe: List<ForelderBarnRelasjon>,
        feilrapporteringListe: MutableList<FeilrapporteringDto>,
    ): List<PersonBo> {
        val barnListe = mutableListOf<PersonBo>()

        forelderBarnRelasjonListe.forEach {
            // Kaller bidrag-person for å hente info om fødselsdato og navn
            if (it.relatertPersonsRolle == Familierelasjon.BARN && it.relatertPersonsIdent != null) {
                val navnFødselDødResponseDto = hentNavnFødselDød(
                    personident = Personident(it.relatertPersonsIdent!!.verdi),
                    feilrapporteringListe = feilrapporteringListe,
                )
                // Lager en liste over fnr for alle barn som er funnet
                barnListe.add(
                    PersonBo(
                        personId = it.relatertPersonsIdent?.verdi,
                        navn = navnFødselDødResponseDto?.navn,
                        fodselsdato = navnFødselDødResponseDto?.fødselsdato,
                        husstandsmedlemPeriodeFra = null,
                        husstandsmedlemPeriodeTil = null,
                    ),
                )
            }
        }
        return barnListe
    }

    // Henter navn, fødselsdato og eventuell dødsdato for personer fra bidrag-person
    private fun hentNavnFødselDød(personident: Personident, feilrapporteringListe: MutableList<FeilrapporteringDto>): NavnFødselDødDto? {
        val navnFødselDødDto: NavnFødselDødDto

        when (
            val restResponseFoedselOgDoed = bidragPersonConsumer.hentNavnFødselOgDød(personident)
        ) {
            is RestResponse.Success -> {
                val foedselOgDoedResponse = restResponseFoedselOgDoed.body

                navnFødselDødDto = NavnFødselDødDto(
                    foedselOgDoedResponse.navn,
                    foedselOgDoedResponse.fødselsdato,
                    foedselOgDoedResponse.fødselsår,
                    foedselOgDoedResponse.dødsdato,
                )
            }

            is RestResponse.Failure -> {
                feilrapporteringListe.add(
                    FeilrapporteringDto(
                        grunnlagstype = GrunnlagRequestType.HUSSTANDSMEDLEMMER_OG_EGNE_BARN,
                        personId = personident.verdi,
                        periodeFra = null,
                        periodeTil = null,
                        feiltype = evaluerFeiltype(
                            melding = restResponseFoedselOgDoed.message,
                            httpStatuskode = restResponseFoedselOgDoed.statusCode,
                        ),
                        feilmelding = evaluerFeilmelding(
                            melding = restResponseFoedselOgDoed.message,
                            grunnlagstype = GrunnlagRequestType.HUSSTANDSMEDLEMMER_OG_EGNE_BARN,
                        ),
                    ),
                )
                return null
            }
        }
        return navnFødselDødDto
    }

    // Metode for å slå sammen sammenhengende perioder en person har vært husstandsmedlem. Responsen fra bidrag-person er gruppert per husstand.
    // Det kan derfor være flere perioder for samme person. Metoden slår sammen perioder som er sammenhengende.
    private fun slåSammenSammenhengendePerioder(husstandsmedlemListe: List<PersonBo>): List<PersonBo> {
        if (husstandsmedlemListe.size < 2) {
            return husstandsmedlemListe
        }

        val resultatListe = mutableListOf<PersonBo>()
        var gjeldendeForekomst = husstandsmedlemListe.first()

        (1 until husstandsmedlemListe.size).forEach { i ->
            val nesteForekomst = husstandsmedlemListe[i]

            gjeldendeForekomst =
                if (gjeldendeForekomst.personId == nesteForekomst.personId &&
                    gjeldendeForekomst.husstandsmedlemPeriodeTil == nesteForekomst.husstandsmedlemPeriodeFra
                ) {
                    // Slår sammen periodene
                    gjeldendeForekomst.copy(husstandsmedlemPeriodeTil = nesteForekomst.husstandsmedlemPeriodeTil)
                } else {
                    // Det finnes ikke flere sammenhengende forekomster for person i listen, legg til gjeldende forekomst i resultatlisten
                    resultatListe.add(gjeldendeForekomst)
                    nesteForekomst
                }
        }

        // Legg til det siste objektet i resultatlisten
        resultatListe.add(gjeldendeForekomst)

        return resultatListe
    }

    private fun husstandsmedlemInnenforPeriode(personIdOgPeriode: PersonIdOgPeriodeRequest, husstandsmedlem: Husstandsmedlem): Boolean {
        if (husstandsmedlem.gyldigFraOgMed == null) {
            return husstandsmedlem.gyldigTilOgMed == null ||
                husstandsmedlem.gyldigTilOgMed!!.isAfter(
                    personIdOgPeriode.periodeFra,
                )
        }

        if (husstandsmedlem.gyldigTilOgMed == null) {
            return husstandsmedlem.gyldigFraOgMed!!.isBefore(personIdOgPeriode.periodeTil)
        }

        if (husstandsmedlem.gyldigFraOgMed!!.isAfter(personIdOgPeriode.periodeTil.minusDays(1))) {
            return false
        }

        if (husstandsmedlem.gyldigTilOgMed!!.isAfter(personIdOgPeriode.periodeFra)) {
            return true
        }
        return false
    }

    private fun hentEktefelleListe(personId: String): List<Personident> {
        val ektefelleListe = mutableListOf<Personident>()
        when (
            val restResponseSivilstand = bidragPersonConsumer.hentSivilstand(Personident(personId))
        ) {
            is RestResponse.Success -> {
                val sivilstandRespons = restResponseSivilstand.body
                sivilstandRespons.sivilstandPdlDto.forEach {
                    if (it.relatertVedSivilstand != null && it.type.toString() == SivilstandskodePDL.GIFT.toString()) {
                        ektefelleListe.add(Personident(it.relatertVedSivilstand!!))
                    }
                }
            }

            is RestResponse.Failure -> {
                return emptyList()
            }
        }
        return ektefelleListe
    }

    // Intern dataklasse brukt for å simulere funksjonalitet fra oppdater- og hent-grunnlagspakke-tjenestene
    data class RelatertPersonIntern(
        val partPersonId: String?,
        val relatertPersonPersonId: String?,
        val gjelderPersonId: String? = null,
        val navn: String?,
        val fødselsdato: LocalDate?,
        val erBarnAvBmBp: Boolean,
        val relasjon: Familierelasjon = Familierelasjon.INGEN,
        val husstandsmedlemPeriodeFra: LocalDate?,
        val husstandsmedlemPeriodeTil: LocalDate?,
    )
}
