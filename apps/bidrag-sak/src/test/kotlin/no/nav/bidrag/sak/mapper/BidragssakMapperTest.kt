@file:Suppress("PropertyName")

package no.nav.bidrag.sak.mapper

import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.ident.ReellMottaker
import no.nav.bidrag.domene.ident.SamhandlerId
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.sak.domain.Bidragssak
import no.nav.bidrag.sak.domain.Rolle
import no.nav.bidrag.transport.sak.OpprettSakRequest
import no.nav.bidrag.transport.sak.ReellMottakerDto
import no.nav.bidrag.transport.sak.RolleDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BidragssakMapperTest {
    private val FNR_BARN = Personident("06510985255")
    private val FNR_RM = Personident("02451550643")
    private val SAM_RM = SamhandlerId("85000000083")

    private fun tomSak() = Bidragssak(
        saksnummer = "25000001",
        eierfogd = "2990",
    )

    private fun fødselsdatoer(vararg identer: Personident) = identer.associateWith { LocalDate.of(2010, 1, 1) }

    @Nested
    inner class MapBarnTilRoller {
        @Test
        fun `barn uten reell mottaker - kun barnerolle`() {
            val roller =
                listOf(
                    RolleDto(
                        fødselsnummer = FNR_BARN,
                        type = Rolletype.BARN,
                    ),
                )

            val result: List<Rolle> =
                with(BidragssakMapper) {
                    roller.mapBarnTilRoller(
                        fødselsdatoer = fødselsdatoer(FNR_BARN),
                        bidragssak = tomSak(),
                    )
                }

            assertThat(result).hasSize(1)
            val barn = result.single()
            assertThat(barn.rolleType).isEqualTo(Rolletype.BARN)
            assertThat(barn.fødselsnummer).isEqualTo(FNR_BARN.verdi)
            assertThat(barn.samhandlerIdent).isNull()
        }

        @Test
        fun `barn med reellMottager=FNR (deprecated streng) - oppretter REELMOTTAKER person`() {
            val roller =
                listOf(
                    RolleDto(
                        fødselsnummer = FNR_BARN,
                        type = Rolletype.BARN,
                        reellMottager = ReellMottaker(FNR_RM.verdi),
                    ),
                )

            val result =
                with(BidragssakMapper) {
                    roller.mapBarnTilRoller(
                        fødselsdatoer = fødselsdatoer(FNR_BARN, FNR_RM),
                        bidragssak = tomSak(),
                    )
                }

            assertThat(result).hasSize(2)
            val barn = result.first { it.rolleType == Rolletype.BARN }
            val rm = result.first { it.rolleType == Rolletype.REELMOTTAKER }

            assertThat(barn.fødselsnummer).isEqualTo(FNR_BARN.verdi)
            assertThat(rm.fødselsnummer).isEqualTo(FNR_RM.verdi)
            assertThat(rm.samhandlerIdent).isNull()
        }

        @Test
        fun `barn med reellMottaker DTO (ident=FNR) - oppretter REELMOTTAKER person`() {
            val roller =
                listOf(
                    RolleDto(
                        fødselsnummer = FNR_BARN,
                        type = Rolletype.BARN,
                        reellMottaker = ReellMottakerDto(ident = ReellMottaker(FNR_RM.verdi), verge = true),
                    ),
                )

            val result =
                with(BidragssakMapper) {
                    roller.mapBarnTilRoller(
                        fødselsdatoer = fødselsdatoer(FNR_BARN, FNR_RM),
                        bidragssak = tomSak(),
                    )
                }

            assertThat(result).hasSize(2)
            val rm = result.first { it.rolleType == Rolletype.REELMOTTAKER }
            assertThat(rm.fødselsnummer).isEqualTo(FNR_RM.verdi)
            assertThat(rm.samhandlerIdent).isNull()
        }

        @Test
        fun `barn med reellMottaker DTO (ident=SamhandlerId) - oppretter REELMOTTAKER samhandler`() {
            val roller =
                listOf(
                    RolleDto(
                        fødselsnummer = FNR_BARN,
                        type = Rolletype.BARN,
                        reellMottaker = ReellMottakerDto(ident = ReellMottaker(SAM_RM.verdi), verge = false),
                    ),
                )

            val result =
                with(BidragssakMapper) {
                    roller.mapBarnTilRoller(
                        fødselsdatoer = fødselsdatoer(FNR_BARN), // RM er samhandler -> ingen fødselsdato i map
                        bidragssak = tomSak(),
                    )
                }

            assertThat(result).hasSize(2)
            val barn = result.first { it.rolleType == Rolletype.BARN }
            val rm = result.first { it.rolleType == Rolletype.REELMOTTAKER }

            assertThat(barn.fødselsnummer).isEqualTo(FNR_BARN.verdi)
            assertThat(rm.fødselsnummer).isNull() // Samhandler, ikke person
            assertThat(rm.samhandlerIdent).isEqualTo(SAM_RM.verdi)
        }

        @Test
        fun `barn med reellMottager=SamhandlerId (deprecated streng) - oppretter REELMOTTAKER samhandler`() {
            val roller =
                listOf(
                    RolleDto(
                        fødselsnummer = FNR_BARN,
                        type = Rolletype.BARN,
                        reellMottager = ReellMottaker(SAM_RM.verdi),
                    ),
                )

            val result =
                with(BidragssakMapper) {
                    roller.mapBarnTilRoller(
                        fødselsdatoer = fødselsdatoer(FNR_BARN),
                        bidragssak = tomSak(),
                    )
                }

            assertThat(result).hasSize(2)
            val rm = result.first { it.rolleType == Rolletype.REELMOTTAKER }
            assertThat(rm.fødselsnummer).isNull()
            assertThat(rm.samhandlerIdent).isEqualTo(SAM_RM.verdi)
        }
    }

    @Nested
    inner class ToBidragssak {
        @Test
        fun `toBidragssak oppretter sak med tilganger og mapper barn+RM korrekt`() {
            val request =
                OpprettSakRequest(
                    eierfogd = Enhetsnummer("2990"),
                    roller =
                    setOf(
                        RolleDto(type = Rolletype.BIDRAGSPLIKTIG, fødselsnummer = Personident("23917599432")),
                        RolleDto(type = Rolletype.BIDRAGSMOTTAKER, fødselsnummer = Personident("27837598708")),
                        RolleDto(
                            type = Rolletype.BARN,
                            fødselsnummer = FNR_BARN,
                            reellMottaker = ReellMottakerDto(ident = ReellMottaker(SAM_RM.verdi), verge = false),
                        ),
                    ),
                )

            val fødselsdatoer =
                fødselsdatoer(
                    Personident("23917599432"),
                    Personident("27837598708"),
                    FNR_BARN,
                )

            val saksnr = Saksnummer("25000077")
            val sak =
                request.let { req ->
                    BidragssakMapper.run {
                        req.toBidragssak(saksnr, fødselsdatoer)
                    }
                }

            // Saksmetadata
            assertThat(sak.saksnummer).isEqualTo(saksnr.verdi)
            assertThat(sak.eierfogd).isEqualTo("2990")

            // Tilgang for eierfogd skal ligge inne
            assertThat(sak.tilganger).anySatisfy { t ->
                assertThat(t.enhetsnummer).isEqualTo("2990")
            }

            // Roller: BP, BM, BA + REELMOTTAKER(samhandler)
            assertThat(sak.roller).hasSize(4)
            assertThat(sak.roller.count { it.rolleType == Rolletype.BIDRAGSPLIKTIG }).isEqualTo(1)
            assertThat(sak.roller.count { it.rolleType == Rolletype.BIDRAGSMOTTAKER }).isEqualTo(1)
            assertThat(sak.roller.count { it.rolleType == Rolletype.BARN }).isEqualTo(1)
            assertThat(sak.roller.count { it.rolleType == Rolletype.REELMOTTAKER }).isEqualTo(1)

            val rm = sak.roller.first { it.rolleType == Rolletype.REELMOTTAKER }
            assertThat(rm.fødselsnummer).isNull()
            assertThat(rm.samhandlerIdent).isEqualTo(SAM_RM.verdi)
        }
    }
}
