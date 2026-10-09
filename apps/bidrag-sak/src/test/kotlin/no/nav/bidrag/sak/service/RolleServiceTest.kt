package no.nav.bidrag.sak.service

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.rolle.TypeEndring
import no.nav.bidrag.domene.enums.sak.Bidragssakstatus
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.ident.ReellMottaker
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.sak.domain.Bidragssak
import no.nav.bidrag.sak.domain.Rolle
import no.nav.bidrag.sak.integration.person.BidragPersonClient
import no.nav.bidrag.sak.integration.samhandler.BidragSamhandlerClient
import no.nav.bidrag.transport.person.Identgruppe
import no.nav.bidrag.transport.person.PersonidentDto
import no.nav.bidrag.transport.sak.ReellMottakerDto
import no.nav.bidrag.transport.sak.RolleDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

internal class RolleServiceTest {
    val bidragPersonClient = mockk<BidragPersonClient>()
    val samhandlerClient = mockk<BidragSamhandlerClient>()
    val rolleService = RolleService(bidragPersonClient, samhandlerClient)

    val fødselsnummerBarn1 = genererFødselsnummer(LocalDate.now().minusYears(1))
    val fødselsnummerBarn2 = genererFødselsnummer(LocalDate.now().minusYears(2))
    val fødselsnummerBp = genererFødselsnummer(LocalDate.now().minusYears(3))
    val fødselsnummerRm = genererFødselsnummer(LocalDate.now().minusYears(4))
    lateinit var bidragssak: Bidragssak

    @BeforeEach
    fun setup() {
        bidragssak =
            Bidragssak(
                saksnummer = "123",
                eierfogd = "1234",
                status = Bidragssakstatus.NY,
            )
    }

    @Test
    fun `skal oppdatere rollene for barn og bm korrekt`() {
        val lagredeRoller =
            mutableSetOf(
                Rolle(fødselsnummer = fødselsnummerBarn1, rolleType = Rolletype.BARN, bidragssak = bidragssak),
                Rolle(fødselsnummer = fødselsnummerBp, rolleType = Rolletype.BIDRAGSPLIKTIG, bidragssak = bidragssak),
            )

        bidragssak.roller = lagredeRoller

        val rolleDtoer =
            setOf(
                RolleDto(
                    fødselsnummer = Personident(fødselsnummerBarn1),
                    type = Rolletype.BARN,
                    mottagerErVerge = true,
                ),
                RolleDto(fødselsnummer = Personident(fødselsnummerBarn2), type = Rolletype.BARN),
            )

        val fødselsdatoer =
            mapOf(
                Personident(fødselsnummerBarn1) to null,
                Personident(fødselsnummerBarn2) to null,
            )

        val result = rolleService.oppdaterRoller(bidragssak, rolleDtoer, fødselsdatoer)
        result shouldHaveSize 3
        result.first { it.fødselsnummer == fødselsnummerBarn1 }.mottagerErVerge shouldBe true
        result.first { it.fødselsnummer == fødselsnummerBarn1 }.rolleType shouldBe Rolletype.BARN
        result.first { it.fødselsnummer == fødselsnummerBp }.rolleType shouldBe Rolletype.BIDRAGSPLIKTIG
        result.first { it.fødselsnummer == fødselsnummerBarn2 }.rolleType shouldBe Rolletype.BARN
    }

    @Test
    fun `skal godta roller når alle personer har fødselsdato-oppslag`() {
        val roller =
            setOf(
                RolleDto(fødselsnummer = Personident(fødselsnummerBarn1), type = Rolletype.BARN),
                RolleDto(fødselsnummer = Personident(fødselsnummerBp), type = Rolletype.BIDRAGSPLIKTIG),
            )
        val fødselsdatoer = mapOf(Personident(fødselsnummerBarn1) to null, Personident(fødselsnummerBp) to null)

        assertDoesNotThrow { rolleService.validerRoller(roller, fødselsdatoer) }
    }

    @Test
    fun `skal fungere på tomme roller`() {
        val lagredeRoller = mutableSetOf(Rolle(fødselsnummer = fødselsnummerBarn1, rolleType = Rolletype.BARN, bidragssak = bidragssak))
        bidragssak.roller = lagredeRoller
        val rolleDtoer = emptySet<RolleDto>()

        val result = rolleService.oppdaterRoller(bidragssak, rolleDtoer, emptyMap())

        result shouldBe setOf(Rolle(fødselsnummer = fødselsnummerBarn1, rolleType = Rolletype.BARN, bidragssak = bidragssak))
    }

    @Test
    fun `skal håndtere roller med feil fødselsnummer`() {
        val lagredeRoller =
            mutableSetOf(
                Rolle(fødselsnummer = null, samhandlerIdent = null, rolleType = Rolletype.BIDRAGSPLIKTIG, bidragssak = bidragssak),
            )

        bidragssak.roller = lagredeRoller

        val fødselsdatoer = mapOf(Personident(fødselsnummerBarn1) to null)

        val rolleDtoer =
            setOf(
                RolleDto(fødselsnummer = Personident(fødselsnummerBp), type = Rolletype.BIDRAGSPLIKTIG),
            )

        assertThrows<IllegalArgumentException> { rolleService.oppdaterRoller(bidragssak, rolleDtoer, fødselsdatoer) }
    }

    @Test
    fun `skal håndtere roller med oppdatering av rm`() {
        val lagredeRoller =
            setOf(
                Rolle(fødselsnummer = fødselsnummerRm, rolleType = Rolletype.REELMOTTAKER),
                Rolle(fødselsnummer = fødselsnummerBarn1, rolleType = Rolletype.BARN),
                Rolle(fødselsnummer = fødselsnummerBarn1, rolleType = Rolletype.REELMOTTAKER),
            )
        val rolleDtoer =
            listOf(
                RolleDto(
                    fødselsnummer = Personident(fødselsnummerBarn1),
                    type = Rolletype.REELMOTTAKER,
                    reellMottager = ReellMottaker(fødselsnummerBarn1),
                ),
            )

        every { bidragPersonClient.hentFødselsdatoer(any()) } returns
            mapOf(
                Personident(fødselsnummerBarn1) to null,
                Personident(fødselsnummerRm) to null,
            )

        val result = rolleService.oppdaterRollerMedReelleMottager(lagredeRoller, rolleDtoer)

        result shouldHaveSize 3
        result
            .first {
                it.fødselsnummer == fødselsnummerBarn1 && it.rolleType == Rolletype.BARN
            }.rolleType shouldBe Rolletype.BARN
        result
            .first {
                it.fødselsnummer == fødselsnummerBarn1 && it.rolleType == Rolletype.REELMOTTAKER
            }.rolleType shouldBe Rolletype.REELMOTTAKER
        result
            .first {
                it.fødselsnummer == fødselsnummerRm && it.rolleType == Rolletype.REELMOTTAKER
            }.rolleType shouldBe Rolletype.REELMOTTAKER
    }

    @Test
    fun `skal ikke opprette ny RM-rolle og ikke nullstille kobling når RM er uendret for barnet`() {
        val rmRolle =
            Rolle(
                fødselsnummer = fødselsnummerRm,
                rolleType = Rolletype.REELMOTTAKER,
                bidragssak = bidragssak,
            ).apply { rolleId = 100 }

        val barnRolle =
            Rolle(
                fødselsnummer = fødselsnummerBarn1,
                rolleType = Rolletype.BARN,
                bidragssak = bidragssak,
            ).apply { rmRolleId = rmRolle.rolleId }

        bidragssak.roller = mutableSetOf(barnRolle, rmRolle)

        val rolleDtoer =
            setOf(
                RolleDto(
                    fødselsnummer = Personident(fødselsnummerBarn1),
                    type = Rolletype.BARN,
                    mottagerErVerge = true,
                    reellMottager = ReellMottaker(fødselsnummerRm), // samme RM som allerede er koblet
                ),
            )

        val fødselsdatoer =
            mapOf(
                Personident(fødselsnummerBarn1) to null,
                Personident(fødselsnummerRm) to null,
            )

        val result = rolleService.oppdaterRoller(bidragssak, rolleDtoer, fødselsdatoer)

        result shouldHaveSize 2 // Barn + eksisterende RM (ingen ny RM-rolle)
        result.count { it.rolleType == Rolletype.REELMOTTAKER && it.fødselsnummer == fødselsnummerRm } shouldBe 1

        val barnEtter = result.first { it.rolleType == Rolletype.BARN && it.fødselsnummer == fødselsnummerBarn1 }
        barnEtter.mottagerErVerge shouldBe true
        barnEtter.rmRolleId shouldBe 100 // skal IKKE nullstilles når RM er uendret
    }

    @Test
    fun `skal gjenbruke eksisterende RM-rolle i saken når request setter RM og den allerede finnes`() {
        val eksisterendeRmRolle =
            Rolle(
                fødselsnummer = fødselsnummerRm,
                rolleType = Rolletype.REELMOTTAKER,
                bidragssak = bidragssak,
            ).apply { rolleId = 200 }

        val barnRolle =
            Rolle(
                fødselsnummer = fødselsnummerBarn1,
                rolleType = Rolletype.BARN,
                bidragssak = bidragssak,
            ).apply {
                // barnet er ikke koblet (evt bytter RM) - viktig at vi IKKE allerede matcher "nåværende RM"
                rmRolleId = null
            }

        bidragssak.roller = mutableSetOf(barnRolle, eksisterendeRmRolle)

        val rolleDtoer =
            setOf(
                RolleDto(
                    fødselsnummer = Personident(fødselsnummerBarn1),
                    type = Rolletype.BARN,
                    reellMottager = ReellMottaker(fødselsnummerRm), // RM finnes allerede i saken
                ),
            )

        val fødselsdatoer =
            mapOf(
                Personident(fødselsnummerBarn1) to null,
                Personident(fødselsnummerRm) to null,
            )

        val result = rolleService.oppdaterRoller(bidragssak, rolleDtoer, fødselsdatoer)

        result shouldHaveSize 2 // Barn + eksisterende RM (ingen ny RM-rolle)
        result.count { it.rolleType == Rolletype.REELMOTTAKER && it.fødselsnummer == fødselsnummerRm } shouldBe 1

        val barnEtter = result.first { it.rolleType == Rolletype.BARN && it.fødselsnummer == fødselsnummerBarn1 }
        // på dette tidspunktet er ikke barnet koblet til RM-rollen enda, det skjer senere i prosessen (oppdaterRollerMedReelleMottager)
        barnEtter.rmRolleId shouldBe null
    }

    @Test
    fun `oppdaterRollerMedReelleMottager skal koble barn til riktig RM rolleId`() {
        val rmRolle =
            Rolle(
                fødselsnummer = fødselsnummerRm,
                rolleType = Rolletype.REELMOTTAKER,
                bidragssak = bidragssak,
            ).apply { rolleId = 42 }

        val barnRolle =
            Rolle(
                fødselsnummer = fødselsnummerBarn1,
                rolleType = Rolletype.BARN,
                bidragssak = bidragssak,
            ).apply { rmRolleId = null }

        val lagredeRoller = setOf(barnRolle, rmRolle)

        val rolleDtoer =
            listOf(
                RolleDto(
                    fødselsnummer = Personident(fødselsnummerBarn1),
                    type = Rolletype.BARN,
                    reellMottager = ReellMottaker(fødselsnummerRm),
                ),
            )

        val result = rolleService.oppdaterRollerMedReelleMottager(lagredeRoller, rolleDtoer)

        result shouldHaveSize 2
        val barnEtter = result.first { it.rolleType == Rolletype.BARN && it.fødselsnummer == fødselsnummerBarn1 }
        barnEtter.rmRolleId shouldBe 42
    }

    @Test
    fun `oppdaterRollerMedReelleMottager skal returnere uendret når ingen roller har RM`() {
        val barnRolle =
            Rolle(
                fødselsnummer = fødselsnummerBarn1,
                rolleType = Rolletype.BARN,
                bidragssak = bidragssak,
            ).apply { rmRolleId = null }

        val lagredeRoller = setOf(barnRolle)

        val result = rolleService.oppdaterRollerMedReelleMottager(lagredeRoller, emptyList())

        result shouldBe lagredeRoller
    }

    @Test
    fun `oppdaterer lagret barn og RM til gjeldende ident med ENDRE_FNR-historikk`() {
        val gammelBarnIdent = fødselsnummerBarn1
        val nyBarnIdent = fødselsnummerBarn2
        val gammelRmIdent = fødselsnummerRm
        val nyRmIdent = fødselsnummerBp
        every { bidragPersonClient.hentPersonidenter(gammelBarnIdent) } returns personidenter(nyBarnIdent, gammelBarnIdent)
        every { bidragPersonClient.hentPersonidenter(gammelRmIdent) } returns personidenter(nyRmIdent, gammelRmIdent)
        val barn = Rolle(rolleId = 3, fødselsnummer = gammelBarnIdent, rolleType = Rolletype.BARN, bidragssak = bidragssak)
        val rm = Rolle(rolleId = 4, fødselsnummer = gammelRmIdent, rolleType = Rolletype.REELMOTTAKER, bidragssak = bidragssak)

        rolleService.oppdaterTilGjeldendeIdent(listOf(barn, rm))

        barn.fødselsnummer shouldBe nyBarnIdent
        rm.fødselsnummer shouldBe nyRmIdent
        listOf(barn to gammelBarnIdent, rm to gammelRmIdent).forEach { (rolle, gammelIdent) ->
            val historikk = rolle.rollehistorikk.single()
            historikk.typeEndring shouldBe TypeEndring.ENDRE_FNR
            historikk.type shouldBe rolle.rolleType
            historikk.rolleFødselsnummer shouldBe gammelIdent
            historikk.rmRolleFødselsnummer shouldBe rolle.fødselsnummer
        }
    }

    @Test
    fun `endrer ikke rolle som allerede har gjeldende ident`() {
        every { bidragPersonClient.hentPersonidenter(fødselsnummerBarn1) } returns personidenter(fødselsnummerBarn1)
        val barn = Rolle(rolleId = 3, fødselsnummer = fødselsnummerBarn1, rolleType = Rolletype.BARN, bidragssak = bidragssak)

        rolleService.oppdaterTilGjeldendeIdent(listOf(barn))

        barn.fødselsnummer shouldBe fødselsnummerBarn1
        barn.rollehistorikk shouldHaveSize 0
    }

    @Test
    fun `bruker gjeldende ident for barn og RM i forespørsel`() {
        every { bidragPersonClient.hentPersonidenter(fødselsnummerBarn1) } returns personidenter(fødselsnummerBarn2, fødselsnummerBarn1)
        every { bidragPersonClient.hentPersonidenter(fødselsnummerRm) } returns personidenter(fødselsnummerBp, fødselsnummerRm)
        val forespørsel = RolleDto(
            type = Rolletype.BARN,
            fødselsnummer = Personident(fødselsnummerBarn1),
            reellMottaker = ReellMottakerDto(ident = ReellMottaker(fødselsnummerRm), verge = false),
        )

        val resultat = rolleService.brukGjeldendeIdent(setOf(forespørsel)).single()

        resultat.fødselsnummer shouldBe Personident(fødselsnummerBarn2)
        resultat.reellMottaker?.ident shouldBe ReellMottaker(fødselsnummerBp)
    }

    private fun personidenter(gjeldende: String, vararg historiske: String) = listOf(PersonidentDto(gjeldende, historisk = false, gruppe = Identgruppe.FOLKEREGISTERIDENT)) +
        historiske.map { PersonidentDto(it, historisk = true, gruppe = Identgruppe.FOLKEREGISTERIDENT) }
}
