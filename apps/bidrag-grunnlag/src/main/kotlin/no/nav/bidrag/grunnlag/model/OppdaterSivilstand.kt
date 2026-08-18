package no.nav.bidrag.grunnlag.model

import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestStatus
import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.grunnlag.SECURE_LOGGER
import no.nav.bidrag.grunnlag.bo.SivilstandBo
import no.nav.bidrag.grunnlag.consumer.bidragperson.BidragPersonConsumer
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.service.PersistenceService
import no.nav.bidrag.grunnlag.service.PersonIdOgPeriodeRequest
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.tilJson
import no.nav.bidrag.transport.behandling.grunnlag.response.OppdaterGrunnlagDto
import no.nav.bidrag.transport.person.SivilstandPdlDto
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.time.LocalDateTime

class OppdaterSivilstand(
    private val grunnlagspakkeId: Int,
    private val timestampOppdatering: LocalDateTime,
    private val persistenceService: PersistenceService,
    private val bidragPersonConsumer: BidragPersonConsumer,

) : MutableList<OppdaterGrunnlagDto> by mutableListOf() {

    fun oppdaterSivilstand(
        sivilstandRequestListe: List<PersonIdOgPeriodeRequest>,
        historiskeIdenterMap: Map<String, List<String>>,
    ): OppdaterSivilstand {
        sivilstandRequestListe.forEach { personIdOgPeriode ->

            var antallPerioderFunnet = 0

            SECURE_LOGGER.info("Kaller bidrag-person og henter sivilstand for: ${tilJson(personIdOgPeriode.personId)}")

            try {
                when (
                    val restResponseSivilstand =
                        bidragPersonConsumer.hentSivilstand(Personident(personIdOgPeriode.personId))
                ) {
                    is RestResponse.Success -> {
                        val sivilstandResponse = restResponseSivilstand.body
                        SECURE_LOGGER.info("Kall til bidrag-person for å hente sivilstand ga følgende respons: ${tilJson(sivilstandResponse)}")

                        if (sivilstandResponse.sivilstandPdlDto.isNotEmpty()) {
                            persistenceService.oppdaterEksisterendeSivilstandTilInaktiv(
                                grunnlagspakkeId,
                                historiskeIdenterMap[personIdOgPeriode.personId] ?: listOf(personIdOgPeriode.personId),
                                timestampOppdatering,
                            )
                            // Sorterer motta sivilstandforekomster etter. Forekomstene som er historiske skal komme først. Deretter sorteres det på
                            // gyldigFraOgMed, bekreftelsesdato, registrert og til slutt på type.
                            antallPerioderFunnet = behandleSivilstandResponse(
                                sivilstandResponse.sivilstandPdlDto.sortedWith(
                                    compareByDescending<SivilstandPdlDto> { it.historisk }.thenBy { it.gyldigFom }
                                        .thenBy { it.bekreftelsesdato }.thenBy { it.registrert }.thenBy { it.type.toString() },
                                ),
                                personIdOgPeriode,
                            )
                        }
                        this.add(
                            OppdaterGrunnlagDto(
                                GrunnlagRequestType.SIVILSTAND,
                                personIdOgPeriode.personId,
                                GrunnlagRequestStatus.HENTET,
                                "Antall sivilstandsforekomster funnet: $antallPerioderFunnet",
                            ),
                        )
                    }

                    is RestResponse.Failure -> this.add(
                        OppdaterGrunnlagDto(
                            GrunnlagRequestType.SIVILSTAND,
                            personIdOgPeriode.personId,
                            if (restResponseSivilstand.statusCode == HttpStatus.NOT_FOUND) {
                                GrunnlagRequestStatus.IKKE_FUNNET
                            } else {
                                GrunnlagRequestStatus.FEILET
                            },
                            "Feil ved henting av sivilstand fra bidrag-person/PDL for perioden: ${personIdOgPeriode.periodeFra} - " +
                                "${personIdOgPeriode.periodeTil}.",
                        ),
                    )
                }
            } catch (e: Exception) {
                this.add(
                    OppdaterGrunnlagDto(
                        type = GrunnlagRequestType.SIVILSTAND,
                        personId = personIdOgPeriode.personId,
                        status = GrunnlagRequestStatus.FEILET,
                        statusMelding = "Feil ved henting av sivilstand for ${personIdOgPeriode.personId}. Exception: ${e.message}",
                    ),
                )
            }
        }

        return this
    }

    private fun behandleSivilstandResponse(sivilstandDtoListe: List<SivilstandPdlDto>, personIdOgPeriodeRequest: PersonIdOgPeriodeRequest): Int {
        var antallPerioderFunnet = 0

        var periodeTil: LocalDate?

        for (indeks in sivilstandDtoListe.indices) {
            // Setter periodeTil lik periodeFra for neste forekomst.
            // Hvis det ikke finnes en neste forekomst så settes periodeTil lik null. Timestamp registrert brukes bare hvis neste forekomst ikke er historisk
            periodeTil = if (sivilstandDtoListe.getOrNull(indeks + 1)?.historisk == true) {
                sivilstandDtoListe.getOrNull(indeks + 1)?.gyldigFom
                    ?: sivilstandDtoListe.getOrNull(indeks + 1)?.bekreftelsesdato
            } else {
                sivilstandDtoListe.getOrNull(indeks + 1)?.gyldigFom
                    ?: sivilstandDtoListe.getOrNull(indeks + 1)?.bekreftelsesdato
                    ?: sivilstandDtoListe.getOrNull(indeks + 1)?.registrert?.toLocalDate()
            }

            antallPerioderFunnet++
            lagreSivilstand(
                sivilstandDtoListe[indeks],
                grunnlagspakkeId,
                timestampOppdatering,
                personIdOgPeriodeRequest.personId,
                periodeTil,
            )
        }

        return antallPerioderFunnet
    }

    private fun lagreSivilstand(
        sivilstand: SivilstandPdlDto,
        grunnlagspakkeId: Int,
        timestampOppdatering: LocalDateTime,
        personId: String,
        periodeTil: LocalDate?,
    ) {
        // Hvis en forekomst er merket som historisk, altså ikke lenger aktiv, så skal periodeFra settes lik gyldgiFraOgMed evt bekreftelsesdato
        // Hvis begge er null så settes periodeFra lik null
        // Hvis forekomsten er aktiv så kan registrert timestamp også brukes til å finne periodeFra. Tanken er da at dette er en ny forekomst og
        // at registrert kan bruukes til å anta riktig periodeFra.
        val periodeFra = if (sivilstand.historisk == true) {
            sivilstand.gyldigFom ?: sivilstand.bekreftelsesdato
        } else {
            sivilstand.gyldigFom ?: sivilstand.bekreftelsesdato ?: sivilstand.registrert?.toLocalDate()
        }

        persistenceService.opprettSivilstand(
            SivilstandBo(
                grunnlagspakkeId = grunnlagspakkeId,
                personId = personId,
                periodeFra = periodeFra,
                periodeTil = periodeTil,
                sivilstand = sivilstand.type.toString(),
                aktiv = true,
                brukFra = timestampOppdatering,
                brukTil = null,
                hentetTidspunkt = timestampOppdatering,
            ),
        )
    }
}
