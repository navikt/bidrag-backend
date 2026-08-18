package no.nav.bidrag.sak.validering

import io.kotest.assertions.throwables.shouldThrowMessage
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.bidrag.commons.util.IdentConsumer
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.sak.Arbeidsfordeling
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.ident.ReellMottaker
import no.nav.bidrag.domene.land.Landkode
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.generer.testdata.person.genererPersonident
import no.nav.bidrag.sak.integration.kodeverk.CachedKodeverkService
import no.nav.bidrag.sak.util.FnrGenerator
import no.nav.bidrag.transport.person.PersonDto
import no.nav.bidrag.transport.sak.OpprettSakRequest
import no.nav.bidrag.transport.sak.ReellMottakerDto
import no.nav.bidrag.transport.sak.RolleDto
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OpprettSakValidatorTest {
    private val identConsumer: IdentConsumer = mockk(relaxed = true)
    private val cachedKodeverkService: CachedKodeverkService = mockk(relaxed = true)

    private lateinit var validator: OpprettSakValidator

    @BeforeEach
    fun setup() {
        every { cachedKodeverkService.hentLandkoder() } returns mapOf(Landkode("NOR") to "Norge")
        validator = OpprettSakValidator(identConsumer, cachedKodeverkService)
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
        fun `skal kaste feil når BM mangler og ikke alle barn har RM`() {
            val req =
                OpprettSakRequest(
                    eierfogd = Enhetsnummer("1701"),
                    roller =
                    setOf(
                        rolleBp(),
                        rolleBarnUtenRm(), // mangler RM
                    ),
                )

            shouldThrowMessage("Når bidragsmottaker (BM) mangler, må alle barn (BA) ha reell mottaker (RM).") {
                validator.valider(req)
            }
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
