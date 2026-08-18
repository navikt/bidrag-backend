package no.nav.bidrag.grunnlag.model

import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestStatus
import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.domene.enums.person.Familierelasjon
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.grunnlag.SECURE_LOGGER
import no.nav.bidrag.grunnlag.bo.PersonBo
import no.nav.bidrag.grunnlag.bo.RelatertPersonBo
import no.nav.bidrag.grunnlag.consumer.bidragperson.BidragPersonConsumer
import no.nav.bidrag.grunnlag.consumer.bidragperson.api.HusstandsmedlemmerRequest
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.service.PersistenceService
import no.nav.bidrag.grunnlag.service.PersonIdOgPeriodeRequest
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.tilJson
import no.nav.bidrag.transport.behandling.grunnlag.response.OppdaterGrunnlagDto
import no.nav.bidrag.transport.person.NavnFødselDødDto
import no.nav.bidrag.transport.person.PersonRequest
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.time.LocalDateTime

class OppdaterRelatertePersoner(
    private val grunnlagspakkeId: Int,
    private val timestampOppdatering: LocalDateTime,
    private val persistenceService: PersistenceService,
    private val bidragPersonConsumer: BidragPersonConsumer,

) : MutableList<OppdaterGrunnlagDto> by mutableListOf() {

    // Henter og lagrer først husstandsmedlemmer for så å hente forelder-barn-relasjoner.
    // Også barn som ikke bor i samme husstand som BM/BP skal være med i grunnlaget og lagres med null i husstandsmedlemPeriodeFra
    // og husstandsmedlemPeriodeTil.
    fun oppdaterRelatertePersoner(
        relatertePersonerRequestListe: List<PersonIdOgPeriodeRequest>,
        historiskeIdenterMap: Map<String, List<String>>,
    ): OppdaterRelatertePersoner {
        relatertePersonerRequestListe.forEach { personIdOgPeriode ->

            // Sett eksisterende forekomster av RelatertPerson til inaktiv
            persistenceService.oppdaterEksisterendeRelatertPersonTilInaktiv(
                grunnlagspakkeId = grunnlagspakkeId,
                personIdListe = historiskeIdenterMap[personIdOgPeriode.personId] ?: listOf(personIdOgPeriode.personId),
                timestampOppdatering = timestampOppdatering,
            )

            // henter alle husstandsmedlemmer til BM/BP
            val husstandsmedlemmerListe = hentHusstandsmedlemmer(personIdOgPeriode.personId, personIdOgPeriode.periodeFra)

            // henter alle barn av BM/BP
            val barnListe = hentBarn(Personident(personIdOgPeriode.personId))

            // Alle husstandsmedlemmer innenfor aktuell periode lagres i tabell relatert_person.
            // Det sjekkes om husstandsmedlem finnes i liste over barn.
            // erBarnAvmBp settes lik true i så fall. Tester slik at person ikke lagres som eget husstandsmedlem.
            husstandsmedlemmerListe.forEach { husstandsmedlem ->
                if (husstandsmedlem.personId != personIdOgPeriode.personId && husstandsmedlemInnenforPeriode(personIdOgPeriode, husstandsmedlem)) {
                    persistenceService.opprettRelatertPerson(
                        RelatertPersonBo(
                            grunnlagspakkeId = grunnlagspakkeId,
                            partPersonId = personIdOgPeriode.personId,
                            relatertPersonPersonId = husstandsmedlem.personId,
                            navn = husstandsmedlem.navn,
                            fodselsdato = husstandsmedlem.fodselsdato,
                            erBarnAvBmBp = barnListe.any { it.personId == husstandsmedlem.personId },
                            husstandsmedlemPeriodeFra = husstandsmedlem.husstandsmedlemPeriodeFra,
                            husstandsmedlemPeriodeTil = husstandsmedlem.husstandsmedlemPeriodeTil,
                            aktiv = true,
                            brukFra = timestampOppdatering,
                            brukTil = null,
                            hentetTidspunkt = timestampOppdatering,
                        ),
                    )
                }
            }

            // Filtrer listen over BM/BPs barn slik at barn som ligger i listen over husstandsmedlemmer,
            // og som derfor allerede er lagret, fjernes og lagrer deretter de gjenværende i tabell relatert_person.
            val filtrertBarnListe = barnListe.filter { barn -> husstandsmedlemmerListe.any { it.personId != barn.personId } }

            filtrertBarnListe.forEach { barn ->
                persistenceService.opprettRelatertPerson(
                    RelatertPersonBo(
                        grunnlagspakkeId = grunnlagspakkeId,
                        partPersonId = personIdOgPeriode.personId,
                        relatertPersonPersonId = barn.personId,
                        navn = barn.navn,
                        fodselsdato = barn.fodselsdato,
                        erBarnAvBmBp = barnListe.any { it.personId == barn.personId },
                        husstandsmedlemPeriodeFra = null,
                        husstandsmedlemPeriodeTil = null,
                        aktiv = true,
                        brukFra = timestampOppdatering,
                        brukTil = null,
                        hentetTidspunkt = timestampOppdatering,
                    ),
                )
            }
        }
        return this
    }

    private fun husstandsmedlemInnenforPeriode(personIdOgPeriode: PersonIdOgPeriodeRequest, husstandsmedlem: PersonBo): Boolean {
        if (husstandsmedlem.husstandsmedlemPeriodeFra == null) {
            return husstandsmedlem.husstandsmedlemPeriodeTil == null ||
                husstandsmedlem.husstandsmedlemPeriodeTil.isAfter(
                    personIdOgPeriode.periodeFra,
                )
        }

        if (husstandsmedlem.husstandsmedlemPeriodeTil == null) {
            return husstandsmedlem.husstandsmedlemPeriodeFra.isBefore(personIdOgPeriode.periodeTil)
        }

        if (husstandsmedlem.husstandsmedlemPeriodeFra.isAfter(personIdOgPeriode.periodeTil.minusDays(1))) {
            return false
        }

        if (husstandsmedlem.husstandsmedlemPeriodeTil.isAfter(personIdOgPeriode.periodeFra)) {
            return true
        }
        return false
    }

    private fun hentHusstandsmedlemmer(husstandsmedlemmerRequest: String, periodeFra: LocalDate?): List<PersonBo> {
        SECURE_LOGGER.info("Kaller bidrag-person Husstandsmedlemmer med request: ${tilJson(husstandsmedlemmerRequest)}")

        val husstandsmedlemListe = mutableListOf<PersonBo>()

        try {
            when (
                val restResponseHusstandsmedlemmer =
                    bidragPersonConsumer.hentHusstandsmedlemmer(
                        HusstandsmedlemmerRequest(
                            PersonRequest(Personident(husstandsmedlemmerRequest)),
                            periodeFra,
                        ),
                    )
            ) {
                is RestResponse.Success -> {
                    val husstandsmedlemmerResponseDto = restResponseHusstandsmedlemmer.body
                    SECURE_LOGGER.info(
                        "Bidrag-person ga følgende respons på Husstandsmedlemmer for grunnlag EgneBarnIHusstanden: ${
                            tilJson(
                                husstandsmedlemmerResponseDto,
                            )
                        }",
                    )

                    if (husstandsmedlemmerResponseDto.husstandListe.isNotEmpty()) {
                        husstandsmedlemmerResponseDto.husstandListe.forEach { husstand ->
                            husstand.husstandsmedlemListe.forEach { husstandsmedlem ->
                                husstandsmedlemListe.add(
                                    PersonBo(
                                        husstandsmedlem.personId.verdi,
                                        husstandsmedlem.navn,
                                        husstandsmedlem.fødselsdato,
                                        husstandsmedlem.gyldigFraOgMed,
                                        husstandsmedlem.gyldigTilOgMed,
                                    ),
                                )
                            }
                        }
                    }
                    this.add(
                        OppdaterGrunnlagDto(
                            GrunnlagRequestType.HUSSTANDSMEDLEMMER_OG_EGNE_BARN,
                            husstandsmedlemmerRequest,
                            GrunnlagRequestStatus.HENTET,
                            "Antall husstandsmedlemmer funnet: ${husstandsmedlemListe.size}",
                        ),
                    )
                    return slåSammenSammenhengendePerioder(
                        husstandsmedlemListe.sortedWith(
                            compareBy(
                                { it.personId },
                                { it.husstandsmedlemPeriodeFra },
                            ),
                        ),
                    )
                }

                is RestResponse.Failure -> this.add(
                    OppdaterGrunnlagDto(
                        GrunnlagRequestType.HUSSTANDSMEDLEMMER_OG_EGNE_BARN,
                        husstandsmedlemmerRequest,
                        if (restResponseHusstandsmedlemmer.statusCode == HttpStatus.NOT_FOUND) {
                            GrunnlagRequestStatus.IKKE_FUNNET
                        } else {
                            GrunnlagRequestStatus.FEILET
                        },
                        "Feil ved henting av husstandsmedlemmer og egne barn for: $husstandsmedlemmerRequest.",
                    ),
                )
            }
        } catch (e: Exception) {
            this.add(
                OppdaterGrunnlagDto(
                    type = GrunnlagRequestType.BARNETILLEGG,
                    personId = husstandsmedlemmerRequest,
                    status = GrunnlagRequestStatus.FEILET,
                    statusMelding = "Feil ved henting av husstandsmedlemmer og egne barn for: $husstandsmedlemmerRequest.. Exception: ${e.message}",
                ),
            )
        }
        return emptyList()
    }

    private fun hentBarn(forelderBarnRequest: Personident): List<PersonBo> {
        SECURE_LOGGER.info("Kaller bidrag-person Forelder-barn-relasjon med request: ${tilJson(forelderBarnRequest)}")

        val barnListe = mutableListOf<PersonBo>()

        // Henter en liste over BMs/BPs barn og henter så info om fødselsdag og navn for disse
        try {
            when (
                val restResponseForelderBarnRelasjon =
                    bidragPersonConsumer.hentForelderBarnRelasjon(forelderBarnRequest)
            ) {
                is RestResponse.Success -> {
                    val forelderBarnRelasjonResponse = restResponseForelderBarnRelasjon.body

                    if (forelderBarnRelasjonResponse.forelderBarnRelasjon.isNotEmpty()) {
                        SECURE_LOGGER.info("Bidrag-person ga følgende respons på forelder-barn-relasjoner: ${tilJson(forelderBarnRelasjonResponse)}")

                        forelderBarnRelasjonResponse.forelderBarnRelasjon.forEach { forelderBarnRelasjon ->
                            // Kaller bidrag-person for å hente info om fødselsdato og navn
                            if (forelderBarnRelasjon.relatertPersonsRolle == Familierelasjon.BARN &&
                                forelderBarnRelasjon.relatertPersonsIdent != null
                            ) {
                                val navnFoedselDoedResponseDto = hentNavnFoedselDoed(Personident(forelderBarnRelasjon.relatertPersonsIdent!!.verdi))
                                // Lager en liste over fnr for alle barn som er funnet
                                barnListe.add(
                                    PersonBo(
                                        forelderBarnRelasjon.relatertPersonsIdent?.verdi,
                                        navnFoedselDoedResponseDto?.navn,
                                        navnFoedselDoedResponseDto?.fødselsdato,
                                        null,
                                        null,
                                    ),
                                )
                            }
                        }
                        return barnListe
                    }
                }

                is RestResponse.Failure -> this.add(
                    OppdaterGrunnlagDto(
                        GrunnlagRequestType.HUSSTANDSMEDLEMMER_OG_EGNE_BARN,
                        forelderBarnRequest.verdi,
                        if (restResponseForelderBarnRelasjon.statusCode == HttpStatus.NOT_FOUND) {
                            GrunnlagRequestStatus.IKKE_FUNNET
                        } else {
                            GrunnlagRequestStatus.FEILET
                        },
                        "Feil ved henting av egne barn for: ${forelderBarnRequest.verdi} .",
                    ),
                )
            }
        } catch (e: Exception) {
            this.add(
                OppdaterGrunnlagDto(
                    type = GrunnlagRequestType.BARNETILLEGG,
                    personId = forelderBarnRequest.verdi,
                    status = GrunnlagRequestStatus.FEILET,
                    statusMelding = "Feil ved henting av egne barn for: ${forelderBarnRequest.verdi}. Exception: ${e.message}",
                ),
            )
        }
        return emptyList()
    }

    private fun hentNavnFoedselDoed(personident: Personident): NavnFødselDødDto? {
        // hent navn, fødselsdato og eventuell dødsdato for personer fra bidrag-person
        SECURE_LOGGER.info("Kaller bidrag-person hent navn og fødselsdato for : $personident")
        try {
            when (
                val restResponseFoedselOgDoed =
                    bidragPersonConsumer.hentNavnFødselOgDød(personident)
            ) {
                is RestResponse.Success -> {
                    val foedselOgDoedResponse = restResponseFoedselOgDoed.body
                    SECURE_LOGGER.info("Bidrag-person ga følgende respons på hent navn og fødselsdato: ${tilJson(foedselOgDoedResponse)}")

                    return NavnFødselDødDto(
                        foedselOgDoedResponse.navn,
                        foedselOgDoedResponse.fødselsdato,
                        foedselOgDoedResponse.fødselsår,
                        foedselOgDoedResponse.dødsdato,
                    )
                }

                is RestResponse.Failure ->
                    return null
            }
        } catch (e: Exception) {
            this.add(
                OppdaterGrunnlagDto(
                    type = GrunnlagRequestType.BARNETILLEGG,
                    personId = personident.verdi,
                    status = GrunnlagRequestStatus.FEILET,
                    statusMelding = "Feil ved henting av hent navn og fødselsdato for: ${personident.verdi}. Exception: ${e.message}",
                ),
            )
        }
        return null
    }

    // Metode for å slå sammen sammenhengende perioder en person har vært husstandsmedlem. Responsen fra bidrag-person er gruppert per husstand,
    // det kan derfor være flere perioder for samme person. Metoden slår sammen perioder som er sammenhengende.
    private fun slåSammenSammenhengendePerioder(husstandsmedlemListe: List<PersonBo>): List<PersonBo> {
        if (husstandsmedlemListe.size < 2) {
            return husstandsmedlemListe
        }

        val resultatListe = mutableListOf<PersonBo>()
        var gjeldendeForekomst = husstandsmedlemListe.first()

        for (i in 1 until husstandsmedlemListe.size) {
            val nesteForekomst = husstandsmedlemListe[i]

            gjeldendeForekomst = if (gjeldendeForekomst.personId == nesteForekomst.personId &&
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
}
