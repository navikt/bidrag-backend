package no.nav.bidrag.sak.validering

import io.kotest.assertions.throwables.shouldThrowMessage
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.bidrag.commons.unleash.UnleashFeaturesProvider
import no.nav.bidrag.commons.util.IdentConsumer
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.sak.Arbeidsfordeling
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.ident.ReellMottaker
import no.nav.bidrag.domene.land.Landkode
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.generer.testdata.person.genererPersonident
import no.nav.bidrag.sak.config.UnleashFeatures
import no.nav.bidrag.sak.domain.Rolle
import no.nav.bidrag.sak.integration.kodeverk.CachedKodeverkService
import no.nav.bidrag.sak.integration.person.BidragPersonClient
import no.nav.bidrag.sak.util.FnrGenerator
import no.nav.bidrag.transport.person.PersonDto
import no.nav.bidrag.transport.sak.OpprettSakRequest
import no.nav.bidrag.transport.sak.ReellMottakerDto
import no.nav.bidrag.transport.sak.RolleDto
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BidragssakValidatorTest {
    private val identConsumer: IdentConsumer = mockk(relaxed = true)
    private val cachedKodeverkService: CachedKodeverkService = mockk(relaxed = true)
    private val bidragPersonClient: BidragPersonClient = mockk()

    private lateinit var validator: BidragssakValidator

    @BeforeEach
    fun setup() {
        every { cachedKodeverkService.hentLandkoder() } returns mapOf(Landkode("NOR") to "Norge")
        every { bidragPersonClient.hentFødselsdatoer(any()) } returns emptyMap()
        validator = BidragssakValidator(identConsumer, cachedKodeverkService, bidragPersonClient)
    }

    @AfterEach
    fun ryddUnleash() {
        unmockkObject(UnleashFeaturesProvider.Companion)
    }

    private fun aktiverUnntak(vararg flagg: UnleashFeatures) {
        mockkObject(UnleashFeaturesProvider.Companion)
        every {
            UnleashFeaturesProvider.isEnabled(any(), false, false)
        } answers {
            firstArg<String>() in flagg.map { it.featureName }
        }
    }

    @Nested
    inner class Personroller {
        @Test
        fun `krever minst en kjent BP BM eller BA`() {
            shouldThrowMessage("Minst én person må ha en kjent rolle i saken.") {
                validator.valider(OpprettSakRequest(eierfogd = Enhetsnummer("1701"), roller = emptySet()))
            }
        }

        @Nested
        inner class Rolleendring {
            @Test
            fun `målrettet unntak tillater bare eksisterende rollekonflikt`() {
                val ident = genererPersonident()
                val annen = genererPersonident()
                val før = listOf(
                    BidragssakValidator.Saksrolle(1, Rolletype.BIDRAGSMOTTAKER, ident.verdi),
                    BidragssakValidator.Saksrolle(2, Rolletype.BIDRAGSPLIKTIG, ident.verdi),
                )
                val bm = Rolle(rolleId = 1, fødselsnummer = ident.verdi, rolleType = Rolletype.BIDRAGSMOTTAKER)
                val bp = Rolle(rolleId = 2, fødselsnummer = ident.verdi, rolleType = Rolletype.BIDRAGSPLIKTIG)
                aktiverUnntak(UnleashFeatures.TILLAT_EKSISTERENDE_DOBLE_SAKSROLLER)

                assertThatCode { validator.validerRolleendring(før, listOf(bm, bp)) }.doesNotThrowAnyException()

                val nyttBarn = Rolle(rolleId = 0, fødselsnummer = ident.verdi, rolleType = Rolletype.BARN)
                shouldThrowMessage("En person kan bare ha én rolle i saken, unntatt RM og FR.") {
                    validator.validerRolleendring(før, listOf(bm, bp, nyttBarn))
                }

                val nyBp = Rolle(rolleId = 2, fødselsnummer = annen.verdi, rolleType = Rolletype.BIDRAGSPLIKTIG)
                shouldThrowMessage("En kjent rolle kan ikke fjernes eller endres.") {
                    validator.validerRolleendring(før, listOf(bm, nyBp))
                }
            }

            @Test
            fun `duplikatroller avvises uten flagg og tillates med flagg også uten bruker`() {
                val ident = genererPersonident()
                val før = listOf(
                    BidragssakValidator.Saksrolle(1, Rolletype.BIDRAGSMOTTAKER, ident.verdi),
                    BidragssakValidator.Saksrolle(2, Rolletype.BIDRAGSPLIKTIG, ident.verdi),
                )
                val etter = listOf(
                    Rolle(rolleId = 1, fødselsnummer = ident.verdi, rolleType = Rolletype.BIDRAGSMOTTAKER),
                    Rolle(rolleId = 2, fødselsnummer = ident.verdi, rolleType = Rolletype.BIDRAGSPLIKTIG),
                )
                shouldThrowMessage("En person kan bare ha én rolle i saken, unntatt RM og FR.") {
                    validator.validerRolleendring(før, etter)
                }

                aktiverUnntak(UnleashFeatures.TILLAT_EKSISTERENDE_DOBLE_SAKSROLLER)
                assertThatCode { validator.validerRolleendring(før, etter) }.doesNotThrowAnyException()
            }

            @Test
            fun `avviser at kjent BM byttes ut`() {
                val opprinnelig = genererPersonident()
                val ny = genererPersonident()
                val før = listOf(BidragssakValidator.Saksrolle(1, Rolletype.BIDRAGSMOTTAKER, opprinnelig.verdi))
                val etter = listOf(Rolle(rolleId = 1, fødselsnummer = ny.verdi, rolleType = Rolletype.BIDRAGSMOTTAKER))

                shouldThrowMessage("En kjent rolle kan ikke fjernes eller endres.") {
                    validator.validerRolleendring(før, etter)
                }
            }

            @Test
            fun `avviser at kjent barn fjernes selv med kjent BM`() {
                val barn = genererPersonident()
                val bm = genererPersonident()
                val før = listOf(
                    BidragssakValidator.Saksrolle(1, Rolletype.BARN, barn.verdi),
                    BidragssakValidator.Saksrolle(2, Rolletype.BIDRAGSMOTTAKER, bm.verdi),
                )
                val etter = listOf(Rolle(rolleId = 2, fødselsnummer = bm.verdi, rolleType = Rolletype.BIDRAGSMOTTAKER))

                shouldThrowMessage("En kjent rolle kan ikke fjernes eller endres.") {
                    validator.validerRolleendring(før, etter)
                }
            }

            @Test
            fun `sjekker ikke RM for myndig barn som ikke er med i forespørselen`() {
                val barn = genererPersonident()
                val lagretBarn = Rolle(rolleId = 3, fødselsnummer = barn.verdi, rolleType = Rolletype.BARN)
                every { identConsumer.hentPersonInformasjon(barn) } returns
                    mockPersoninfo(barn, LocalDate.now().minusYears(18))

                assertThatCode {
                    validator.validerRolleendring(listOf(BidragssakValidator.Saksrolle(lagretBarn)), listOf(lagretBarn))
                }.doesNotThrowAnyException()
            }

            @Test
            fun `sjekker RM for myndig barn som er med i forespørselen`() {
                val barn = genererPersonident()
                every { identConsumer.hentPersonInformasjon(barn) } returns mockPersoninfo(barn, null)
                every { bidragPersonClient.hentFødselsdatoer(listOf(barn)) } returns
                    mapOf(barn to LocalDate.now().minusYears(18))

                shouldThrowMessage("Hvis barnet er myndig, må reell mottaker (RM) være satt.") {
                    validator.validerForespurteRoller(setOf(rolleBarnUtenRm(barn)))
                }
            }

            @Test
            fun `tillater registrering av tidligere ukjent BM`() {
                val barn = genererPersonident()
                val bm = genererPersonident()
                every { identConsumer.hentPersonInformasjon(barn) } returns
                    mockPersoninfo(barn, LocalDate.now().minusYears(10))
                val før = listOf(BidragssakValidator.Saksrolle(3, Rolletype.BARN, barn.verdi))
                val etter = listOf(
                    Rolle(rolleId = 2, fødselsnummer = bm.verdi, rolleType = Rolletype.BIDRAGSMOTTAKER),
                    Rolle(rolleId = 3, fødselsnummer = barn.verdi, rolleType = Rolletype.BARN),
                )

                assertThatCode { validator.validerRolleendring(før, etter) }.doesNotThrowAnyException()
            }
        }

        @Test
        fun `avviser samme person i to ulike roller`() {
            val ident = genererPersonident()
            shouldThrowMessage("En person kan bare ha én rolle i saken, unntatt RM og FR.") {
                validator.valider(
                    OpprettSakRequest(
                        eierfogd = Enhetsnummer("1701"),
                        roller = setOf(rolleBp(ident), rolleBm(ident)),
                    ),
                )
            }
        }

        @Test
        fun `avviser to roller for historisk og gjeldende ident for samme person`() {
            val gammelIdent = genererPersonident()
            val nyIdent = genererPersonident()
            every { identConsumer.hentAlleIdenter(gammelIdent.verdi) } returns
                listOf(gammelIdent.verdi, nyIdent.verdi)

            shouldThrowMessage("En person kan bare ha én rolle i saken, unntatt RM og FR.") {
                validator.valider(
                    OpprettSakRequest(
                        eierfogd = Enhetsnummer("1701"),
                        roller = setOf(rolleBp(gammelIdent), rolleBm(nyIdent)),
                    ),
                )
            }
        }

        @Test
        fun `tillater at barnet selv er RM`() {
            val barn = genererPersonident()
            every { identConsumer.hentPersonInformasjon(barn) } returns
                mockPersoninfo(barn, LocalDate.now().minusYears(18))
            val rolle = RolleDto(
                fødselsnummer = barn,
                type = Rolletype.BARN,
                reellMottaker = ReellMottakerDto(ReellMottaker(barn.verdi)),
            )
            assertThatCode {
                validator.valider(OpprettSakRequest(eierfogd = Enhetsnummer("1701"), roller = setOf(rolle)))
            }.doesNotThrowAnyException()
        }
    }

    // ==========================================================
    // Landkode-validering
    // ==========================================================
    @Nested
    inner class Landkode {
        @Test
        fun `skal kaste feil hvis request har ugyldig landkode`() {
            val req =
                OpprettSakRequest(
                    eierfogd = Enhetsnummer("1701"),
                    land = Landkode("XRY"),
                    roller = setOf(rolleBm(), rolleBarnUtenRm()),
                )

            shouldThrowMessage("Bidragssak forsøkt opprettet med ugyldig land: XRY") {
                validator.valider(req)
            }
        }

        @Test
        fun `skal godta gyldig landkode`() {
            val req =
                OpprettSakRequest(
                    eierfogd = Enhetsnummer("1701"),
                    land = Landkode("NOR"),
                    roller = setOf(rolleBm(), rolleBarnUtenRm()),
                )

            assertThatCode { validator.valider(req) }.doesNotThrowAnyException()
        }
    }

    // ==========================================================
    // Ukjent BM validering
    // ==========================================================
    @Nested
    inner class UkjentBM {
        @Test
        fun `skal kaste feil når BM har tomt fødselsnummer`() {
            val req =
                OpprettSakRequest(
                    eierfogd = Enhetsnummer("1701"),
                    roller =
                    setOf(
                        RolleDto(type = Rolletype.BIDRAGSMOTTAKER, fødselsnummer = Personident("")),
                        RolleDto(type = Rolletype.BARN, fødselsnummer = Personident("")),
                    ),
                )

            shouldThrowMessage("Fødselsnummer kan ikke være tom streng.") {
                validator.valider(req)
            }
        }

        @Test
        fun `skal godta barn under 18 uten RM når BM mangler`() {
            val barn = genererPersonident()
            every { identConsumer.hentPersonInformasjon(barn) } returns
                mockPersoninfo(barn, LocalDate.now().minusYears(10))
            val req =
                OpprettSakRequest(
                    eierfogd = Enhetsnummer("1701"),
                    roller =
                    setOf(
                        rolleBp(),
                        rolleBarnUtenRm(barn),
                    ),
                )

            assertThatCode { validator.valider(req) }.doesNotThrowAnyException()
        }

        @Test
        fun `skal godta ukjent BM, kjent BP, og alle barn har RM`() {
            val req =
                OpprettSakRequest(
                    eierfogd = Enhetsnummer("1701"),
                    roller =
                    setOf(
                        // Kjent BP (gyldig fnr)
                        RolleDto(
                            fødselsnummer = Personident(FnrGenerator.generer()),
                            type = Rolletype.BIDRAGSPLIKTIG,
                        ),
                        // Ukjent BM
                        // Barn uten RM
                        rolleBarnMedRm(genererPersonident()),
                    ),
                )

            // når BM er ukjent må alle barn ha RM
            assertThatCode { validator.valider(req) }.doesNotThrowAnyException()
        }
    }

    // ==========================================================
    // Ukjent BP validering
    // ==========================================================
    @Nested
    inner class UkjentBP {
        @Test
        fun `skal godta at BP mangler`() {
            val fnr = genererPersonident()

            val utenBP =
                OpprettSakRequest(
                    eierfogd = Enhetsnummer("1701"),
                    roller =
                    setOf(
                        RolleDto(fødselsnummer = Personident(FnrGenerator.generer()), type = Rolletype.BIDRAGSMOTTAKER),
                        RolleDto(
                            type = Rolletype.BARN,
                            fødselsnummer = fnr,
                        ),
                    ),
                )

            assertThatCode { validator.valider(utenBP) }.doesNotThrowAnyException()
            verify(exactly = 1) { identConsumer.hentPersonInformasjon(fnr) }
        }

        @Test
        fun `skal kaste feil når BP har tomt fødselsnummer`() {
            val req =
                OpprettSakRequest(
                    eierfogd = Enhetsnummer("1701"),
                    roller =
                    setOf(
                        RolleDto(type = Rolletype.BIDRAGSPLIKTIG, fødselsnummer = Personident("")), // <- ikke tillatt
                        RolleDto(fødselsnummer = Personident(FnrGenerator.generer()), type = Rolletype.BIDRAGSMOTTAKER),
                        RolleDto(type = Rolletype.BARN, fødselsnummer = Personident("22461353014")),
                    ),
                )

            shouldThrowMessage("Fødselsnummer kan ikke være tom streng.") {
                validator.valider(req)
            }
        }
    }

    // ==========================================================
    // Reell mottaker validering
    // ==========================================================
    @Nested
    inner class ReellMottaker {
        @Test
        fun `skal godta når BM finnes selv om barn under 18 mangler RM`() {
            val barnFnr = genererPersonident()

            // Barn er under 18
            every { identConsumer.hentPersonInformasjon(barnFnr) } returns
                mockPersoninfo(
                    ident = barnFnr,
                    fødselsdato = LocalDate.now().minusYears(10),
                )

            val req =
                OpprettSakRequest(
                    eierfogd = Enhetsnummer("1701"),
                    roller =
                    setOf(
                        rolleBm(), // BM finnes
                        rolleBarnUtenRm(barnFnr),
                    ),
                )

            assertThatCode { validator.valider(req) }.doesNotThrowAnyException()
            verify(exactly = 1) { identConsumer.hentPersonInformasjon(barnFnr) }
        }

        @Test
        fun `skal kaste feil når RM settes på ikke-barn rolle`() {
            val req =
                OpprettSakRequest(
                    eierfogd = Enhetsnummer("1701"),
                    roller =
                    setOf(
                        RolleDto(
                            type = Rolletype.BIDRAGSPLIKTIG,
                            fødselsnummer = genererPersonident(),
                            reellMottaker = ReellMottakerDto(ident = ReellMottaker("85000000083"), verge = false),
                        ),
                        RolleDto(
                            type = Rolletype.BIDRAGSMOTTAKER,
                            fødselsnummer = genererPersonident(),
                            reellMottaker = null,
                        ),
                        RolleDto(
                            type = Rolletype.BARN,
                            fødselsnummer = genererPersonident(),
                            reellMottaker = null,
                        ),
                    ),
                )

            shouldThrowMessage("Reell mottaker (RM) kan kun registreres på barn (BA).") {
                validator.valider(req)
            }
        }

        @Test
        fun `skal kaste feil når barn er myndig og RM mangler`() {
            val myndigFnr = genererPersonident()

            // fødselsdato som gjør barnet 18+
            every { identConsumer.hentPersonInformasjon(myndigFnr) } returns
                mockPersoninfo(ident = myndigFnr, fødselsdato = LocalDate.now().minusYears(18).minusDays(1))

            val req =
                OpprettSakRequest(
                    eierfogd = Enhetsnummer("1701"),
                    roller =
                    setOf(
                        rolleBm(),
                        RolleDto(type = Rolletype.BARN, fødselsnummer = myndigFnr),
                    ),
                )

            shouldThrowMessage("Hvis barnet er myndig, må reell mottaker (RM) være satt.") {
                validator.valider(req)
            }

            verify(exactly = 1) { identConsumer.hentPersonInformasjon(myndigFnr) }
        }

        @Test
        fun `skal godta når barn er myndig og RM er satt`() {
            val myndigFnr = genererPersonident()
            every { identConsumer.hentPersonInformasjon(myndigFnr) } returns
                mockPersoninfo(ident = myndigFnr, fødselsdato = LocalDate.now().minusYears(19))

            val req =
                OpprettSakRequest(
                    eierfogd = Enhetsnummer("1701"),
                    roller =
                    setOf(
                        rolleBarnMedRm(myndigFnr),
                    ),
                )

            assertThatCode { validator.valider(req) }.doesNotThrowAnyException()
            verify(exactly = 1) { identConsumer.hentPersonInformasjon(myndigFnr) }
        }

        @Test
        fun `skal ikke kreve RM hvis alder ikke kan beregnes (ident-oppslag feiler eller fødselsdato mangler)`() {
            val fnr = genererPersonident()

            // Simuler at oppslag ikke gir fødselsdato -> validerRolle fanger exception og alder blir null
            every { identConsumer.hentPersonInformasjon(fnr) } returns
                mockPersoninfo(ident = fnr, fødselsdato = null)

            val req =
                OpprettSakRequest(
                    eierfogd = Enhetsnummer("1701"),
                    roller =
                    setOf(
                        rolleBm(),
                        RolleDto(type = Rolletype.BARN, fødselsnummer = fnr), // ingen RM
                    ),
                )

            assertThatCode { validator.valider(req) }.doesNotThrowAnyException()
            verify(exactly = 1) { identConsumer.hentPersonInformasjon(fnr) }
            verify(exactly = 1) { bidragPersonClient.hentFødselsdatoer(listOf(fnr)) }
        }

        @Test
        fun `skal kreve RM når PDL-reserveoppslag finner at barnet er myndig`() {
            val barn = genererPersonident()
            every { identConsumer.hentPersonInformasjon(barn) } returns mockPersoninfo(barn, null)
            every { bidragPersonClient.hentFødselsdatoer(listOf(barn)) } returns
                mapOf(barn to LocalDate.now().minusYears(18))

            shouldThrowMessage("Hvis barnet er myndig, må reell mottaker (RM) være satt.") {
                validator.valider(
                    OpprettSakRequest(
                        eierfogd = Enhetsnummer("1701"),
                        roller = setOf(rolleBarnUtenRm(barn)),
                    ),
                )
            }
        }

        @Test
        fun `feil i PDL-reserveoppslag går videre`() {
            val barn = genererPersonident()
            every { identConsumer.hentPersonInformasjon(barn) } returns mockPersoninfo(barn, null)
            every { bidragPersonClient.hentFødselsdatoer(listOf(barn)) } throws IllegalStateException("PDL utilgjengelig")

            shouldThrowMessage("PDL utilgjengelig") {
                validator.valider(
                    OpprettSakRequest(
                        eierfogd = Enhetsnummer("1701"),
                        roller = setOf(rolleBarnUtenRm(barn)),
                    ),
                )
            }
        }

        @Test
        fun `skal regne RM med tomt fødselsnummer som ikke satt`() {
            val barnFnr = genererPersonident()

            every { identConsumer.hentPersonInformasjon(barnFnr) } returns
                mockPersoninfo(
                    ident = barnFnr,
                    fødselsdato = LocalDate.now().minusYears(19), // myndig barn
                )

            val req =
                OpprettSakRequest(
                    eierfogd = Enhetsnummer("1701"),
                    roller =
                    setOf(
                        rolleBm(),
                        RolleDto(
                            type = Rolletype.BARN,
                            fødselsnummer = barnFnr,
                            reellMottaker =
                            ReellMottakerDto(
                                ident = ReellMottaker(""), // tom streng
                                verge = false,
                            ),
                        ),
                    ),
                )

            shouldThrowMessage("Hvis barnet er myndig, må reell mottaker (RM) være satt.") {
                validator.valider(req)
            }

            verify(exactly = 1) { identConsumer.hentPersonInformasjon(barnFnr) }
        }

        @Test
        fun `skal godta når RM har gyldig nummer`() {
            val barnFnr = genererPersonident()

            every { identConsumer.hentPersonInformasjon(barnFnr) } returns
                mockPersoninfo(
                    ident = barnFnr,
                    fødselsdato = LocalDate.now().minusYears(19),
                )

            val req =
                OpprettSakRequest(
                    eierfogd = Enhetsnummer("1701"),
                    roller =
                    setOf(
                        rolleBm(),
                        rolleBarnMedRm(barnFnr),
                    ),
                )

            assertThatCode { validator.valider(req) }.doesNotThrowAnyException()
            verify(exactly = 1) { identConsumer.hentPersonInformasjon(barnFnr) }
        }

        @Test
        fun `skal regne RM null som ikke satt og kaste feil for myndig barn`() {
            val barnFnr = genererPersonident()

            every { identConsumer.hentPersonInformasjon(barnFnr) } returns
                mockPersoninfo(
                    ident = barnFnr,
                    fødselsdato = LocalDate.now().minusYears(19),
                )

            val req =
                OpprettSakRequest(
                    eierfogd = Enhetsnummer("1701"),
                    roller =
                    setOf(
                        rolleBm(),
                        RolleDto(
                            type = Rolletype.BARN,
                            fødselsnummer = barnFnr,
                            reellMottaker = null, // eksplisitt null
                        ),
                    ),
                )

            shouldThrowMessage("Hvis barnet er myndig, må reell mottaker (RM) være satt.") {
                validator.valider(req)
            }

            verify(exactly = 1) { identConsumer.hentPersonInformasjon(barnFnr) }
        }
    }

    // ==========================================================
    // Ektefellebidrag (BM + BP og uten barn) validering
    // ==========================================================
    @Nested
    inner class Ektefellebidrag {
        @Test
        fun `skal godta når BM og BP er oppgitt og ingen barn`() {
            val req =
                OpprettSakRequest(
                    eierfogd = Enhetsnummer("1701"),
                    roller = setOf(rolleBm(), rolleBp()),
                    arbeidsfordeling = Arbeidsfordeling.EKTEFELLLESAK,
                )

            assertThatCode { validator.valider(req) }.doesNotThrowAnyException()
        }
    }

    // ==========================================================
    // Antall BM/BP validering
    // ==========================================================
    @Nested
    inner class AntallRoller {
        @Test
        fun `skal kaste feil hvis request har flere enn én BM`() {
            val req =
                OpprettSakRequest(
                    eierfogd = Enhetsnummer("1701"),
                    roller =
                    setOf(
                        rolleBm(),
                        rolleBm(),
                        rolleBarnUtenRm(),
                    ),
                )

            shouldThrowMessage("Kan ikke ha flere enn én bidragsmottaker (BM).") {
                validator.valider(req)
            }
        }

        @Test
        fun `skal kaste feil hvis request har flere enn én BP`() {
            val req =
                OpprettSakRequest(
                    eierfogd = Enhetsnummer("1701"),
                    roller =
                    setOf(
                        rolleBp(),
                        rolleBp(),
                        rolleBarnUtenRm(),
                    ),
                )

            shouldThrowMessage("Kan ikke ha flere enn én bidragspliktig (BP).") {
                validator.valider(req)
            }
        }

        @Test
        fun `skal godta når BM og BP er oppgitt og ingen barn`() {
            val req =
                OpprettSakRequest(
                    eierfogd = Enhetsnummer("1701"),
                    roller =
                    setOf(
                        rolleBm(),
                        rolleBp(),
                        rolleBarnUtenRm(),
                    ),
                    arbeidsfordeling = Arbeidsfordeling.EKTEFELLLESAK,
                )

            assertThatCode { validator.valider(req) }.doesNotThrowAnyException()
        }
    }

    // ==========================================================
    // Person eksisterer validering
    // ==========================================================
    @Nested
    inner class PersonEksisterer {
        @Test
        fun `skal kaste feil når person ikke finnes for en rolle`() {
            val fnr = genererPersonident()

            every { identConsumer.hentPersonInformasjon(fnr) } returns null

            val req =
                OpprettSakRequest(
                    eierfogd = Enhetsnummer("1701"),
                    roller =
                    setOf(
                        rolleBm(fnr),
                        rolleBarnUtenRm(),
                    ),
                )

            shouldThrowMessage("Person finnes ikke for rolle av type BIDRAGSMOTTAKER.") {
                validator.valider(req)
            }

            verify(exactly = 1) { identConsumer.hentPersonInformasjon(fnr) }
        }

        @Test
        fun `skal kaste feil hvis person ikke finnes for barn`() {
            val barn = genererPersonident()
            val bm = genererPersonident()

            every { identConsumer.hentPersonInformasjon(barn) } returns null
            every { identConsumer.hentPersonInformasjon(bm) } returns
                mockPersoninfo(ident = bm, fødselsdato = LocalDate.now().minusYears(30))

            val req =
                OpprettSakRequest(
                    eierfogd = Enhetsnummer("1701"),
                    roller =
                    setOf(
                        rolleBm(),
                        rolleBarnMedRm(barn),
                    ),
                )

            shouldThrowMessage("Person finnes ikke for rolle av type BARN.") {
                validator.valider(req)
            }
        }
    }

    // ---- Test builders / helpers ----

    private fun rolleBm(fnr: Personident = genererPersonident()) = RolleDto(type = Rolletype.BIDRAGSMOTTAKER, fødselsnummer = fnr)

    private fun rolleBp(fnr: Personident = genererPersonident()) = RolleDto(type = Rolletype.BIDRAGSPLIKTIG, fødselsnummer = fnr)

    private fun rolleBarnUtenRm(fnr: Personident = genererPersonident()) = RolleDto(type = Rolletype.BARN, fødselsnummer = fnr)

    private fun rolleBarnMedRm(fnr: Personident) = RolleDto(
        type = Rolletype.BARN,
        fødselsnummer = fnr,
        reellMottaker = ReellMottakerDto(ident = ReellMottaker("85000000074"), verge = false),
    )

    private fun mockPersoninfo(
        ident: Personident,
        fødselsdato: LocalDate?,
    ): PersonDto = PersonDto(
        ident = ident,
        fødselsdato = fødselsdato,
    )
}
