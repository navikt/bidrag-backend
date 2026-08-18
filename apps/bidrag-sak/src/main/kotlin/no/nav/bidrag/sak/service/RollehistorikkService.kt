package no.nav.bidrag.sak.service

import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.rolle.TypeEndring
import no.nav.bidrag.sak.domain.Bidragssak
import no.nav.bidrag.sak.domain.Rolle
import no.nav.bidrag.sak.domain.Rollehistorikk
import no.nav.bidrag.transport.sak.RolleDto
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class RollehistorikkService {
    fun oppdaterRollehistorikk(
        rollerFørOppdatering: List<RolleDto>,
        requestRolleDto: Set<RolleDto>,
        oppdatertBidragssak: Bidragssak,
    ): MutableSet<Rolle> {
        val roller = oppdatertBidragssak.roller
        val oppdaterteRoller = mutableSetOf<Rolle>()

        val opprettetAv =
            TokenUtils.hentSaksbehandlerIdent()
                ?: TokenUtils.hentApplikasjonsnavn()
                ?: ""

        // Finn hvilke endringer som er gjort for barn i saken, og opprett Rollehistorikk for disse endringene
        roller.forEach { rolle ->
            // Rollehistorikk skal ikke skrives for BM eller BP
            if (rolle.rolleType == Rolletype.BIDRAGSPLIKTIG || rolle.rolleType == Rolletype.BIDRAGSMOTTAKER) {
                oppdaterteRoller.add(rolle)
            } else {
                val matchendeRequestRolleDto =
                    requestRolleDto.firstOrNull {
                        it.fødselsnummer?.verdi == rolle.fødselsnummer && it.type == rolle.rolleType
                    }
                if (matchendeRequestRolleDto == null) {
                    // Rollen er ikke endret og det skal derfor ikke opprettes noen rollehistorikk
                    oppdaterteRoller.add(rolle)
                } else {
                    when (matchendeRequestRolleDto.reellMottaker) {
                        null -> {
                            // sjekk om det lå rm på rollen fra før, hvis ja så skriv til rollehistorikk.
                            if (rollerFørOppdatering.any {
                                    it.fødselsnummer == matchendeRequestRolleDto.fødselsnummer && it.type == rolle.rolleType &&
                                        it.reellMottaker != null
                                }
                            ) {
                                rolle.rollehistorikk.add(
                                    Rollehistorikk(
                                        saksnummer = rolle.bidragssak!!.saksnummer,
                                        rolleFødselsnummer = rolle.fødselsnummer,
                                        type = rolle.rolleType,
                                        rmRolleId = null,
                                        rmRolleFødselsnummer = null,
                                        typeEndring = TypeEndring.SATT_TIL_BM,
                                        opprettetAv = opprettetAv,
                                        opprettetTidspunkt = LocalDateTime.now(),
                                        rolle = rolle,
                                    ),
                                )
                            }
                            oppdaterteRoller.add(rolle)
                        }

                        else -> {
                            // Finner eventuell rm på barnet før oppdatering, og sjekker om det er forskjell på denne og det som ligger i requesten.
                            // Hvis det er forskjell så skriv til rollehistorikk
                            val eksisterendeRm =
                                rollerFørOppdatering
                                    .firstOrNull {
                                        it.fødselsnummer == matchendeRequestRolleDto.fødselsnummer && it.type == Rolletype.BARN
                                    }?.reellMottaker
                                    ?.ident
                                    ?.verdi

                            val nyRm =
                                oppdatertBidragssak.roller.firstOrNull {
                                    it.fødselsnummer == matchendeRequestRolleDto.reellMottaker?.ident?.verdi && it.rolleType ==
                                        Rolletype.REELMOTTAKER
                                } ?: oppdatertBidragssak.roller.firstOrNull {
                                    it.samhandlerIdent == matchendeRequestRolleDto.reellMottaker?.ident?.verdi && it.rolleType ==
                                        Rolletype.REELMOTTAKER
                                }

                            if (eksisterendeRm == null) {
                                rolle.rollehistorikk.add(
                                    Rollehistorikk(
                                        saksnummer = rolle.bidragssak!!.saksnummer,
                                        rolleFødselsnummer = rolle.fødselsnummer,
                                        type = rolle.rolleType,
                                        rmRolleId = nyRm?.rolleId,
                                        rmRolleFødselsnummer = nyRm?.fødselsnummer,
                                        typeEndring = TypeEndring.SATT_NY_RM,
                                        opprettetAv = opprettetAv,
                                        opprettetTidspunkt = LocalDateTime.now(),
                                        rolle = rolle,
                                    ),
                                )
                            } else {
                                if (eksisterendeRm != nyRm?.fødselsnummer && eksisterendeRm != nyRm?.samhandlerIdent) {
                                    // RM er endret og det skal lagres i historikk
                                    rolle.rollehistorikk.add(
                                        Rollehistorikk(
                                            saksnummer = rolle.bidragssak!!.saksnummer,
                                            rolleFødselsnummer = rolle.fødselsnummer,
                                            type = rolle.rolleType,
                                            rmRolleId = nyRm?.rolleId,
                                            rmRolleFødselsnummer = nyRm?.fødselsnummer,
                                            typeEndring = TypeEndring.SATT_RM,
                                            opprettetAv = opprettetAv,
                                            opprettetTidspunkt = LocalDateTime.now(),
                                            rolle = rolle,
                                        ),
                                    )
                                }
                            }
                            oppdaterteRoller.add(rolle)
                        }
                    }
                }
            }
        }
        return oppdaterteRoller
    }
}
