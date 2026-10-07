package no.nav.bidrag.sak.validering

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowMessage
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.sak.Arbeidsfordeling
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.ident.ReellMottaker
import no.nav.bidrag.domene.land.Landkode
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.generer.testdata.person.genererPersonident
import no.nav.bidrag.sak.domain.Rolle
import no.nav.bidrag.sak.util.FnrGenerator
import no.nav.bidrag.transport.sak.OpprettSakRequest
import no.nav.bidrag.transport.sak.ReellMottakerDto
import no.nav.bidrag.transport.sak.RolleDto
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BidragssakValidatorTest {
    private val validator = BidragssakValidator()

    @Nested
    inner class MinstÉnKjentRolle {
        @Test
        fun `krever minst en kjent BP BM eller BA`() {
            val req = OpprettSakRequest(eierfogd = Enhetsnummer("1701"), roller = emptySet())
            shouldThrowMessage("Minst én person må ha en kjent rolle i saken.") {
                validator.valider(req, grunnlagFor(req))
            }
        }
    }

    @Nested
    inner class ÉnRollePerPerson {
        @Test
        fun `avviser samme person i to ulike roller`() {
            val ident = genererPersonident()
            val req = OpprettSakRequest(eierfogd = Enhetsnummer("1701"), roller = setOf(rolleBp(ident), rolleBm(ident)))

            shouldThrowMessage("En person kan bare ha én rolle i saken, unntatt RM og FR.") {
                validator.valider(req, grunnlagFor(req))
            }
        }

        @Test
        fun `avviser to roller for historisk og gjeldende ident for samme person`() {
            val gammelIdent = genererPersonident()
            val nyIdent = genererPersonident()
            val req = OpprettSakRequest(eierfogd = Enhetsnummer("1701"), roller = setOf(rolleBp(gammelIdent), rolleBm(nyIdent)))

            shouldThrowMessage("En person kan bare ha én rolle i saken, unntatt RM og FR.") {
                validator.valider(req, grunnlagFor(req).medSammePerson(gammelIdent, nyIdent))
            }
        }

        @Test
        fun `avviser nytt barn med ny ident når bare den nye identen kjenner den gamle`() {
            val gammelIdent = genererPersonident()
            val nyIdent = genererPersonident()
            val før = listOf(BidragssakValidator.Saksrolle(3, Rolletype.BARN, gammelIdent.verdi))
            val etter = listOf(
                Rolle(rolleId = 3, fødselsnummer = gammelIdent.verdi, rolleType = Rolletype.BARN),
                Rolle(rolleId = 4, fødselsnummer = nyIdent.verdi, rolleType = Rolletype.BARN),
            )
            val grunnlag = grunnlag(gammelIdent, nyIdent).let {
                it.copy(identer = it.identer + (nyIdent.verdi to setOf(nyIdent.verdi, gammelIdent.verdi)))
            }

            shouldThrowMessage("En person kan bare ha én rolle i saken, unntatt RM og FR.") {
                validator.validerRolleendring(før, etter, grunnlag)
            }
        }

        @Test
        fun `feiler når grunnlaget mangler identer for en rolle`() {
            val bm = genererPersonident()
            val bp = genererPersonident()
            val før = listOf(BidragssakValidator.Saksrolle(1, Rolletype.BIDRAGSMOTTAKER, bm.verdi))
            val etter = listOf(
                Rolle(rolleId = 1, fødselsnummer = bm.verdi, rolleType = Rolletype.BIDRAGSMOTTAKER),
                Rolle(rolleId = 2, fødselsnummer = bp.verdi, rolleType = Rolletype.BIDRAGSPLIKTIG),
            )

            shouldThrow<IllegalStateException> {
                validator.validerRolleendring(før, etter, grunnlag(bp))
            }
        }

        @Test
        fun `avviser samme barn to ganger i forespørselen`() {
            val barn = genererPersonident()

            shouldThrowMessage("En person kan bare ha én rolle i saken, unntatt RM og FR.") {
                validator.validerForespurteRoller(setOf(rolleBarnUtenRm(barn), rolleBarnMedRm(barn)), grunnlag(barn))
            }
        }

        @Test
        fun `tillater at barnet selv er RM`() {
            val barn = genererPersonident()
            val rolle = RolleDto(
                fødselsnummer = barn,
                type = Rolletype.BARN,
                reellMottaker = ReellMottakerDto(ReellMottaker(barn.verdi)),
            )
            val req = OpprettSakRequest(eierfogd = Enhetsnummer("1701"), roller = setOf(rolle))

            assertThatCode {
                validator.valider(req, grunnlagFor(req).medFødselsdato(barn, LocalDate.now().minusYears(18)))
            }.doesNotThrowAnyException()
        }
    }

    @Nested
    inner class Rolleendring {
        @Test
        fun `avviser eksisterende doble roller ved endring`() {
            val ident = genererPersonident()
            val før = listOf(
                BidragssakValidator.Saksrolle(1, Rolletype.BIDRAGSMOTTAKER, ident.verdi),
                BidragssakValidator.Saksrolle(2, Rolletype.BIDRAGSPLIKTIG, ident.verdi),
            )
            val bm = Rolle(rolleId = 1, fødselsnummer = ident.verdi, rolleType = Rolletype.BIDRAGSMOTTAKER)
            val bp = Rolle(rolleId = 2, fødselsnummer = ident.verdi, rolleType = Rolletype.BIDRAGSPLIKTIG)

            shouldThrowMessage("En person kan bare ha én rolle i saken, unntatt RM og FR.") {
                validator.validerRolleendring(før, listOf(bm, bp), grunnlag(ident))
            }
        }

        @Test
        fun `avviser ny konflikt med tidligere ukjent rolle`() {
            val bm = genererPersonident()
            val før = listOf(
                BidragssakValidator.Saksrolle(1, Rolletype.BIDRAGSMOTTAKER, bm.verdi),
                BidragssakValidator.Saksrolle(2, Rolletype.BIDRAGSPLIKTIG, null),
            )
            val etter = listOf(
                Rolle(rolleId = 1, fødselsnummer = bm.verdi, rolleType = Rolletype.BIDRAGSMOTTAKER),
                Rolle(rolleId = 2, fødselsnummer = bm.verdi, rolleType = Rolletype.BIDRAGSPLIKTIG),
            )

            shouldThrowMessage("En person kan bare ha én rolle i saken, unntatt RM og FR.") {
                validator.validerRolleendring(før, etter, grunnlag(bm))
            }
        }

        @Test
        fun `avviser at kjent BM byttes ut`() {
            val opprinnelig = genererPersonident()
            val ny = genererPersonident()
            val før = listOf(BidragssakValidator.Saksrolle(1, Rolletype.BIDRAGSMOTTAKER, opprinnelig.verdi))
            val etter = listOf(Rolle(rolleId = 1, fødselsnummer = ny.verdi, rolleType = Rolletype.BIDRAGSMOTTAKER))

            shouldThrowMessage("En kjent rolle kan ikke fjernes eller endres.") {
                validator.validerRolleendring(før, etter, grunnlag(opprinnelig, ny))
            }
        }

        @Test
        fun `godtar kjent rolle med ny ident for samme person`() {
            val gammelIdent = genererPersonident()
            val nyIdent = genererPersonident()
            val før = listOf(BidragssakValidator.Saksrolle(1, Rolletype.BIDRAGSMOTTAKER, gammelIdent.verdi))
            val etter = listOf(Rolle(rolleId = 1, fødselsnummer = nyIdent.verdi, rolleType = Rolletype.BIDRAGSMOTTAKER))

            assertThatCode {
                validator.validerRolleendring(før, etter, grunnlag(gammelIdent, nyIdent).medSammePerson(gammelIdent, nyIdent))
            }.doesNotThrowAnyException()
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
                validator.validerRolleendring(før, etter, grunnlag(barn, bm))
            }
        }

        @Test
        fun `sjekker ikke RM for myndig barn som ikke er med i forespørselen`() {
            val barn = genererPersonident()
            val lagretBarn = Rolle(rolleId = 3, fødselsnummer = barn.verdi, rolleType = Rolletype.BARN)
            val grunnlag = grunnlag(barn).medFødselsdato(barn, LocalDate.now().minusYears(18))

            assertThatCode {
                validator.validerRolleendring(listOf(BidragssakValidator.Saksrolle(lagretBarn)), listOf(lagretBarn), grunnlag)
            }.doesNotThrowAnyException()
        }

        @Test
        fun `sjekker RM for myndig barn som er med i forespørselen`() {
            val barn = genererPersonident()
            val grunnlag = grunnlag(barn).medFødselsdato(barn, LocalDate.now().minusYears(18))

            shouldThrowMessage("Hvis barnet er myndig, må reell mottaker (RM) være satt.") {
                validator.validerForespurteRoller(setOf(rolleBarnUtenRm(barn)), grunnlag)
            }
        }

        @Test
        fun `tillater registrering av tidligere ukjent BM`() {
            val barn = genererPersonident()
            val bm = genererPersonident()
            val før = listOf(BidragssakValidator.Saksrolle(3, Rolletype.BARN, barn.verdi))
            val etter = listOf(
                Rolle(rolleId = 2, fødselsnummer = bm.verdi, rolleType = Rolletype.BIDRAGSMOTTAKER),
                Rolle(rolleId = 3, fødselsnummer = barn.verdi, rolleType = Rolletype.BARN),
            )

            assertThatCode { validator.validerRolleendring(før, etter, grunnlag(barn, bm)) }.doesNotThrowAnyException()
        }
    }

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
                validator.valider(req, grunnlagFor(req))
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

            assertThatCode { validator.valider(req, grunnlagFor(req)) }.doesNotThrowAnyException()
        }
    }

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
                validator.valider(req, grunnlagFor(req))
            }
        }

        @Test
        fun `skal godta barn under 18 uten RM når BM mangler`() {
            val barn = genererPersonident()
            val req = OpprettSakRequest(eierfogd = Enhetsnummer("1701"), roller = setOf(rolleBp(), rolleBarnUtenRm(barn)))

            assertThatCode {
                validator.valider(req, grunnlagFor(req).medFødselsdato(barn, LocalDate.now().minusYears(10)))
            }.doesNotThrowAnyException()
        }

        @Test
        fun `skal godta ukjent BM, kjent BP, og alle barn har RM`() {
            val req =
                OpprettSakRequest(
                    eierfogd = Enhetsnummer("1701"),
                    roller =
                    setOf(
                        RolleDto(fødselsnummer = Personident(FnrGenerator.generer()), type = Rolletype.BIDRAGSPLIKTIG),
                        rolleBarnMedRm(genererPersonident()),
                    ),
                )

            assertThatCode { validator.valider(req, grunnlagFor(req)) }.doesNotThrowAnyException()
        }
    }

    @Nested
    inner class UkjentBP {
        @Test
        fun `skal godta at BP mangler`() {
            val req =
                OpprettSakRequest(
                    eierfogd = Enhetsnummer("1701"),
                    roller =
                    setOf(
                        RolleDto(fødselsnummer = Personident(FnrGenerator.generer()), type = Rolletype.BIDRAGSMOTTAKER),
                        RolleDto(type = Rolletype.BARN, fødselsnummer = genererPersonident()),
                    ),
                )

            assertThatCode { validator.valider(req, grunnlagFor(req)) }.doesNotThrowAnyException()
        }

        @Test
        fun `skal kaste feil når BP har tomt fødselsnummer`() {
            val req =
                OpprettSakRequest(
                    eierfogd = Enhetsnummer("1701"),
                    roller =
                    setOf(
                        RolleDto(type = Rolletype.BIDRAGSPLIKTIG, fødselsnummer = Personident("")),
                        RolleDto(fødselsnummer = Personident(FnrGenerator.generer()), type = Rolletype.BIDRAGSMOTTAKER),
                        RolleDto(type = Rolletype.BARN, fødselsnummer = Personident("22461353014")),
                    ),
                )

            shouldThrowMessage("Fødselsnummer kan ikke være tom streng.") {
                validator.valider(req, grunnlagFor(req))
            }
        }
    }

    @Nested
    inner class ReellMottaker {
        @Test
        fun `skal godta når BM finnes selv om barn under 18 mangler RM`() {
            val barn = genererPersonident()
            val req = OpprettSakRequest(eierfogd = Enhetsnummer("1701"), roller = setOf(rolleBm(), rolleBarnUtenRm(barn)))

            assertThatCode {
                validator.valider(req, grunnlagFor(req).medFødselsdato(barn, LocalDate.now().minusYears(10)))
            }.doesNotThrowAnyException()
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
                        rolleBm(),
                        rolleBarnUtenRm(),
                    ),
                )

            shouldThrowMessage("Reell mottaker (RM) kan kun registreres på barn (BA).") {
                validator.valider(req, grunnlagFor(req))
            }
        }

        @Test
        fun `skal kaste feil når barn er myndig og RM mangler`() {
            val barn = genererPersonident()
            val req = OpprettSakRequest(eierfogd = Enhetsnummer("1701"), roller = setOf(rolleBm(), rolleBarnUtenRm(barn)))

            shouldThrowMessage("Hvis barnet er myndig, må reell mottaker (RM) være satt.") {
                validator.valider(req, grunnlagFor(req).medFødselsdato(barn, LocalDate.now().minusYears(18).minusDays(1)))
            }
        }

        @Test
        fun `skal godta når barn er myndig og RM er satt`() {
            val barn = genererPersonident()
            val req = OpprettSakRequest(eierfogd = Enhetsnummer("1701"), roller = setOf(rolleBarnMedRm(barn)))

            assertThatCode {
                validator.valider(req, grunnlagFor(req).medFødselsdato(barn, LocalDate.now().minusYears(19)))
            }.doesNotThrowAnyException()
        }

        @Test
        fun `skal ikke kreve RM når fødselsdato er ukjent`() {
            val req = OpprettSakRequest(eierfogd = Enhetsnummer("1701"), roller = setOf(rolleBm(), rolleBarnUtenRm()))

            assertThatCode { validator.valider(req, grunnlagFor(req)) }.doesNotThrowAnyException()
        }

        @Test
        fun `skal regne RM med tomt fødselsnummer som ikke satt`() {
            val barn = genererPersonident()
            val rolle = RolleDto(
                type = Rolletype.BARN,
                fødselsnummer = barn,
                reellMottaker = ReellMottakerDto(ident = ReellMottaker(""), verge = false),
            )
            val req = OpprettSakRequest(eierfogd = Enhetsnummer("1701"), roller = setOf(rolleBm(), rolle))

            shouldThrowMessage("Hvis barnet er myndig, må reell mottaker (RM) være satt.") {
                validator.valider(req, grunnlagFor(req).medFødselsdato(barn, LocalDate.now().minusYears(19)))
            }
        }

        @Test
        fun `skal godta når RM har gyldig nummer`() {
            val barn = genererPersonident()
            val req = OpprettSakRequest(eierfogd = Enhetsnummer("1701"), roller = setOf(rolleBm(), rolleBarnMedRm(barn)))

            assertThatCode {
                validator.valider(req, grunnlagFor(req).medFødselsdato(barn, LocalDate.now().minusYears(19)))
            }.doesNotThrowAnyException()
        }

        @Test
        fun `skal regne RM null som ikke satt og kaste feil for myndig barn`() {
            val barn = genererPersonident()
            val rolle = RolleDto(type = Rolletype.BARN, fødselsnummer = barn, reellMottaker = null)
            val req = OpprettSakRequest(eierfogd = Enhetsnummer("1701"), roller = setOf(rolleBm(), rolle))

            shouldThrowMessage("Hvis barnet er myndig, må reell mottaker (RM) være satt.") {
                validator.valider(req, grunnlagFor(req).medFødselsdato(barn, LocalDate.now().minusYears(19)))
            }
        }
    }

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

            assertThatCode { validator.valider(req, grunnlagFor(req)) }.doesNotThrowAnyException()
        }
    }

    @Nested
    inner class AntallRoller {
        @Test
        fun `skal kaste feil hvis request har flere enn én BM`() {
            val req = OpprettSakRequest(eierfogd = Enhetsnummer("1701"), roller = setOf(rolleBm(), rolleBm(), rolleBarnUtenRm()))

            shouldThrowMessage("Kan ikke ha flere enn én bidragsmottaker (BM).") {
                validator.valider(req, grunnlagFor(req))
            }
        }

        @Test
        fun `skal kaste feil hvis request har flere enn én BP`() {
            val req = OpprettSakRequest(eierfogd = Enhetsnummer("1701"), roller = setOf(rolleBp(), rolleBp(), rolleBarnUtenRm()))

            shouldThrowMessage("Kan ikke ha flere enn én bidragspliktig (BP).") {
                validator.valider(req, grunnlagFor(req))
            }
        }

        @Test
        fun `skal godta når BM og BP er oppgitt og ingen barn`() {
            val req =
                OpprettSakRequest(
                    eierfogd = Enhetsnummer("1701"),
                    roller = setOf(rolleBm(), rolleBp(), rolleBarnUtenRm()),
                    arbeidsfordeling = Arbeidsfordeling.EKTEFELLLESAK,
                )

            assertThatCode { validator.valider(req, grunnlagFor(req)) }.doesNotThrowAnyException()
        }
    }

    @Nested
    inner class PersonEksisterer {
        @Test
        fun `skal kaste feil når person ikke finnes for en rolle`() {
            val bm = genererPersonident()
            val req = OpprettSakRequest(eierfogd = Enhetsnummer("1701"), roller = setOf(rolleBm(bm), rolleBarnUtenRm()))

            shouldThrowMessage("Person finnes ikke for rolle av type BIDRAGSMOTTAKER.") {
                validator.valider(req, grunnlagFor(req).utenPerson(bm))
            }
        }

        @Test
        fun `skal kaste feil hvis person ikke finnes for barn`() {
            val barn = genererPersonident()
            val req = OpprettSakRequest(eierfogd = Enhetsnummer("1701"), roller = setOf(rolleBm(), rolleBarnMedRm(barn)))

            shouldThrowMessage("Person finnes ikke for rolle av type BARN.") {
                validator.valider(req, grunnlagFor(req).utenPerson(barn))
            }
        }
    }

    private fun grunnlag(vararg identer: Personident) = Valideringsgrunnlag(
        personer = identer.associate { it.verdi to Valideringsgrunnlag.Person(fødselsdato = null) },
        fødselsdatoer = emptyMap(),
        identer = identer.associate { it.verdi to setOf(it.verdi) },
        landkoder = setOf(Landkode("NOR")),
    )

    private fun grunnlagFor(request: OpprettSakRequest) = grunnlag(*request.roller.mapNotNull { it.fødselsnummer }.filter { it.verdi.isNotBlank() }.toTypedArray())

    private fun Valideringsgrunnlag.medFødselsdato(ident: Personident, fødselsdato: LocalDate) = copy(personer = personer + (ident.verdi to Valideringsgrunnlag.Person(fødselsdato)))

    private fun Valideringsgrunnlag.utenPerson(ident: Personident) = copy(personer = personer - ident.verdi)

    private fun Valideringsgrunnlag.medSammePerson(første: Personident, andre: Personident): Valideringsgrunnlag {
        val begge = setOf(første.verdi, andre.verdi)
        return copy(identer = identer + (første.verdi to begge) + (andre.verdi to begge))
    }

    private fun rolleBm(fnr: Personident = genererPersonident()) = RolleDto(type = Rolletype.BIDRAGSMOTTAKER, fødselsnummer = fnr)

    private fun rolleBp(fnr: Personident = genererPersonident()) = RolleDto(type = Rolletype.BIDRAGSPLIKTIG, fødselsnummer = fnr)

    private fun rolleBarnUtenRm(fnr: Personident = genererPersonident()) = RolleDto(type = Rolletype.BARN, fødselsnummer = fnr)

    private fun rolleBarnMedRm(fnr: Personident) = RolleDto(
        type = Rolletype.BARN,
        fødselsnummer = fnr,
        reellMottaker = ReellMottakerDto(ident = ReellMottaker("85000000074"), verge = false),
    )
}
