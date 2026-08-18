package no.nav.bidrag.sak.service

import io.kotest.matchers.collections.shouldExist
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.every
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.rolle.TypeEndring
import no.nav.bidrag.domene.enums.sak.Arbeidsfordeling
import no.nav.bidrag.domene.enums.sak.Bidragssakstatus
import no.nav.bidrag.domene.enums.sak.Konvensjon
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.ident.ReellMottaker
import no.nav.bidrag.domene.land.Landkode
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.generer.testdata.person.genererPersonident
import no.nav.bidrag.sak.SpringTestRunner
import no.nav.bidrag.sak.config.BidragOrganisasjonTestConfig
import no.nav.bidrag.sak.mapper.RollehistorikkMapper.toRollehistorikkDto
import no.nav.bidrag.sak.repository.BidragssakRepository
import no.nav.bidrag.sak.repository.findByIdOrThrow
import no.nav.bidrag.sak.util.TransactionHelper
import no.nav.bidrag.transport.person.PersonDto
import no.nav.bidrag.transport.sak.OppdaterSakRequest
import no.nav.bidrag.transport.sak.OpprettSakRequest
import no.nav.bidrag.transport.sak.ReellMottakerDto
import no.nav.bidrag.transport.sak.RolleDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BidragSakServiceIntegrationIT : SpringTestRunner() {
    @Autowired
    private lateinit var bidragssakRepository: BidragssakRepository

    @Autowired
    private lateinit var bidragssakService: BidragSakService

    @Autowired
    private lateinit var th: TransactionHelper

    @BeforeEach
    fun setUpBase() {
    }

    @Nested
    inner class OppdaterSak {
        private lateinit var saksnummer: Saksnummer
        private lateinit var saksnummerMedSamhandlere: Saksnummer

        private val fnrBM = genererPersonident()
        private val fnrBM2 = genererPersonident()
        private val fnrBP = genererPersonident()
        private val fnrBA1 = genererPersonident()
        private val fnrBA2 = genererPersonident()
        private val fnrBA3 = genererPersonident()

        @BeforeEach
        fun setUp() {
            clearMocks(identConsumer)

            MDC.put("callId", "NavCallId")

            // lag personer
            stubPerson(fnrBM, LocalDate.now().minusYears(30))
            stubPerson(fnrBM2, LocalDate.now().minusYears(30))
            stubPerson(fnrBP, LocalDate.now().minusYears(30))

            // Myndige barn (18+): må ha RM i request
            stubPerson(fnrBA1, LocalDate.now().minusYears(18).minusDays(1))
            stubPerson(fnrBA2, LocalDate.now().minusYears(18).minusDays(1))

            // Umyndig barn
            stubPerson(fnrBA3, LocalDate.now().minusYears(10))

            every { identConsumer.hentAlleIdenter(any()) }.answers { listOf(firstArg()) }

            // Sak 1: BA1/BA2 myndige -> har RM. BA3 umyndig -> kan være uten RM.
            saksnummer =
                bidragssakService
                    .opprettSak(
                        OpprettSakRequest(
                            Enhetsnummer("1701"),
                            roller =
                            setOf(
                                RolleDto(fnrBM, Rolletype.BIDRAGSMOTTAKER),
                                RolleDto(fnrBP, Rolletype.BIDRAGSPLIKTIG),
                                rolleBarnMedRm(fnrBA1, rm = fnrBA1.verdi),
                                rolleBarnMedRm(fnrBA2, rm = fnrBA2.verdi),
                                rolleBarnUtenRm(fnrBA3),
                            ),
                        ),
                    ).saksnummer

            // Sak 2 (samhandlere): OGSÅ må BA1/BA2 som er myndige ha RM!
            saksnummerMedSamhandlere =
                bidragssakService
                    .opprettSak(
                        OpprettSakRequest(
                            Enhetsnummer("1701"),
                            roller =
                            setOf(
                                RolleDto(fnrBM2, Rolletype.BIDRAGSMOTTAKER),
                                RolleDto(fnrBP, Rolletype.BIDRAGSPLIKTIG),
                                rolleBarnMedRm(fnrBA1, rm = "80060878901"), // samhandler-RM (gyldig RM)
                                rolleBarnMedRm(fnrBA2, rm = "80070978901"), // samhandler-RM (gyldig RM)
                                rolleBarnMedRm(fnrBA3, rm = "80050778901"),
                            ),
                        ),
                    ).saksnummer
        }

        @Test
        fun `skal oppdatere bidragssak med alle felter fra dto`() {
            bidragssakService.oppdaterSak(
                OppdaterSakRequest(
                    saksnummer = saksnummer,
                    status = Bidragssakstatus.AK,
                    ansatt = true,
                    inhabilitet = true,
                    levdeAdskilt = true,
                    sanertDato = LocalDate.now(),
                    arbeidsfordeling = Arbeidsfordeling.BARNEBORTFØRING,
                    kategorikode = Sakskategori.UTLAND,
                    landkode = Landkode("SWE"),
                    konvensjonskode = Konvensjon.H73,
                    konvensjonsdato = LocalDate.now(),
                    ffuReferansenr = "Foo",
                ),
            )

            val oppdatertSak = bidragssakRepository.findByIdOrThrow(saksnummer.verdi)
            oppdatertSak.status shouldBe Bidragssakstatus.AK
            oppdatertSak.ansatt shouldBe true
            oppdatertSak.inhabilitet shouldBe true
            oppdatertSak.levdeAdskilt shouldBe true
            oppdatertSak.sanertDato shouldBe LocalDate.now()
            oppdatertSak.arbeidsfordeling shouldBe Arbeidsfordeling.BARNEBORTFØRING
            oppdatertSak.kategori shouldBe Sakskategori.UTLAND
            oppdatertSak.land shouldBe "SWE"
            oppdatertSak.konvensjon shouldBe Konvensjon.H73
            oppdatertSak.konvensjonsdato shouldBe LocalDate.now()
            oppdatertSak.ffuReferansenr shouldBe "Foo"
            oppdatertSak.roller shouldHaveSize 7
        }

        @Test
        @Disabled
        fun `skal lagre nytt barn med reell mottager`() {
            val nyttBarnFnr = genererPersonident()
            val rmFnr = genererPersonident()

            stubPerson(nyttBarnFnr, LocalDate.now().minusYears(18).minusDays(1)) // myndig -> må ha RM

            bidragssakService.oppdaterSak(
                OppdaterSakRequest(
                    saksnummer = saksnummer,
                    roller = setOf(rolleBarnMedRm(nyttBarnFnr, rm = rmFnr.verdi)),
                ),
            )

            val oppdatertSak = bidragssakRepository.findByIdOrThrow(saksnummer.verdi)
            oppdatertSak.roller shouldHaveSize 9

            oppdatertSak.roller
                .first { it.fødselsnummer == fnrBA1.verdi && it.rolleType == Rolletype.BARN }
                .rolleId shouldBe
                oppdatertSak.roller.first { it.fødselsnummer == fnrBA1.verdi && it.rolleType == Rolletype.REELMOTTAKER }.rolleId

            oppdatertSak.roller
                .first { it.fødselsnummer == fnrBA2.verdi && it.rolleType == Rolletype.BARN }
                .rolleId shouldBe
                oppdatertSak.roller.first { it.fødselsnummer == fnrBA2.verdi && it.rolleType == Rolletype.REELMOTTAKER }.rolleId

            oppdatertSak.roller
                .first { it.fødselsnummer == nyttBarnFnr.verdi && it.rolleType == Rolletype.BARN }
                .rolleId shouldBe
                oppdatertSak.roller.first { it.fødselsnummer == rmFnr.verdi && it.rolleType == Rolletype.REELMOTTAKER }.rolleId
        }

        @Test
        fun `skal lagre ny reell mottager`() {
            val rmFnr = genererPersonident()

            // BA3 er under 18 i setup, men nå legger vi til RM på BA3 -> OK
            bidragssakService.oppdaterSak(
                OppdaterSakRequest(
                    saksnummer = saksnummer,
                    roller = setOf(rolleBarnMedRm(fnrBA3, rm = rmFnr.verdi)),
                ),
            )

            th.transactional {
                val oppdatertSak = bidragssakRepository.findByIdOrThrow(saksnummer.verdi)

                oppdatertSak.roller
                    .first { it.fødselsnummer == fnrBA3.verdi && it.rolleType == Rolletype.BARN }
                    .rmRolleId shouldBe
                    oppdatertSak.roller.first { it.fødselsnummer == rmFnr.verdi && it.rolleType == Rolletype.REELMOTTAKER }.rolleId

                oppdatertSak.roller
                    .first { it.fødselsnummer == fnrBA3.verdi && it.rolleType == Rolletype.BARN }
                    .rollehistorikk
                    .first()
                    .typeEndring shouldBe
                    TypeEndring.SATT_NY_RM

                oppdatertSak.roller shouldHaveSize 8

                val rollehistorikkDto =
                    oppdatertSak.roller
                        .first { it.fødselsnummer == fnrBA3.verdi && it.rolleType == Rolletype.BARN }
                        .rollehistorikk
                        .toRollehistorikkDto(
                            oppdatertSak.roller,
                        )
                rollehistorikkDto.first().typeEndring shouldBe
                    TypeEndring.SATT_NY_RM
            }
        }

        @Test
        fun `skal ikke lagre duplikat mottager`() {
            // Oppdaterer bare BM (og rører ikke BA-rollene) -> skal ikke trigge validator på BA-rollene
            bidragssakService.oppdaterSak(
                OppdaterSakRequest(
                    saksnummer = saksnummer,
                    roller = setOf(RolleDto(fnrBM, Rolletype.BIDRAGSMOTTAKER)),
                ),
            )

            th.transactional {
                val oppdatertSak = bidragssakRepository.findByIdOrThrow(saksnummer.verdi)
                oppdatertSak.roller shouldHaveSize 7
            }
        }

        @Test
        fun `skal lagre endring av reell mottager`() {
            val rmFnr = genererPersonident()

            // BA1 er myndig -> fortsatt RM (bytter RM)
            bidragssakService.oppdaterSak(
                OppdaterSakRequest(
                    saksnummer = saksnummer,
                    roller = setOf(rolleBarnMedRm(fnrBA1, rm = rmFnr.verdi)),
                ),
            )

            th.transactional {
                val oppdatertSak = bidragssakRepository.findByIdOrThrow(saksnummer.verdi)
                oppdatertSak.roller shouldHaveSize 8
            }
        }

        @Test
        fun `skal lagre fjerning av reell mottager`() {
            bidragssakService.oppdaterSak(
                OppdaterSakRequest(
                    saksnummer = saksnummer,
                    roller = setOf(rolleBarnUtenRm(fnrBA3)),
                ),
            )

            th.transactional {
                val oppdatertSak = bidragssakRepository.findByIdOrThrow(saksnummer.verdi)
                oppdatertSak.roller shouldNotBe null
            }
        }

        @Test
        fun `skal takle internt bytte av reell mottager`() {
            bidragssakService.oppdaterSak(
                OppdaterSakRequest(
                    saksnummer = saksnummer,
                    roller =
                    setOf(
                        rolleBarnMedRm(fnrBA1, rm = genererPersonident().verdi),
                        rolleBarnMedRm(fnrBA2, rm = genererPersonident().verdi),
                    ),
                ),
            )

            th.transactional {
                val oppdatertSak = bidragssakRepository.findByIdOrThrow(saksnummer.verdi)
                oppdatertSak.roller shouldHaveSize 9
            }
        }

        @Test
        fun `skal lagre nytt barn med reell mottager som er samhandler`() {
            val nyttBarnFnr = genererPersonident()
            stubPerson(nyttBarnFnr, LocalDate.now().minusYears(18).minusDays(1)) // myndig -> må ha RM

            bidragssakService.oppdaterSak(
                OppdaterSakRequest(
                    saksnummer = saksnummerMedSamhandlere,
                    roller = setOf(rolleBarnMedRm(nyttBarnFnr, rm = "85000000083")),
                ),
            )

            th.transactional {
                val oppdatertSak = bidragssakRepository.findByIdOrThrow(saksnummerMedSamhandlere.verdi)
                // Opprinnelig i setup: BM, BP, BA1 + RM, BA2 + RM, BA3 + RM = 8 roller, så legger vi til ett barn med RM = 10 roller
                oppdatertSak.roller shouldHaveSize 10
            }
        }

        @Test
        fun `skal lagre endring av reell mottager som er samhandler`() {
            // BA2 er myndig -> fortsatt RM
            bidragssakService.oppdaterSak(
                OppdaterSakRequest(
                    saksnummer = saksnummerMedSamhandlere,
                    roller = setOf(rolleBarnMedRm(fnrBA2, rm = "85000000083")),
                ),
            )

            th.transactional {
                val oppdatertSak = bidragssakRepository.findByIdOrThrow(saksnummerMedSamhandlere.verdi)
                // Opprinnelig i setup: BM, BP, BA1 + RM, BA2 + RM, BA3 + RM = 8 roller, men BA2 sin RM endres så en ny RM-rolle lages, til sammen 9 roller
                oppdatertSak.roller shouldHaveSize 9
            }
        }

        @Test
        fun `skal lagre fjerning av reell mottager som er samhandler`() {
            bidragssakService.oppdaterSak(
                OppdaterSakRequest(
                    saksnummer = saksnummerMedSamhandlere,
                    roller = setOf(rolleBarnUtenRm(fnrBA3)),
                ),
            )

            th.transactional {
                val oppdatertSak = bidragssakRepository.findByIdOrThrow(saksnummerMedSamhandlere.verdi)
                oppdatertSak.roller shouldNotBe null
            }
        }

        @Test
        fun `skal takle internt bytte av reell mottager som er samhandler`() {
            // BA2 myndig -> RM ok, BA3 under 18 -> RM ok (her lar vi begge ha RM)
            bidragssakService.oppdaterSak(
                OppdaterSakRequest(
                    saksnummer = saksnummerMedSamhandlere,
                    roller =
                    setOf(
                        rolleBarnMedRm(fnrBA2, rm = "80000008111"),
                        rolleBarnMedRm(fnrBA3, rm = "85000000083"),
                    ),
                ),
            )

            th.transactional {
                val oppdatertSak = bidragssakRepository.findByIdOrThrow(saksnummerMedSamhandlere.verdi)
                // Opprinnelig i setup: BM, BP, BA1 + RM, BA2 + RM, BA3 + RM = 8 roller, men BA2 og BA3 sine RM endres så to nye RM-roller lages, til sammen 10 roller
                oppdatertSak.roller shouldHaveSize 10
            }
        }

        @Test
        fun `skal sette eierfogd til det som returneres fra hentEnhetForArbeidsfordelingGeografiskTilknytning`() {
            bidragssakService.oppdaterSak(
                OppdaterSakRequest(
                    saksnummer = saksnummer,
                    kategorikode = Sakskategori.UTLAND,
                    landkode = Landkode("SWE"),
                    konvensjonskode = Konvensjon.H73,
                    konvensjonsdato = LocalDate.now(),
                    ffuReferansenr = "Foo",
                ),
            )

            val oppdatertSak = bidragssakRepository.findByIdOrThrow(saksnummer.verdi)
            val sisteEierfogd = oppdatertSak.tilganger.filter { it.tilgangTomDato == null }
            sisteEierfogd.size shouldBe 1
            sisteEierfogd.first().enhetsnummer shouldBe BidragOrganisasjonTestConfig.EIERFOGD_UTLAND
            oppdatertSak.eierfogd shouldBe BidragOrganisasjonTestConfig.EIERFOGD_UTLAND
        }
    }

    @Nested
    inner class OpprettSak {
        @BeforeEach
        fun setUp() {
            clearMocks(identConsumer)
            every { identConsumer.hentAlleIdenter(any()) }.answers { listOf(firstArg()) }
        }

        @Test
        fun `oppretter sak basert på OpprettSakRequest med korrekte felter`() {
            val fnr1 = genererPersonident()
            val fnr2 = genererPersonident()
            val ba = genererPersonident()

            stubPerson(fnr1, LocalDate.now().minusYears(30))
            stubPerson(fnr2, LocalDate.now().minusYears(30))
            stubPerson(ba, LocalDate.now().minusYears(3))

            val opprettSak =
                bidragssakService.opprettSak(
                    OpprettSakRequest(
                        eierfogd = Enhetsnummer("1701"),
                        kategori = Sakskategori.NASJONAL,
                        konvensjon = Konvensjon.NI,
                        roller =
                        setOf(
                            RolleDto(fnr1, Rolletype.BIDRAGSMOTTAKER),
                            RolleDto(fnr2, Rolletype.BIDRAGSPLIKTIG),
                            RolleDto(ba, Rolletype.BARN),
                        ),
                    ),
                )

            opprettSak shouldNotBe null
            val nySakFraBase = bidragssakRepository.findByIdOrThrow(opprettSak.saksnummer.verdi)
            nySakFraBase.roller shouldExist { it.fødselsnummer == fnr1.verdi && it.rolleType == Rolletype.BIDRAGSMOTTAKER }
            nySakFraBase.roller shouldExist { it.fødselsnummer == fnr2.verdi && it.rolleType == Rolletype.BIDRAGSPLIKTIG }
        }
    }

    private fun stubPerson(
        fnr: Personident,
        fødselsdato: LocalDate,
    ) {
        every { identConsumer.hentPersonInformasjon(match { it.verdi == fnr.verdi }) } returns
            PersonDto(ident = fnr, fødselsdato = fødselsdato)
    }

    private fun rolleBarnMedRm(
        fnr: Personident = genererPersonident(),
        rm: String = "80060878901",
    ) = RolleDto(
        type = Rolletype.BARN,
        fødselsnummer = fnr,
        // gammel + ny (du har begge i DTO-en og harRM() støtter begge)
        reellMottager = ReellMottaker(rm),
        reellMottaker = ReellMottakerDto(ident = ReellMottaker(rm), verge = false),
    )

    private fun rolleBarnUtenRm(fnr: Personident = genererPersonident()) = RolleDto(type = Rolletype.BARN, fødselsnummer = fnr)
}
