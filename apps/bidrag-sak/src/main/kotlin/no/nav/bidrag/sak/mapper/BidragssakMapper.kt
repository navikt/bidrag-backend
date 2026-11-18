package no.nav.bidrag.sak.mapper

import no.nav.bidrag.commons.service.AppContext
import no.nav.bidrag.commons.util.IdentConsumer
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.sak.Fogdårsak
import no.nav.bidrag.domene.enums.sak.UkjentPart
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.ident.ReellMottaker
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.sak.domain.Bidragssak
import no.nav.bidrag.sak.domain.Forelderrolle
import no.nav.bidrag.sak.domain.FØRSTE_OBJEKTNUMMER_BARN
import no.nav.bidrag.sak.domain.Rolle
import no.nav.bidrag.sak.domain.Rollehistorikk
import no.nav.bidrag.sak.domain.Tilgang
import no.nav.bidrag.sak.mapper.RollehistorikkMapper.toRollehistorikkDto
import no.nav.bidrag.sak.mapper.model.RolleMedFødselsdato
import no.nav.bidrag.transport.sak.BarnISak
import no.nav.bidrag.transport.sak.OpprettSakRequest
import no.nav.bidrag.transport.sak.OpprettSakResponse
import no.nav.bidrag.transport.sak.ReellMottakerDto
import no.nav.bidrag.transport.sak.RolleDto
import no.nav.bidrag.transport.sak.RollehistorikkDto
import java.time.LocalDate

object BidragssakMapper {
    fun OpprettSakRequest.toBidragssak(
        saksnummer: Saksnummer,
        fødselsdatoer: Map<Personident, LocalDate?>,
    ): Bidragssak {
        val sak =
            Bidragssak(
                saksnummer = saksnummer.verdi,
                eierfogd = this.eierfogd.verdi,
                kategori = this.kategori,
                ansatt = this.ansatt,
                inhabilitet = this.inhabilitet,
                levdeAdskilt = this.levdeAdskilt,
                konvensjon = this.konvensjon,
                konvensjonsdato = this.konvensjonsdato,
                ffuReferansenr = this.ffuReferansenr,
                land = this.land?.verdi,
                arbeidsfordeling = this.arbeidsfordeling,
            )

        sak.roller.addAll(
            this.roller.toRoller(fødselsdatoer, sak).map {
                it.bidragssak = sak
                it
            },
        )
        sak.tilganger.addAll(mutableSetOf(Tilgang(enhetsnummer = this.eierfogd.verdi, årsak = Fogdårsak.EIER, bidragssak = sak)))
        return sak
    }

    fun Collection<RolleDto>.toRoller(
        fødselsdatoer: Map<Personident, LocalDate?>,
        bidragssak: Bidragssak,
    ): Set<Rolle> = (this.mapBarnTilRoller(fødselsdatoer, bidragssak = bidragssak) + this.mapForeldreTilRoller(fødselsdatoer)).toSet()

    private fun Collection<RolleDto>.mapForeldreTilRoller(
        fødselsdatoer: Map<Personident, LocalDate?>,
    ): List<Rolle> = Forelderrolle.entries.map { forelderrolle ->
        val rolleDto = this.firstOrNull { it.type == forelderrolle.rolletype }
        Rolle(
            fødselsnummer = rolleDto?.fødselsnummer?.verdi,
            objektnummer = forelderrolle.objektnummer,
            rolleType = forelderrolle.rolletype,
            ukjentPart = if (rolleDto?.fødselsnummer?.verdi.isNullOrBlank()) UkjentPart.UK else null,
            mottagerErVerge = rolleDto?.mottagerErVerge ?: false,
            fødselsdato = fødselsdatoer[rolleDto?.fødselsnummer],
        )
    }

    fun Collection<RolleDto>.mapBpBmTilRoller(
        fødselsdatoer: Map<Personident, LocalDate?>,
        bidragssak: Bidragssak,
    ): List<Rolle> = this
        .mapNotNull { dto -> Forelderrolle.fra(dto.type)?.let { dto to it } }
        .map { (dto, forelderrolle) ->
            Rolle(
                fødselsnummer = dto.fødselsnummer?.verdi,
                rolleType = forelderrolle.rolletype,
                objektnummer = forelderrolle.objektnummer,
                ukjentPart = if (dto.fødselsnummer == null) UkjentPart.UK else null,
                mottagerErVerge = dto.mottagerErVerge,
                fødselsdato = fødselsdatoer[dto.fødselsnummer],
                bidragssak = bidragssak,
            )
        }

    fun Collection<RolleDto>.mapBarnTilRoller(
        fødselsdatoer: Map<Personident, LocalDate?>,
        førsteLedigeObjektnummer: Int = FØRSTE_OBJEKTNUMMER_BARN,
        bidragssak: Bidragssak,
    ): List<Rolle> = this
        .filter { it.type == Rolletype.BARN }
        .flatMapIndexed { index, dto ->
            val objektnummer = "%02d".format(index + førsteLedigeObjektnummer)
            val rmFnr = dto.rmFødselsnummer()
            val rmSamhandler = dto.rmSamhandlerId()

            val barn =
                Rolle(
                    fødselsnummer = dto.fødselsnummer?.verdi,
                    objektnummer = objektnummer,
                    rolleType = dto.type,
                    mottagerErVerge = dto.mottagerErVerge,
                    fødselsdato = fødselsdatoer[dto.fødselsnummer],
                    bidragssak = bidragssak,
                )

            if (dto.harRM()) {
                val rm =
                    Rolle(
                        fødselsnummer = rmFnr?.verdi,
                        samhandlerIdent = rmSamhandler?.verdi,
                        rolleType = Rolletype.REELMOTTAKER,
                        fødselsdato = rmFnr?.let { fødselsdatoer[it] },
                        bidragssak = bidragssak,
                    )
                listOf(barn, rm)
            } else {
                listOf(barn)
            }
        }

    fun Bidragssak.toOpprettSakResponse() = OpprettSakResponse(saksnummer = Saksnummer(this.saksnummer))
}

object RolleMapper {
    fun Collection<Rolle>.toRolleDto(visRollehistorikk: Boolean) = this.mapNotNull {
        if (!it.erPerson()) {
            null
        } else {
            val reellMottaker =
                it.rmRolleId
                    ?.let { rolleId -> this.find { rolle -> rolle.rolleId == rolleId } }

            val rmFnr = reellMottaker?.fødselsnummer
            val rmSamhandler = reellMottaker?.samhandlerIdent

            RolleDto(
                fødselsnummer = it.fødselsnummer?.let { fødelsnummer -> hentGjeldendeIdent(fødelsnummer) },
                type = it.rolleType,
                objektnummer = it.objektnummer,
                reellMottager =
                when {
                    !rmFnr.isNullOrBlank() -> ReellMottaker(rmFnr)
                    !rmSamhandler.isNullOrBlank() -> ReellMottaker(rmSamhandler)
                    else -> null
                },
                reellMottaker =
                when {
                    !rmFnr.isNullOrBlank() -> ReellMottakerDto(ReellMottaker(rmFnr), reellMottaker.mottagerErVerge)
                    !rmSamhandler.isNullOrBlank() -> ReellMottakerDto(ReellMottaker(rmSamhandler), reellMottaker.mottagerErVerge)
                    else -> null
                },
                rollehistorikk = if (visRollehistorikk) it.rollehistorikk.toRollehistorikkDto(this as Set<Rolle>) else emptyList(),
            )
        }
    }
}

object RollehistorikkMapper {
    fun Collection<Rollehistorikk>.toRollehistorikkDto(roller: Set<Rolle>) = this.map { rollehistorikk ->
        val reellMottakerErVerge =
            rollehistorikk.rmRolleId
                ?.let { rMRolleid -> roller.find { it.rolleId == rMRolleid }?.mottagerErVerge }
                ?: false

        val reellMottakerSamhandlerident =
            rollehistorikk.rmRolleId
                ?.let { rMRolleid -> roller.firstOrNull { it.rolleId == rMRolleid }?.samhandlerIdent }

        RollehistorikkDto(
            fødselsnummer = rollehistorikk.rolleFødselsnummer?.let { fødselsnummer -> hentGjeldendeIdent(fødselsnummer) },
            type = rollehistorikk.type!!,
            reellMottaker =
            if (rollehistorikk.rmRolleId != null &&
                (rollehistorikk.rmRolleFødselsnummer != null || reellMottakerSamhandlerident != null)
            ) {
                ReellMottakerDto(
                    ident = ReellMottaker(rollehistorikk.rmRolleFødselsnummer ?: reellMottakerSamhandlerident!!),
                    verge = reellMottakerErVerge,
                )
            } else {
                null
            },
            typeEndring = rollehistorikk.typeEndring,
            opprettetAv = rollehistorikk.opprettetAv,
            opprettetTidspunkt = rollehistorikk.opprettetTidspunkt,
        )
    }
}

fun Collection<Rolle>.personidentBidragspliktig(): Personident? = this.firstOrNull { it.rolleType == Rolletype.BIDRAGSPLIKTIG }?.fødselsnummer?.let { Personident(it) }

fun Collection<Rolle>.personidentBidragsmottaker(): Personident? = this.firstOrNull { it.rolleType == Rolletype.BIDRAGSMOTTAKER }?.fødselsnummer?.let { Personident(it) }

fun Collection<Rolle>.toBarnISak() = this
    .filter { it.rolleType == Rolletype.BARN }
    .map {
        val personidentReellMottaker =
            it.rmRolleId
                ?.let { rolleId -> this.find { rolle -> rolle.rolleId == rolleId } }
                ?.let { rolle -> rolle.fødselsnummer ?: rolle.samhandlerIdent }
                ?.let { ident -> ReellMottaker(ident) }

        BarnISak(
            ident = it.fødselsnummer?.let { fødselsnummer -> hentGjeldendeIdent(fødselsnummer) },
            reellMottaker = personidentReellMottaker,
        )
    }

fun RolleDto.medFødselsdato(fødselsdatoer: Map<Personident, LocalDate?>) = RolleMedFødselsdato(this, this.fødselsnummer?.let { fødselsdatoer[it] })

private fun hentGjeldendeIdent(ident: String) = Personident(AppContext.getBean(IdentConsumer::class.java).hentGjeldendeIdent(ident) ?: ident)
