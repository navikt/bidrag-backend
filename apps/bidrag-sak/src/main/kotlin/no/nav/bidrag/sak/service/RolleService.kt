package no.nav.bidrag.sak.service

import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.sak.UkjentPart
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.sak.domain.Bidragssak
import no.nav.bidrag.sak.domain.Rolle
import no.nav.bidrag.sak.integration.person.BidragPersonClient
import no.nav.bidrag.sak.integration.samhandler.BidragSamhandlerClient
import no.nav.bidrag.sak.mapper.BidragssakMapper.mapBarnTilRoller
import no.nav.bidrag.sak.mapper.medFødselsdato
import no.nav.bidrag.sak.mapper.model.RolleMedFødselsdato
import no.nav.bidrag.sak.mapper.model.fødselsnummer
import no.nav.bidrag.sak.mapper.model.harRM
import no.nav.bidrag.sak.mapper.model.rmFødselsnummer
import no.nav.bidrag.sak.mapper.model.rmSamhandlerId
import no.nav.bidrag.sak.mapper.model.tilFødselsdatoMap
import no.nav.bidrag.sak.mapper.model.type
import no.nav.bidrag.transport.sak.RolleDto
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class RolleService(
    private val bidragPersonClient: BidragPersonClient,
    private val samhandlerClient: BidragSamhandlerClient,
) {
    fun oppdaterRollerMedReelleMottager(
        lagredeRoller: Set<Rolle>,
        rollerMedReelleMottagere: List<RolleDto>,
    ): Set<Rolle> {
        if (rollerMedReelleMottagere.isEmpty()) {
            return lagredeRoller
        }

        val rolleTilReellMottagerRolleIdMap = assosierRollerMedIdForReellMottager(lagredeRoller, rollerMedReelleMottagere)
        return lagredeRoller
            .map { r ->
                rolleTilReellMottagerRolleIdMap[r]?.let { r.rmRolleId = it }
                r
            }.toSet()
    }

    private fun assosierRollerMedIdForReellMottager(
        lagredeRoller: Set<Rolle>,
        rollerMedReelleMottagere: List<RolleDto>,
    ): Map<Rolle, Int> = rollerMedReelleMottagere.associate { rolleDto ->
        val barn =
            lagredeRoller.firstOrNull {
                it.rolleType == Rolletype.BARN &&
                    it.fødselsnummer == rolleDto.fødselsnummer?.verdi
            } ?: error("Fant ikke BARN-rolle for ${rolleDto.fødselsnummer?.verdi}")

        val rmFnr = rolleDto.rmFødselsnummer()?.verdi
        val rmSam = rolleDto.rmSamhandlerId()?.verdi

        val rm =
            lagredeRoller.firstOrNull {
                it.rolleType == Rolletype.REELMOTTAKER &&
                    ((rmFnr != null && it.fødselsnummer == rmFnr) || (rmSam != null && it.samhandlerIdent == rmSam))
            } ?: error("Fant ikke REELMOTTAKER-rolle for rmFnr=$rmFnr rmSam=$rmSam")

        barn to rm.rolleId
    }

    fun oppdaterRoller(
        eksisterendeBidragssak: Bidragssak,
        requestRolleDtoer: Set<RolleDto>,
    ): Set<Rolle> {
        val lagredeRoller = eksisterendeBidragssak.roller
        val berikRequestRolleDtoer = berikRollerMedFødselsdato(requestRolleDtoer)

        val (dtoRollerTilOppdatering, nyeDtoRoller) =
            berikRequestRolleDtoer.partition {
                it.type in setOf(Rolletype.BIDRAGSMOTTAKER, Rolletype.BIDRAGSPLIKTIG) ||
                    lagredeRoller.any { eksisterende ->
                        it.fødselsnummer?.verdi == eksisterende.fødselsnummer && eksisterende.rolleType == Rolletype.BARN
                    }
            }

        val oppdaterteRoller = oppdaterEksiterendeRoller(dtoRollerTilOppdatering, eksisterendeBidragssak)
        val nyeRoller =
            nyeDtoRoller
                .map {
                    it.rolle
                }.mapBarnTilRoller(
                    berikRequestRolleDtoer.tilFødselsdatoMap(),
                    finnFørsteLedigeObjektnummer(lagredeRoller),
                    eksisterendeBidragssak,
                )

        return (oppdaterteRoller + nyeRoller).toSet()
    }

    fun validerRollerOgHentFødselsdatoer(roller: Set<RolleDto>): Map<Personident, LocalDate?> {
        val fødselsnumre = roller.mapNotNull { it.fødselsnummer } + roller.mapNotNull { it.rmFødselsnummer() }
        val fødselsdatoer = if (fødselsnumre.isEmpty()) mapOf() else bidragPersonClient.hentFødselsdatoer(fødselsnumre)

        require(fødselsdatoer.keys.containsAll(fødselsnumre)) {
            "Rolle forsøk opprettet for person som ikke finnes."
        }

        val samhandlere = roller.mapNotNull { it.rmSamhandlerId() }
        samhandlere.forEach {
            // Sjekk at samhandlere eksisterer (kaster feil hvis de ikke finnes).
            samhandlerClient.hentSamhandler(it)
        }

        return fødselsdatoer
    }

    fun berikRollerMedFødselsdato(roller: Set<RolleDto>): List<RolleMedFødselsdato> {
        val fødselsdatoer = validerRollerOgHentFødselsdatoer(roller)
        return roller.map { it.medFødselsdato(fødselsdatoer) }
    }

    private fun finnFørsteLedigeObjektnummer(eksisterendeRoller: Collection<Rolle>) = eksisterendeRoller.maxOf { it.objektnummer?.toInt() ?: 2 } + 1

    private fun oppdaterEksiterendeRoller(
        dtoRollerTilOppdatering: List<RolleMedFødselsdato>,
        eksisterendeBidragssak: Bidragssak,
    ): List<Rolle> {
        val barnDtoMap =
            dtoRollerTilOppdatering
                .filter { it.rolle.type == Rolletype.BARN && it.fødselsnummer != null }
                .associateBy { it.fødselsnummer!!.verdi }
        val originalDtoer = dtoRollerTilOppdatering.map { it.rolle }

        return eksisterendeBidragssak.roller.flatMap { eksisterendeRolle ->
            when (eksisterendeRolle.rolleType) {
                Rolletype.BARN -> oppdaterBarnRolle(eksisterendeRolle, barnDtoMap[eksisterendeRolle.fødselsnummer], eksisterendeBidragssak)
                Rolletype.BIDRAGSPLIKTIG, Rolletype.BIDRAGSMOTTAKER -> oppdaterBmBpRolle(originalDtoer, eksisterendeRolle)
                else -> listOf(eksisterendeRolle)
            }
        }
    }

    private fun oppdaterBmBpRolle(
        rolleDtoer: List<RolleDto>,
        rolle: Rolle,
    ): Set<Rolle> {
        val rolleDto = rolleDtoer.find { it.type == rolle.rolleType }
        return if (rolleDto == null) {
            setOf(rolle)
        } else {
            setOf(
                rolle.apply {
                    fødselsnummer = rolleDto.fødselsnummer?.verdi
                    ukjentPart = if (rolle.fødselsnummer == null && rolle.samhandlerIdent == null) UkjentPart.UK else null
                },
            )
        }
    }

    private fun oppdaterBarnRolle(
        eksisterendeRolle: Rolle,
        dtoTilOppdatering: RolleMedFødselsdato?,
        eksisterendeBidragssak: Bidragssak,
    ): List<Rolle> {
        if (dtoTilOppdatering == null) {
            return listOf(eksisterendeRolle)
        }

        val ønsketRmFnr = dtoTilOppdatering.rmFødselsnummer?.verdi
        val ønsketRmSam = dtoTilOppdatering.rmSamhandlerId?.verdi

        // Finn nåværende RM basert på rmRolleId (koblingen barnet har i dag)
        val nåværendeRm: Rolle? =
            eksisterendeRolle.rmRolleId?.let { rmId ->
                eksisterendeBidragssak.roller.firstOrNull { it.rolleId == rmId && it.rolleType == Rolletype.REELMOTTAKER }
            }

        val rmIdentMatch =
            nåværendeRm?.let {
                (ønsketRmFnr != null && it.fødselsnummer == ønsketRmFnr) ||
                    (ønsketRmSam != null && it.samhandlerIdent == ønsketRmSam)
            } ?: false

        // Hvis RM i request er lik nåværende RM -> ikke lag ny RM-rolle, ikke nullstill kobling
        if (dtoTilOppdatering.harRM && rmIdentMatch) {
            eksisterendeRolle.apply {
                mottagerErVerge = dtoTilOppdatering.rolle.mottagerErVerge
            }
            return listOf(eksisterendeRolle)
        }

        // Gjenbruk en eksisterende RM-rolle i saken (samme ident), i stedet for å lage ny
        val eksisterendeRmMedSammeIdent: Rolle? =
            if (dtoTilOppdatering.harRM) {
                eksisterendeBidragssak.roller.firstOrNull {
                    it.rolleType == Rolletype.REELMOTTAKER &&
                        (
                            (ønsketRmFnr != null && it.fødselsnummer == ønsketRmFnr) ||
                                (ønsketRmSam != null && it.samhandlerIdent == ønsketRmSam)
                            )
                }
            } else {
                null
            }

        eksisterendeRolle.apply {
            mottagerErVerge = dtoTilOppdatering.rolle.mottagerErVerge
            rmRolleId = null
        }

        // Hvis request ikke har RM: returner kun barn.
        if (!dtoTilOppdatering.harRM) {
            return listOf(eksisterendeRolle)
        }

        // Hvis RM finnes allerede: ikke lag ny RM-rolle og gjenbruk eksisterende RM-rolle med samme ident
        if (eksisterendeRmMedSammeIdent != null) {
            return listOf(eksisterendeRolle, eksisterendeRmMedSammeIdent)
        }

        // Ellers lag ny RM-rolle
        val nyReellMottaker =
            Rolle(
                fødselsnummer = ønsketRmFnr,
                samhandlerIdent = ønsketRmSam,
                rolleType = Rolletype.REELMOTTAKER,
                fødselsdato = dtoTilOppdatering.rmFødselsnummer?.let { dtoTilOppdatering.fødselsdato },
                bidragssak = eksisterendeBidragssak,
            )

        return listOf(eksisterendeRolle, nyReellMottaker)
    }
}
