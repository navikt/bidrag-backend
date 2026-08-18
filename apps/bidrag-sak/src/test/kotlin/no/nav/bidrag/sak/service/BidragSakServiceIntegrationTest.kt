package no.nav.bidrag.sak.service

import io.kotest.matchers.collections.shouldExist
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.every
import no.nav.bidrag.domene.enums.behandling.HendelseType
import no.nav.bidrag.domene.enums.behandling.SøknadGruppeKombinasjon
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.rolle.TypeEndring
import no.nav.bidrag.domene.enums.sak.Arbeidsfordeling
import no.nav.bidrag.domene.enums.sak.Bidragssakstatus
import no.nav.bidrag.domene.enums.sak.Fogdårsak
import no.nav.bidrag.domene.enums.sak.Konvensjon
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.enums.sak.Tilgangstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.ident.ReellMottaker
import no.nav.bidrag.domene.land.Landkode
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.generer.testdata.person.genererPersonident
import no.nav.bidrag.sak.SpringH2TestRunner
import no.nav.bidrag.sak.config.BidragOrganisasjonTestConfig
import no.nav.bidrag.sak.domain.Hendelse
import no.nav.bidrag.sak.mapper.RollehistorikkMapper.toRollehistorikkDto
import no.nav.bidrag.sak.repository.BidragssakRepository
import no.nav.bidrag.sak.repository.HendelseRepository
import no.nav.bidrag.sak.repository.findByIdOrThrow
import no.nav.bidrag.sak.util.TransactionHelper
import no.nav.bidrag.transport.person.PersonDto
import no.nav.bidrag.transport.sak.OppdaterSakRequest
import no.nav.bidrag.transport.sak.OpprettMidlertidligTilgangRequest
import no.nav.bidrag.transport.sak.OpprettSakRequest
import no.nav.bidrag.transport.sak.ReellMottakerDto
import no.nav.bidrag.transport.sak.RolleDto
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BidragSakServiceIntegrationTest : SpringH2TestRunner() {
    @Autowired
    private lateinit var bidragssakRepository: BidragssakRepository

    @Autowired
    private lateinit var hendelseRepository: HendelseRepository

    @Autowired
    private lateinit var bidragssakService: BidragSakService

    @Autowired
    private lateinit var tilgangClientMock: Tilgangskontroll

    @Autowired
    private lateinit var th: TransactionHelper

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
        fun `skal lagre nytt barn med reell mottaker`() {
            val nyttBarnFnr = genererPersonident()
            val rmFnr = genererPersonident()

            stubPerson(nyttBarnFnr, LocalDate.now().minusYears(18).minusDays(1)) // myndig -> må ha RM

            bidragssakService.oppdaterSak(
                OppdaterSakRequest(
                    saksnummer = saksnummer,
                    roller = setOf(rolleBarnMedRm(nyttBarnFnr, rm = rmFnr.verdi)),
                ),
            )

            th.transactional {
                val oppdatertSak = bidragssakRepository.findByIdOrThrow(saksnummer.verdi)
                oppdatertSak.roller shouldHaveSize 9

                val sortertRollehistorikkBarn1 =
                    oppdatertSak.roller
                        .first {
                            it.fødselsnummer == fnrBA1.verdi && it.rolleType == Rolletype.BARN
                        }.rollehistorikk
                        .sortedBy { it.opprettetTidspunkt }

                val sortertRollehistorikkBarn2 =
                    oppdatertSak.roller
                        .first {
                            it.fødselsnummer == fnrBA2.verdi && it.rolleType == Rolletype.BARN
                        }.rollehistorikk
                        .sortedBy { it.opprettetTidspunkt }

                val sortertRollehistorikkNyttBarn =
                    oppdatertSak.roller
                        .first {
                            it.fødselsnummer == nyttBarnFnr.verdi && it.rolleType == Rolletype.BARN
                        }.rollehistorikk
                        .sortedBy { it.opprettetTidspunkt }

                sortertRollehistorikkBarn1[0].rmRolleId shouldBe
                    oppdatertSak.roller.first { it.fødselsnummer == fnrBA1.verdi && it.rolleType == Rolletype.REELMOTTAKER }.rolleId

                sortertRollehistorikkBarn2[0]
                    .rmRolleId shouldBe
                    oppdatertSak.roller.first { it.fødselsnummer == fnrBA2.verdi && it.rolleType == Rolletype.REELMOTTAKER }.rolleId

                sortertRollehistorikkNyttBarn[0]
                    .rmRolleId shouldBe
                    oppdatertSak.roller.first { it.fødselsnummer == rmFnr.verdi && it.rolleType == Rolletype.REELMOTTAKER }.rolleId

                sortertRollehistorikkNyttBarn[0].rolle?.rolleId shouldBe
                    oppdatertSak.roller.first { it.fødselsnummer == nyttBarnFnr.verdi && it.rolleType == Rolletype.BARN }.rolleId

                sortertRollehistorikkNyttBarn[0].rmRolleId shouldBe
                    oppdatertSak.roller.first { it.fødselsnummer == rmFnr.verdi && it.rolleType == Rolletype.REELMOTTAKER }.rolleId

                sortertRollehistorikkNyttBarn[0].rmRolleFødselsnummer shouldBe
                    oppdatertSak.roller.first { it.fødselsnummer == rmFnr.verdi && it.rolleType == Rolletype.REELMOTTAKER }.fødselsnummer
            }
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
        fun `skal ikke lagre duplikat mottaker`() {
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

                val sortertRollehistorikk =
                    oppdatertSak.roller
                        .first {
                            it.fødselsnummer == fnrBA1.verdi && it.rolleType == Rolletype.BARN
                        }.rollehistorikk
                        .sortedBy { it.opprettetTidspunkt }

                sortertRollehistorikk[1]
                    .rmRolleId shouldBe
                    oppdatertSak.roller.filter { it.fødselsnummer == rmFnr.verdi && it.rolleType == Rolletype.REELMOTTAKER }[0].rolleId

                sortertRollehistorikk[1].typeEndring shouldBe TypeEndring.SATT_RM

                oppdatertSak.roller shouldHaveSize 8
            }
        }

        @Test
        fun `skal lagre fjerning av reell mottager`() {
            bidragssakService.oppdaterSak(
                OppdaterSakRequest(
                    saksnummer = saksnummer,
                    roller = setOf(rolleBarnMedRm(fnrBA3)),
                ),
            )

            th.transactional {
                bidragssakRepository.findByIdOrThrow(saksnummer.verdi)
            }

            bidragssakService.oppdaterSak(
                OppdaterSakRequest(
                    saksnummer = saksnummer,
                    roller = setOf(rolleBarnUtenRm(fnrBA3)),
                ),
            )

            th.transactional {
                val oppdatertSak = bidragssakRepository.findByIdOrThrow(saksnummer.verdi)
                oppdatertSak.roller.first { it.fødselsnummer == fnrBA3.verdi && it.rolleType == Rolletype.BARN }.rmRolleId shouldBe null

                val sortertRollehistorikk =
                    oppdatertSak.roller
                        .first {
                            it.fødselsnummer == fnrBA3.verdi && it.rolleType == Rolletype.BARN
                        }.rollehistorikk
                        .sortedBy { it.opprettetTidspunkt }

                sortertRollehistorikk[0].rmRolleId shouldNotBe null
                sortertRollehistorikk[1].rmRolleId shouldBe null
                sortertRollehistorikk[0].typeEndring shouldBe TypeEndring.SATT_NY_RM
                sortertRollehistorikk[1].typeEndring shouldBe TypeEndring.SATT_TIL_BM
            }
        }

        @Test
        fun `skal takle internt bytte av reell mottager`() {
            val rm1 = genererPersonident().verdi
            val rm2 = genererPersonident().verdi
            bidragssakService.oppdaterSak(
                OppdaterSakRequest(
                    saksnummer = saksnummer,
                    roller =
                    setOf(
                        rolleBarnMedRm(fnrBA1, rm = rm1),
                        rolleBarnMedRm(fnrBA2, rm = rm2),
                    ),
                ),
            )

            th.transactional {
                val oppdatertSak = bidragssakRepository.findByIdOrThrow(saksnummer.verdi)
                oppdatertSak.roller shouldHaveSize 9

                val sortertRollehistorikkBarn1 =
                    oppdatertSak.roller
                        .first {
                            it.fødselsnummer == fnrBA1.verdi && it.rolleType == Rolletype.BARN
                        }.rollehistorikk
                        .sortedBy { it.opprettetTidspunkt }

                val sortertRollehistorikkBarn2 =
                    oppdatertSak.roller
                        .first {
                            it.fødselsnummer == fnrBA2.verdi && it.rolleType == Rolletype.BARN
                        }.rollehistorikk
                        .sortedBy { it.opprettetTidspunkt }

                sortertRollehistorikkBarn1[1]
                    .rmRolleId shouldBe
                    oppdatertSak.roller.filter { it.fødselsnummer.toString() == rm1 && it.rolleType == Rolletype.REELMOTTAKER }[0].rolleId

                sortertRollehistorikkBarn1[1].typeEndring shouldBe TypeEndring.SATT_RM

                sortertRollehistorikkBarn2[1]
                    .rmRolleId shouldBe
                    oppdatertSak.roller.filter { it.fødselsnummer.toString() == rm2 && it.rolleType == Rolletype.REELMOTTAKER }[0].rolleId

                sortertRollehistorikkBarn2[1].typeEndring shouldBe TypeEndring.SATT_RM
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

            th.transactional {
                val oppdatertSak = bidragssakRepository.findByIdOrThrow(saksnummer.verdi)
                val sisteEierfogd = oppdatertSak.tilganger.filter { it.tilgangTomDato == null }
                sisteEierfogd.size shouldBe 1
                sisteEierfogd.first().enhetsnummer shouldBe BidragOrganisasjonTestConfig.EIERFOGD_UTLAND
                oppdatertSak.eierfogd shouldBe BidragOrganisasjonTestConfig.EIERFOGD_UTLAND
            }
        }

        @Test
        fun `skal ikke lagre ny forekomst rollehistorikk hvis rm er uendret`() {
            // Rollen oppdateres med samme RM som ligger på barnet fra før fra initiell opprettelse av sak
            bidragssakService.oppdaterSak(
                OppdaterSakRequest(
                    saksnummer = saksnummer,
                    roller =
                    setOf(
                        RolleDto(
                            type = Rolletype.BARN,
                            objektnummer = "04",
                            fødselsnummer = fnrBA1,
                            foedselsnummer = fnrBA1,
                            reellMottager = null,
                            reellMottaker = ReellMottakerDto(ident = ReellMottaker("85000000076"), verge = false),
                            rollehistorikk = emptyList(),
                        ),
                        RolleDto(
                            type = Rolletype.BARN,
                            objektnummer = "05",
                            fødselsnummer = fnrBA2,
                            foedselsnummer = fnrBA2,
                            reellMottager = null,
                            reellMottaker = ReellMottakerDto(ident = ReellMottaker(fnrBA2.verdi), verge = false),
                            rollehistorikk = emptyList(),
                        ),
                    ),
                ),
            )

            bidragssakService.oppdaterSak(
                OppdaterSakRequest(
                    saksnummer = saksnummer,
                    roller =
                    setOf(
                        RolleDto(
                            type = Rolletype.BARN,
                            objektnummer = "04",
                            fødselsnummer = fnrBA1,
                            foedselsnummer = fnrBA1,
                            reellMottager = null,
                            reellMottaker = ReellMottakerDto(ident = ReellMottaker("85000000076"), verge = false),
                            rollehistorikk = emptyList(),
                        ),
                        RolleDto(
                            type = Rolletype.BARN,
                            objektnummer = "05",
                            fødselsnummer = fnrBA2,
                            foedselsnummer = fnrBA2,
                            reellMottager = null,
                            reellMottaker = ReellMottakerDto(ident = ReellMottaker(fnrBA2.verdi), verge = false),
                            rollehistorikk = emptyList(),
                        ),
                    ),
                ),
            )

            th.transactional {
                val oppdatertSak = bidragssakRepository.findByIdOrThrow(saksnummer.verdi)

                val sortertRollehistorikk1 =
                    oppdatertSak.roller
                        .first {
                            it.fødselsnummer == fnrBA1.verdi && it.rolleType == Rolletype.BARN
                        }.rollehistorikk
                        .sortedBy { it.opprettetTidspunkt }

                val sortertRollehistorikk2 =
                    oppdatertSak.roller
                        .first {
                            it.fødselsnummer == fnrBA2.verdi && it.rolleType == Rolletype.BARN
                        }.rollehistorikk
                        .sortedBy { it.opprettetTidspunkt }

                oppdatertSak.roller
                    .filter {
                        it.fødselsnummer == fnrBA1.verdi && it.rolleType == Rolletype.BARN
                    } shouldHaveSize 1

                sortertRollehistorikk1 shouldHaveSize 2
                sortertRollehistorikk2 shouldHaveSize 1

                sortertRollehistorikk1[0]
                    .rmRolleId shouldBe
                    oppdatertSak.roller.filter { it.fødselsnummer == fnrBA1.verdi && it.rolleType == Rolletype.REELMOTTAKER }[0].rolleId

                sortertRollehistorikk1[0].typeEndring shouldBe TypeEndring.SATT_NY_RM

                oppdatertSak.roller shouldHaveSize 8
            }
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
            val bp = genererPersonident()
            val bm = genererPersonident()
            val ba = genererPersonident()

            stubPerson(bp, LocalDate.now().minusYears(30))
            stubPerson(bm, LocalDate.now().minusYears(30))
            stubPerson(ba, LocalDate.now().minusYears(3))

            val opprettSak =
                bidragssakService.opprettSak(
                    OpprettSakRequest(
                        eierfogd = Enhetsnummer("1701"),
                        kategori = Sakskategori.NASJONAL,
                        konvensjon = Konvensjon.NI,
                        roller =
                        setOf(
                            RolleDto(bp, Rolletype.BIDRAGSMOTTAKER),
                            RolleDto(bm, Rolletype.BIDRAGSPLIKTIG),
                            RolleDto(ba, Rolletype.BARN),
                        ),
                    ),
                )

            opprettSak shouldNotBe null
            val nySakFraBase = bidragssakRepository.findByIdOrThrow(opprettSak.saksnummer.verdi)
            nySakFraBase.roller shouldExist { it.fødselsnummer == bp.verdi && it.rolleType == Rolletype.BIDRAGSMOTTAKER }
            nySakFraBase.roller shouldExist { it.fødselsnummer == bm.verdi && it.rolleType == Rolletype.BIDRAGSPLIKTIG }
        }

        @Test
        fun `test på at det ikke opprettes ny sak hvis matchende sak allerede finnes på roller`() {
            val bp = genererPersonident()
            val bm1 = genererPersonident()
            val bm2 = genererPersonident()
            val ba = genererPersonident()

            stubPerson(bp, LocalDate.now().minusYears(30))
            stubPerson(bm1, LocalDate.now().minusYears(30))
            stubPerson(bm2, LocalDate.now().minusYears(30))
            stubPerson(ba, LocalDate.now().minusYears(3))

            val eksisterendeSak =
                bidragssakService.opprettSak(
                    OpprettSakRequest(
                        eierfogd = Enhetsnummer("1701"),
                        kategori = Sakskategori.NASJONAL,
                        konvensjon = Konvensjon.NI,
                        roller =
                        setOf(
                            RolleDto(bm1, Rolletype.BIDRAGSMOTTAKER),
                            RolleDto(bp, Rolletype.BIDRAGSPLIKTIG),
                            RolleDto(ba, Rolletype.BARN),
                        ),
                    ),
                )

            bidragssakService.opprettSak(
                OpprettSakRequest(
                    eierfogd = Enhetsnummer("1801"),
                    kategori = Sakskategori.NASJONAL,
                    konvensjon = Konvensjon.NI,
                    roller =
                    setOf(
                        RolleDto(bm2, Rolletype.BIDRAGSMOTTAKER),
                        RolleDto(bp, Rolletype.BIDRAGSPLIKTIG),
                        RolleDto(ba, Rolletype.BARN),
                    ),
                ),
            )

            val nyopprettetSak =
                bidragssakService.opprettSak(
                    OpprettSakRequest(
                        eierfogd = Enhetsnummer("1702"),
                        kategori = Sakskategori.NASJONAL,
                        konvensjon = Konvensjon.H73,
                        roller =
                        setOf(
                            RolleDto(bm1, Rolletype.BIDRAGSMOTTAKER),
                            RolleDto(bp, Rolletype.BIDRAGSPLIKTIG),
                            RolleDto(ba, Rolletype.BARN),
                        ),
                    ),
                )

            // Det er ikke opprettet en ny sak, saksnummer til eksisterende sak returneres.
            nyopprettetSak.saksnummer shouldBe eksisterendeSak.saksnummer
            val nySakFraBase = bidragssakRepository.findByIdOrThrow(eksisterendeSak.saksnummer.verdi)
            nySakFraBase.roller shouldExist { it.fødselsnummer == bm1.verdi && it.rolleType == Rolletype.BIDRAGSMOTTAKER }
            nySakFraBase.roller shouldExist { it.fødselsnummer == bp.verdi && it.rolleType == Rolletype.BIDRAGSPLIKTIG }

            val bPsSaker = bidragssakRepository.findByRoller(listOf(bp.verdi))

            bPsSaker shouldHaveSize 2
        }

        @Test
        fun `test på at det opprettes ny sak hvis eksisterende sak er uten BM`() {
            val bp = genererPersonident()
            val ba = genererPersonident()

            stubPerson(bp, LocalDate.now().minusYears(30))
            stubPerson(ba, LocalDate.now().minusYears(3))

            val eksisterendeSak =
                bidragssakService.opprettSak(
                    OpprettSakRequest(
                        eierfogd = Enhetsnummer("1701"),
                        kategori = Sakskategori.NASJONAL,
                        konvensjon = Konvensjon.NI,
                        roller =
                        setOf(
                            RolleDto(type = Rolletype.BIDRAGSMOTTAKER),
                            RolleDto(bp, Rolletype.BIDRAGSPLIKTIG),
                            RolleDto(ba, Rolletype.BARN),
                        ),
                    ),
                )

            val bm1 = genererPersonident()
            stubPerson(bm1, LocalDate.now().minusYears(30))

            val nyopprettetSak =
                bidragssakService.opprettSak(
                    OpprettSakRequest(
                        eierfogd = Enhetsnummer("1702"),
                        kategori = Sakskategori.NASJONAL,
                        konvensjon = Konvensjon.H73,
                        roller =
                        setOf(
                            RolleDto(type = Rolletype.BIDRAGSMOTTAKER),
                            RolleDto(bp, Rolletype.BIDRAGSPLIKTIG),
                            RolleDto(ba, Rolletype.BARN),
                        ),
                    ),
                )

            // Det er ikke opprettet en ny sak, saksnummer til eksisterende sak returneres.
            nyopprettetSak.saksnummer shouldNotBe eksisterendeSak.saksnummer
            val nySakFraBase = bidragssakRepository.findByIdOrThrow(eksisterendeSak.saksnummer.verdi)
            nySakFraBase.roller shouldExist { it.fødselsnummer == bp.verdi && it.rolleType == Rolletype.BIDRAGSPLIKTIG }

            val bPsSaker = bidragssakRepository.findByRoller(listOf(bp.verdi))

            bPsSaker shouldHaveSize 2
        }
    }

    @Nested
    inner class Fogdhistorikk {
        @BeforeEach
        fun setUp() {
            clearMocks(identConsumer)
            every { identConsumer.hentAlleIdenter(any()) }.answers { listOf(firstArg()) }
            every { identConsumer.hentPersonInformasjon(any()) } returns null
        }

        @Test
        fun `skal returnere fogdhistorikk for sak med eierfogd-tilgang`() {
            val bp = genererPersonident()
            val bm = genererPersonident()

            stubPerson(bp, LocalDate.now().minusYears(30))
            stubPerson(bm, LocalDate.now().minusYears(30))

            val opprettetSak =
                bidragssakService.opprettSak(
                    OpprettSakRequest(
                        eierfogd = Enhetsnummer("1701"),
                        roller =
                        setOf(
                            RolleDto(bp, Rolletype.BIDRAGSMOTTAKER),
                            RolleDto(bm, Rolletype.BIDRAGSPLIKTIG),
                        ),
                    ),
                )

            val fogdhistorikk = bidragssakService.finnFogdhistorikk(opprettetSak.saksnummer)

            fogdhistorikk shouldHaveSize 1
            fogdhistorikk[0].enhetsnummer shouldBe "1701"
            fogdhistorikk[0].tilgangFomDato shouldBe LocalDate.now()
            fogdhistorikk[0].tilgangTomDato shouldBe null
            fogdhistorikk[0].arsak shouldBe Fogdårsak.EIER
            fogdhistorikk[0].type shouldBe Tilgangstype.EIER
        }

        @Test
        fun `skal returnere tom liste for sak som ikke finnes`() {
            val fogdhistorikk = bidragssakService.finnFogdhistorikk(Saksnummer("9999999"))

            fogdhistorikk shouldHaveSize 0
        }

        @Test
        fun `skal returnere alle tilganger inkludert midlertidlig tilgang`() {
            val bp = genererPersonident()
            val bm = genererPersonident()

            stubPerson(bp, LocalDate.now().minusYears(30))
            stubPerson(bm, LocalDate.now().minusYears(30))

            val opprettetSak =
                bidragssakService.opprettSak(
                    OpprettSakRequest(
                        eierfogd = Enhetsnummer("1701"),
                        roller =
                        setOf(
                            RolleDto(bp, Rolletype.BIDRAGSMOTTAKER),
                            RolleDto(bm, Rolletype.BIDRAGSPLIKTIG),
                        ),
                    ),
                )

            bidragssakService.opprettEllerUtvidMidlertidligTilgangSak(
                OpprettMidlertidligTilgangRequest(
                    saksnummer = opprettetSak.saksnummer.verdi,
                    enhet = "4806",
                    tilgangTilOgMedDato = LocalDate.now().plusMonths(3),
                ),
            )

            val fogdhistorikk = bidragssakService.finnFogdhistorikk(opprettetSak.saksnummer)

            fogdhistorikk shouldHaveSize 2
            fogdhistorikk shouldExist { it.enhetsnummer == "1701" && it.arsak == Fogdårsak.EIER && it.type == Tilgangstype.EIER }
            fogdhistorikk shouldExist {
                it.enhetsnummer == "4806" &&
                    it.arsak == Fogdårsak.MAKO &&
                    it.type == Tilgangstype.MIDL &&
                    it.tilgangTomDato == LocalDate.now().plusMonths(3)
            }
        }

        @Test
        fun `skal returnere tom liste om saksbehanler ikke har tilgang`() {
            val bp = genererPersonident()
            val bm = genererPersonident()

            stubPerson(bp, LocalDate.now().minusYears(30))
            stubPerson(bm, LocalDate.now().minusYears(30))

            val opprettetSak =
                bidragssakService.opprettSak(
                    OpprettSakRequest(
                        eierfogd = Enhetsnummer("1701"),
                        roller =
                        setOf(
                            RolleDto(bp, Rolletype.BIDRAGSMOTTAKER),
                            RolleDto(bm, Rolletype.BIDRAGSPLIKTIG),
                        ),
                    ),
                )
            every { tilgangClientMock.harTilgangSaksnummer(opprettetSak.saksnummer) }
                .returns(false)

            val fogdhistorikk = bidragssakService.finnFogdhistorikk(opprettetSak.saksnummer)

            fogdhistorikk shouldHaveSize 0
        }
    }

    @Nested
    inner class FinnHendelserForSak {
        private val saksnummer = Saksnummer("9999-000001")
        private val begrensetSaksnummer = Saksnummer("9999-000099")

        @BeforeEach
        fun setUp() {
            clearMocks(identConsumer)
            every { identConsumer.hentAlleIdenter(any()) }.answers { listOf(firstArg()) }
            every { identConsumer.hentPersonInformasjon(any()) } returns null
        }

        @AfterEach
        fun tearDown() {
            hendelseRepository.deleteAll()
        }

        @Test
        fun `skal returnere mappede hendelser for saksnummer`() {
            hendelseRepository.save(
                Hendelse(saksnummer = saksnummer.verdi, type = HendelseType.VEDTAK, enhet = "1701", søknad = null),
            )
            hendelseRepository.save(
                Hendelse(
                    saksnummer = saksnummer.verdi,
                    type = HendelseType.BRUKERSTØTTE,
                    enhet = "4806",
                    grKombKode = SøknadGruppeKombinasjon.FORSKUDD.kode,
                    søknad = null,
                ),
            )

            val result = bidragssakService.finnHendelserForSak(saksnummer)

            result shouldHaveSize 2
            result shouldExist { it.link == "VEDTAK" && it.enhet.verdi == "1701" }
            result shouldExist {
                it.link == SøknadGruppeKombinasjon.FORSKUDD.kode &&
                    it.søknadsgruppe == SøknadGruppeKombinasjon.FORSKUDD &&
                    it.enhet.verdi == "4806"
            }
            result.forEach {
                it.erLukket shouldBe true
                it.erBisysVedtakOgErOverført shouldBe false
            }
            // VEDT-hendelsen har vedtakslenke og vedtaks-hendelsestype → resultatIBisys = true
            result.first { it.link == "VEDTAK" }.resultatIBisys shouldBe true
            // FORSKUDD-hendelsen har verken BBM-resultat eller vedtakslenke → resultatIBisys = false
            result.first { it.link == SøknadGruppeKombinasjon.FORSKUDD.kode }.resultatIBisys shouldBe false
        }

        @Test
        fun `resultatIBisys skal være true når fraBbm er true, resultat er satt og link ikke er null`() {
            // NB: erVedtak=false når fraBbm=true+resultat!=null, så grKombKode brukes for å gi link != null
            hendelseRepository.save(
                Hendelse(
                    saksnummer = saksnummer.verdi,
                    type = HendelseType.BRUKERSTØTTE,
                    enhet = "1701",
                    resultat = "INNVILGET",
                    fraBbm = true,
                    grKombKode = SøknadGruppeKombinasjon.FORSKUDD.kode,
                    søknad = null,
                ),
            )

            val result = bidragssakService.finnHendelserForSak(saksnummer)

            result.first().resultatIBisys shouldBe true
            result.first().link shouldBe SøknadGruppeKombinasjon.FORSKUDD.kode
            result.first().erBisysVedtakOgErOverført shouldBe false
        }

        @Test
        fun `skal returnere tom liste om saksbehandler ikke har tilgang`() {
            hendelseRepository.save(
                Hendelse(saksnummer = begrensetSaksnummer.verdi, type = HendelseType.BRUKERSTØTTE, enhet = "1701", søknad = null),
            )
            every { tilgangClientMock.harTilgangSaksnummer(begrensetSaksnummer) } returns false

            val result = bidragssakService.finnHendelserForSak(begrensetSaksnummer)

            result shouldHaveSize 0
        }

        @Test
        fun `skal returnere tom liste for sak uten hendelser`() {
            val result = bidragssakService.finnHendelserForSak(saksnummer)

            result shouldHaveSize 0
        }

        @Test
        fun `vedtak fra BBM med resultat skal ikke få link VEDTAK`() {
            hendelseRepository.save(
                Hendelse(
                    saksnummer = saksnummer.verdi,
                    type = HendelseType.VEDTAK,
                    enhet = "1701",
                    resultat = "INNVILGET",
                    fraBbm = true,
                    søknad = null,
                ),
            )

            val result = bidragssakService.finnHendelserForSak(saksnummer)

            result.first().link shouldBe null
            // fraBbm=true og resultat satt, men link=null → resultatIBisys=false
            result.first().resultatIBisys shouldBe false
            result.first().erBisysVedtakOgErOverført shouldBe false
        }

        @Test
        fun `skal mappe alle SøknadGruppeKombinasjon-lenker korrekt`() {
            listOf(
                SøknadGruppeKombinasjon.FORSKUDD to SøknadGruppeKombinasjon.FORSKUDD.kode,
                SøknadGruppeKombinasjon.SÆRBIDRAG to SøknadGruppeKombinasjon.SÆRBIDRAG.kode,
                SøknadGruppeKombinasjon.BIDRAG to SøknadGruppeKombinasjon.BIDRAG.kode,
            ).forEach { (kombinasjon, forventetLink) ->
                hendelseRepository.save(
                    Hendelse(
                        saksnummer = saksnummer.verdi,
                        type = HendelseType.BRUKERSTØTTE,
                        enhet = "1701",
                        grKombKode = kombinasjon.kode,
                        søknad = null,
                    ),
                )

                val result = bidragssakService.finnHendelserForSak(saksnummer)

                result shouldExist { it.link == forventetLink && it.søknadsgruppe == kombinasjon }

                hendelseRepository.deleteAll()
            }
        }

        @Test
        fun `skal sette link null for hendelse som ikke matcher noen kategori`() {
            hendelseRepository.save(
                Hendelse(saksnummer = saksnummer.verdi, type = HendelseType.BRUKERSTØTTE, enhet = "1701", søknad = null),
            )

            val result = bidragssakService.finnHendelserForSak(saksnummer)

            result.first().link shouldBe null
            result.first().søknadsgruppe shouldBe null
            result.first().erLukket shouldBe true
            result.first().resultatIBisys shouldBe false
            result.first().erBisysVedtakOgErOverført shouldBe false
        }

        @Test
        fun `skal kun returnere hendelser for riktig saksnummer`() {
            val annetSaksnummer = "9999-000002"
            hendelseRepository.save(
                Hendelse(saksnummer = saksnummer.verdi, type = HendelseType.BRUKERSTØTTE, enhet = "1701", søknad = null),
            )
            hendelseRepository.save(
                Hendelse(saksnummer = annetSaksnummer, type = HendelseType.BRUKERSTØTTE, enhet = "1701", søknad = null),
            )

            val result = bidragssakService.finnHendelserForSak(saksnummer)

            result shouldHaveSize 1
        }

        @Test
        fun `skal mappe opprettetTidspunkt og resultat korrekt`() {
            val tidspunkt = LocalDateTime.of(2026, 3, 15, 12, 30)
            hendelseRepository.save(
                Hendelse(
                    saksnummer = saksnummer.verdi,
                    type = HendelseType.BRUKERSTØTTE,
                    enhet = "4806",
                    opprettetTidspunkt = tidspunkt,
                    resultat = "AVSLÅTT",
                    søknad = null,
                ),
            )

            val result = bidragssakService.finnHendelserForSak(saksnummer)

            result.first().opprettetTidspunkt shouldBe tidspunkt
            result.first().enhet.verdi shouldBe "4806"
            result.first().type shouldBe HendelseType.BRUKERSTØTTE
            result.first().resultat shouldBe "AVSLÅTT"
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
