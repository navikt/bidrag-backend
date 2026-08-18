package no.nav.bidrag.dokument.journalpost.entity

import no.nav.bidrag.commons.util.KildesystemIdenfikator
import no.nav.bidrag.dokument.journalpost.AvvikshendelseBuilder.Companion.enAvvikshendelse
import no.nav.bidrag.dokument.journalpost.AvvikshendelseBuilder.Companion.enAvvikshendelseFor
import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles
import no.nav.bidrag.dokument.journalpost.dto.AvsenderMottaker
import no.nav.bidrag.dokument.journalpost.dto.AvvikshendelseIntern
import no.nav.bidrag.dokument.journalpost.dto.CommandBuilder
import no.nav.bidrag.dokument.journalpost.dto.DokumentIntern
import no.nav.bidrag.dokument.journalpost.dto.EndreJournalpostCommandIntern
import no.nav.bidrag.dokument.journalpost.dto.JournalpostIntern
import no.nav.bidrag.dokument.journalpost.dto.KodeIntern
import no.nav.bidrag.dokument.journalpost.dto.Oppgave
import no.nav.bidrag.dokument.journalpost.exception.SakIkkeTilknyttetJournalpostException
import no.nav.bidrag.dokument.journalpost.exception.SaksnummerManglerException
import no.nav.bidrag.dokument.journalpost.exception.ViolationException
import no.nav.bidrag.dokument.journalpost.model.Avviksbehandling
import no.nav.bidrag.dokument.journalpost.model.Avvikstype
import no.nav.bidrag.dokument.journalpost.model.DokumentType
import no.nav.bidrag.dokument.journalpost.model.Fagomrade
import no.nav.bidrag.dokument.journalpost.model.GyldigAvviksbehandling
import no.nav.bidrag.dokument.journalpost.model.JoarkArkiveringStatus
import no.nav.bidrag.dokument.journalpost.model.Journalstatus
import no.nav.bidrag.dokument.journalpost.model.UgyldigAvviksbehandling
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.transport.dokument.AktorDto
import no.nav.bidrag.transport.dokument.AvvikType
import no.nav.bidrag.transport.dokument.DokumentDto
import no.nav.bidrag.transport.dokument.EndreDokument
import no.nav.bidrag.transport.dokument.EndreJournalpostCommand
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.function.Consumer
import java.util.stream.Stream

@DisplayName("Journalpost")
@ActiveProfiles(BidragDokumentJournalpostProfiles.TEST)
internal class JournalpostTest {
    private var journalpost = Journalpost()

    @BeforeEach
    fun settJournalpostId() {
        journalpost.journalpostId = -1
    }

    @Nested
    @DisplayName("og dto")
    internal inner class Dto {
        @Test
        @DisplayName("skal lage JournalpostDto fra entitet")
        fun skalLageDtoFraEntitet() {
            val journalpost =
                JournalpostBygger
                    .enJournalpost()
                    .medAvsender("Kom")
                    .medBeskrivelse("from russia with love")
                    .medDokumentdato(LocalDate.now())
                    .medDokumentreferanse("10101")
                    .medDokumentType("Røyksignal")
                    .medFagomrade("LUDO")
                    .medGjelder(genererFødselsnummer())
                    .medJournalforendeEnhet("trygdekontoret")
                    .medJournalfortAv("tobias")
                    .medJournaldato(LocalDate.now().plusDays(2))
                    .leggTilSaksnummer("123456789")
                    .hent()
            journalpost.journalpostId = 123
            val (avsenderNavn, _, dokumenter, dokumentDato, _, _, fagomrade, _, gjelderAktor, innhold, journalforendeEnhet, journalfortAv, journalfortDato, journalpostId, _, _, mottattDato) =
                journalpost
                    .tilJournalpostIntern()
                    .tilJournalpostDto()
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(
                            avsenderNavn,
                        ).`as`("avsenderNavn")
                        .isEqualTo("Kom")
                },
                Executable {
                    Assertions
                        .assertThat(
                            innhold,
                        ).`as`("innhold")
                        .isEqualTo(journalpost.getBeskrivelse())
                },
                Executable {
                    Assertions
                        .assertThat(
                            dokumentDato,
                        ).`as`("dokumentDato")
                        .isEqualTo(journalpost.getDokumentdato())
                },
                Executable {
                    Assertions
                        .assertThat<DokumentDto>(
                            dokumenter,
                        ).extracting<String, RuntimeException>(DokumentDto::dokumentreferanse)
                        .`as`("dokumentreferanse")
                        .isEqualTo(listOf<String>(journalpost.getDokumentreferanse()))
                },
                Executable {
                    Assertions
                        .assertThat<DokumentDto>(
                            dokumenter,
                        ).extracting<String, RuntimeException>(DokumentDto::dokumentType)
                        .`as`("dokumenttype")
                        .isEqualTo(listOf<String>(journalpost.getDokumentType()))
                },
                Executable {
                    Assertions
                        .assertThat(
                            fagomrade,
                        ).`as`("fagomrade")
                        .isEqualTo("LUDO")
                },
                Executable {
                    Assertions
                        .assertThat(
                            gjelderAktor,
                        ).`as`("gjelderAktor")
                        .isEqualTo(AktorDto(journalpost.getGjelder(), null))
                },
                Executable {
                    Assertions
                        .assertThat(
                            journalfortAv,
                        ).`as`("journalfortAv")
                        .isEqualTo(journalpost.getJournalfortAv())
                },
                Executable {
                    Assertions
                        .assertThat(
                            journalforendeEnhet,
                        ).`as`("journalforendeEnhet")
                        .isEqualTo("trygdekontoret")
                },
                Executable {
                    Assertions
                        .assertThat(
                            journalfortDato,
                        ).`as`("journalfortDato")
                        .isEqualTo(LocalDate.now().plusDays(2))
                },
                Executable {
                    Assertions
                        .assertThat(
                            journalpostId,
                        ).`as`("journalpostId")
                        .isEqualTo(KildesystemIdenfikator.PREFIX_BIDRAG_COMPLETE + "123")
                },
                Executable {
                    Assertions
                        .assertThat(
                            mottattDato,
                        ).`as`("mottattDato")
                        .isEqualTo(journalpost.journaldato)
                },
            )
        }

        @Test
        @DisplayName("skal ikke endre data uten en journalpost id på journalposten")
        fun skalIkkeEndreDataUtenJournalpostIdPaJournalposten() {
            Assertions
                .assertThatExceptionOfType(
                    ViolationException::class.java,
                ).isThrownBy {
                    journalpost.endre(
                        EndreJournalpostCommandIntern(
                            1,
                            "4806",
                            EndreJournalpostCommand(),
                        ),
                    )
                }.withMessageContaining("Kan ikke endre journalpost med ugyldig id")
        }

        @Test
        @DisplayName("skal ikke endre data med feil journalpost id i forhold til Journalposten")
        fun skalIkkeEndreDataMedFeilJournalpostId() {
            val endreJournalpostCommandIntern =
                EndreJournalpostCommandIntern(1, "4806", EndreJournalpostCommand())
            journalpost.journalpostId = 2
            Assertions
                .assertThatExceptionOfType(
                    ViolationException::class.java,
                ).isThrownBy { journalpost.endre(endreJournalpostCommandIntern) }
                .withMessageContaining("Kan ikke endre journalpost med ugyldig id")
        }
    }

    @Nested
    @DisplayName("og datakvalitet")
    internal inner class Datakvalitet {
        @Test
        @DisplayName("skal filtrere på fagområde BID som om det er BNR")
        fun skalFiltrereJournalpostPaFagomradeBidLikBnr() {
            val journalpostMedFagomradeBnr =
                JournalpostBygger
                    .enJournalpost()
                    .medFagomrade(
                        Fagomrade.BIDRAG_DATABASE,
                    ).hent()
            val journalpostMedFagomradeFar =
                JournalpostBygger
                    .enJournalpost()
                    .medFagomrade(
                        Fagomrade.FARSKAP,
                    ).hent()
            Assertions
                .assertThat(
                    Stream
                        .of(journalpostMedFagomradeBnr, journalpostMedFagomradeFar)
                        .filter { jp: Journalpost -> jp.erFor(Fagomrade.BIDRAG) },
                ).hasSize(1)
        }

        @Test
        @DisplayName("skal oversette fagområade til BID når BNR")
        fun skalOversetteFagormradeTilBidNarBnr() {
            val journalpostMedFagomradeBnr =
                JournalpostBygger
                    .enJournalpost()
                    .medFagomrade(
                        Fagomrade.BIDRAG_DATABASE,
                    ).leggTilSaksnummer("12345")
                    .hent()
            val journalpostMedFagomradeFar =
                JournalpostBygger
                    .enJournalpost()
                    .medFagomrade(
                        Fagomrade.FARSKAP,
                    ).leggTilSaksnummer("12345")
                    .hent()
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(journalpostMedFagomradeBnr.tilJournalpostIntern())
                        .extracting(JournalpostIntern::fagomrade)
                        .`as`("BNR")
                        .isEqualTo(Fagomrade.BIDRAG)
                },
                Executable {
                    Assertions
                        .assertThat(journalpostMedFagomradeFar.tilJournalpostIntern())
                        .extracting(JournalpostIntern::fagomrade)
                        .`as`("FAR")
                        .isEqualTo(Fagomrade.FARSKAP)
                },
            )
        }

        @Test
        @DisplayName("skal ikke mappe gjelder bruker id når feltet er null")
        fun skalIkkeMappeGjelderBrukerIdNarFeltErNull() {
            val (_, _, _, _, _, _, _, _, gjelderAktor) = journalpost.tilJournalpostIntern()
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(journalpost.getGjelder()).isNull() },
                Executable {
                    Assertions
                        .assertThat(
                            gjelderAktor,
                        ).isNull()
                },
            )
        }

        @DisplayName("skal lage blanke strenger når (enkelte) felt som er av type streng kan være null")
        @Test
        fun skalLageBlankeStrengerNarFeltErNull() {
            val (avsenderNavn, _, _, _, _, _, _, _, _, innhold, journalforendeEnhet, journalfortAv) =
                journalpost
                    .tilJournalpostIntern()
                    .tilJournalpostDto()
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(
                            avsenderNavn,
                        ).`as`("avsenderNavn")
                        .isEqualTo(BLANK_STRENG)
                },
                Executable {
                    Assertions
                        .assertThat(
                            innhold,
                        ).`as`("innhold")
                        .isEqualTo(BLANK_STRENG)
                },
                Executable {
                    Assertions
                        .assertThat(
                            journalforendeEnhet,
                        ).`as`("journalforendeEnhet")
                        .isEqualTo(BLANK_STRENG)
                },
                Executable {
                    Assertions
                        .assertThat(
                            journalfortAv,
                        ).`as`("journalfortAv")
                        .isEqualTo(BLANK_STRENG)
                },
            )
        }

        @Test
        @DisplayName("skal ikke endre til blanke felt i database")
        fun skalIkkeEndreTilBlankeFelt() {
            val endreJournalpostCommandIntern =
                EndreJournalpostCommandIntern(101, "4806", EndreJournalpostCommand())
            endreJournalpostCommandIntern.avsenderMottaker =
                AvsenderMottaker("Til test", BLANK_STRENG, null)
            endreJournalpostCommandIntern.beskrivelse = BLANK_STRENG
            endreJournalpostCommandIntern.gjelder = BLANK_STRENG
            journalpost.journalpostId = 101
            journalpost.endre(endreJournalpostCommandIntern)
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(journalpost.avsenderFornavn)
                        .`as`("avsenderFornavn")
                        .isNull()
                },
                Executable {
                    Assertions.assertThat(journalpost.getBeskrivelse()).`as`("beskrivelse").isNull()
                },
                Executable {
                    Assertions.assertThat(journalpost.getGjelder()).`as`("gjelder").isNull()
                },
            )
        }

        @Test
        @DisplayName("skal hente ut fornavn og etternavn fra avsendernavn")
        fun skalHenteUtFornavnOgEtternavnFraAvsendernavn() {
            journalpost = JournalpostBygger.enJournalpost().medAvsender("Bukken, Bruse").hent()
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(journalpost.avsenderFornavn)
                        .`as`("fornavn")
                        .isEqualTo("Bruse")
                },
                Executable {
                    Assertions
                        .assertThat(journalpost.getAvsender())
                        .`as`("etternavn")
                        .isEqualTo("Bukken")
                },
            )
        }

        @Test
        @DisplayName("skal hente ut etternavn fra avsendernavn")
        fun skalHenteUtEtternavnFraAvsendernavn() {
            journalpost = JournalpostBygger.enJournalpost().medAvsender("Bond").hent()
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions.assertThat(journalpost.avsenderFornavn).`as`("fornavn").isNull()
                },
                Executable {
                    Assertions
                        .assertThat(journalpost.getAvsender())
                        .`as`("etternavn")
                        .isEqualTo("Bond")
                },
            )
        }

        @Test
        @DisplayName("skal hente ut fornavn fra avsendernavn")
        fun skalHenteUtFornavnFraAvsendernavn() {
            journalpost = JournalpostBygger.enJournalpost().medAvsender(", James").hent()
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(journalpost.avsenderFornavn)
                        .`as`("fornavn")
                        .isEqualTo("James")
                },
                Executable {
                    Assertions.assertThat(journalpost.getAvsender()).`as`("etternavn").isNull()
                },
            )
        }

        @Test
        @DisplayName("skal ikke sette blankt fornavn")
        fun skalIkkeSetteBlanktFornavn() {
            journalpost = JournalpostBygger.enJournalpost().medAvsender("Jameson, ").hent()
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions.assertThat(journalpost.avsenderFornavn).`as`("fornavn").isNull()
                },
                Executable {
                    Assertions
                        .assertThat(journalpost.getAvsender())
                        .`as`("etternavn")
                        .isEqualTo("Jameson")
                },
            )
        }

        @Test
        @DisplayName("skal mappe brevkode (uten dekode)")
        fun skalMappeBrevkode() {
            journalpost.brevkode = "koden"
            val journalpostIntern = journalpost.tilJournalpostIntern()
            Assertions
                .assertThat(journalpostIntern)
                .extracting(JournalpostIntern::brevkode)
                .isEqualTo(KodeIntern("koden"))
        }
    }

    @Nested
    @DisplayName("og journalføring")
    internal inner class Journalforing {
        @Test
        @DisplayName("skal hente ut dokument ved journalføring")
        fun skalHenteUtDokumentVedJournalforing() {
            journalpost =
                JournalpostBygger
                    .enJournalpost()
                    .medDokumentreferanse("dokref")
                    .medBeskrivelse("tittel")
                    .leggTilSaksnummer("12345")
                    .hent()
            journalpost.dokumentreferanseVedJournalforing = "original dokref"
            val (_, _, _, _, _, dokumenter) = journalpost.tilJournalpostIntern()
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(
                            dokumenter,
                        ).`as`("antall dokumenter")
                        .hasSize(2)
                },
                Executable {
                    Assertions
                        .assertThat(
                            dokumenter,
                        ).`as`("dokumenter")
                        .contains(
                            DokumentIntern("dokref", null, "tittel"),
                            DokumentIntern(
                                "original dokref",
                                null,
                                "Dokumentreferanse ved journalføring",
                            ),
                        )
                },
            )
        }

        @Test
        @DisplayName("skal ikke hente ut dokument ved journalføring når den er null")
        fun skalIkkeHenteUtDokumentVedJournalforingNarDenErNull() {
            journalpost =
                JournalpostBygger
                    .enJournalpost()
                    .medDokumentreferanse("dokref")
                    .medBeskrivelse("tittel")
                    .leggTilSaksnummer("12345")
                    .hent()
            journalpost.dokumentreferanseVedJournalforing = null
            val (_, _, _, _, _, dokumenter) = journalpost.tilJournalpostIntern()
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(
                            dokumenter,
                        ).`as`("antall dokumenter")
                        .hasSize(1)
                },
                Executable {
                    Assertions
                        .assertThat(
                            dokumenter,
                        ).`as`("dokumenter")
                        .contains(
                            DokumentIntern("dokref", null, "tittel"),
                        )
                },
            )
        }

        @Test
        @DisplayName("skal ikke hente ut dokument ved journalføring når den er lik dokumentreferansen")
        fun skalIkkeHenteUtDokumentVedJournalforingNarDenErLikDokumentreferansen() {
            journalpost =
                JournalpostBygger
                    .enJournalpost()
                    .medDokumentreferanse("dokref")
                    .medBeskrivelse("tittel")
                    .leggTilSaksnummer("12345")
                    .hent()
            journalpost.dokumentreferanseVedJournalforing = "dokref"
            val (_, _, _, _, _, dokumenter) = journalpost.tilJournalpostIntern()
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(
                            dokumenter,
                        ).`as`("antall dokumenter")
                        .hasSize(1)
                },
                Executable {
                    Assertions
                        .assertThat(
                            dokumenter,
                        ).`as`("dokumenter")
                        .contains(
                            DokumentIntern("dokref", null, "tittel"),
                        )
                },
            )
        }
    }

    @Nested
    @DisplayName("og avvik")
    internal inner class Avvik {
        @ParameterizedTest
        @DisplayName("skal starte avviksbehandling uten at feil oppstår")
        @EnumSource(
            Avvikstype::class,
        )
        fun skalStarteAvviksbehandlingUtenAtFeilOppstar(avvikstype: Avvikstype?) {
            journalpost.journalforendeEnhet = "1234"
            Assertions
                .assertThat(
                    journalpost.startAvviksbehandling(
                        enAvvikshendelse().med(avvikstype).byggAvvikshendelseIntern(),
                    ),
                ).isNotNull()
        }

        @Nested
        @DisplayName("ARKIVERE_JOURNALPOST")
        internal inner class Arkivering {
            @Test
            @DisplayName(
                "skal kaste SakIkkeTilknyttetJournalpostException hvis oppgitt saksnummer ikke er tilknyttet reservert journalpost",
            )
            fun skalKasteSakIkkeTilknyttetJournalpostExceptionHvisSakManglerRelasjonTilReservertJournalpost() {
                val saksnr = "1900000"
                journalpost.journalstatus = Journalstatus.KLAR_TIL_PRINT
                Assertions
                    .assertThatExceptionOfType(
                        SakIkkeTilknyttetJournalpostException::class.java,
                    ).isThrownBy {
                        journalpost.startAvviksbehandling(
                            enAvvikshendelse()
                                .med(Avvikstype.ARKIVERE_JOURNALPOST)
                                .med(JoarkArkiveringStatus.STARTET)
                                .medSaksnummer(saksnr)
                                .byggAvvikshendelseIntern(),
                        )
                    }.withMessage(
                        "Sak med saksnummer %s mangler kobling til oppgitt journalpost",
                        saksnr,
                    )
            }

            @Test
            @DisplayName("skal kaste SaksnummerManglerException dersom saksnummer ikke er oppgitt i request")
            fun skalKasteSaksnummerManglerExceptionDersomSaksnummerIkkeErOppgittIRequest() {
                journalpost.journalstatus = Journalstatus.KLAR_TIL_PRINT
                Assertions
                    .assertThatExceptionOfType(
                        SaksnummerManglerException::class.java,
                    ).isThrownBy {
                        journalpost.startAvviksbehandling(
                            enAvvikshendelse()
                                .med(Avvikstype.ARKIVERE_JOURNALPOST)
                                .med(JoarkArkiveringStatus.STARTET)
                                .medSaksnummer(null)
                                .byggAvvikshendelseIntern(),
                        )
                    }.withMessage("Saksnummer mangler i request!")
            }

            @Test
            @DisplayName("skal gi ugyldig avviksbehandling dersom journalpost ikke er reservert")
            fun skalGiUgyldigAvviksbehandlingDersomJournalpostIkkeHarStatusReservert() {
                val saksnr = "1900000"
                journalpost.journalstatus = Journalstatus.MOTTAKSREGISTRERT
                val respons =
                    journalpost.startAvviksbehandling(
                        enAvvikshendelse()
                            .med(Avvikstype.ARKIVERE_JOURNALPOST)
                            .med(JoarkArkiveringStatus.STARTET)
                            .medSaksnummer(saksnr)
                            .byggAvvikshendelseIntern(),
                    )
                org.junit.jupiter.api.Assertions.assertAll(
                    Executable {
                        Assertions
                            .assertThat(respons)
                            .extracting { obj: Avviksbehandling -> obj.erGyldig() }
                            .isEqualTo(false)
                    },
                    Executable {
                        Assertions
                            .assertThat(respons)
                            .extracting(Avviksbehandling::avvikstype)
                            .isEqualTo(Avvikstype.ARKIVERE_JOURNALPOST)
                    },
                )
            }

            @Test
            @DisplayName("skal oppdatere ARKIVERING_STARTET dersom joarkArkiveringStatus har verdi STARTET")
            fun skalOppdatereArkiveringStartetDersomJoarkArkiveringStatusErStartet() {
                val unitTestStartTime = LocalDateTime.now()
                val saksnr = "1900000"
                journalpost.journalstatus = Journalstatus.KLAR_TIL_PRINT
                journalpost.journalsaker.add(Journalsak(saksnr))
                val respons =
                    journalpost.startAvviksbehandling(
                        enAvvikshendelse()
                            .med(Avvikstype.ARKIVERE_JOURNALPOST)
                            .med(JoarkArkiveringStatus.STARTET)
                            .medSaksnummer(saksnr)
                            .byggAvvikshendelseIntern(),
                    ) as GyldigAvviksbehandling
                org.junit.jupiter.api.Assertions.assertAll(
                    Executable {
                        Assertions
                            .assertThat(
                                journalpost.journalsaker[0].arkiveringStartet,
                            ).isAfterOrEqualTo(unitTestStartTime)
                    },
                    Executable {
                        Assertions
                            .assertThat(respons.avvikstype.name)
                            .isEqualTo(AvvikType.ARKIVERE_JOURNALPOST.name)
                    },
                )
            }

            @Test
            @DisplayName("skal oppdatere ARKIVERING_FEILET dersom joarkArkiveringStatus har verdi FEILET")
            fun skalOppdatereArkiveringFeiletDersomJoarkArkiveringStatusErFeilet() {
                val unitTestStartTime = LocalDateTime.now()
                val saksnr = "1900000"
                journalpost.journalstatus = Journalstatus.KLAR_TIL_PRINT
                journalpost.journalsaker.add(Journalsak(saksnr))
                val respons =
                    journalpost.startAvviksbehandling(
                        enAvvikshendelse()
                            .med(Avvikstype.ARKIVERE_JOURNALPOST)
                            .med(JoarkArkiveringStatus.FEILET)
                            .medSaksnummer(saksnr)
                            .byggAvvikshendelseIntern(),
                    ) as GyldigAvviksbehandling
                org.junit.jupiter.api.Assertions.assertAll(
                    Executable {
                        Assertions
                            .assertThat(
                                journalpost.journalsaker[0].arkiveringFeilet,
                            ).isAfterOrEqualTo(unitTestStartTime)
                    },
                    Executable {
                        Assertions
                            .assertThat(respons.avvikstype.name)
                            .isEqualTo(AvvikType.ARKIVERE_JOURNALPOST.name)
                    },
                )
            }

            @Test
            @DisplayName("skal oppdatere ARKIVERING_FULLFORT dersom joarkArkiveringStatus har verdi FULLFORT")
            fun skalOppdatereArkiveringFullfortDersomJoarkArkiveringStatusErFullfort() {
                val unitTestStartTime = LocalDateTime.now()
                val saksnr = "1900000"
                val joarkJpId = 123456
                journalpost.journalstatus = Journalstatus.KLAR_TIL_PRINT
                journalpost.journalsaker.add(Journalsak(saksnr))
                val respons =
                    journalpost.startAvviksbehandling(
                        enAvvikshendelse()
                            .med(Avvikstype.ARKIVERE_JOURNALPOST)
                            .med(JoarkArkiveringStatus.FULLFORT)
                            .medSaksnummer(saksnr)
                            .medJoarkJournalpostId(joarkJpId)
                            .byggAvvikshendelseIntern(),
                    ) as GyldigAvviksbehandling
                org.junit.jupiter.api.Assertions.assertAll(
                    Executable {
                        Assertions
                            .assertThat(
                                journalpost.journalsaker[0].arkiveringFullfort,
                            ).isAfterOrEqualTo(unitTestStartTime)
                    },
                    Executable {
                        Assertions
                            .assertThat(
                                journalpost.journalsaker[0].joarkJpId,
                            ).isEqualTo(joarkJpId)
                    },
                    Executable {
                        Assertions
                            .assertThat(respons.avvikstype.name)
                            .isEqualTo(AvvikType.ARKIVERE_JOURNALPOST.name)
                    },
                )
            }
        }

        @Nested
        @DisplayName("BESTILL_ORIGINAL")
        internal inner class BestillOriginal {
            @Test
            @DisplayName("skal ikke opprette Avvikstype.BESTILL_ORIGINAL på utgående dokument")
            fun skalIkkeOppretteAvvikBestillOriginalPaUtgaendeDokument() {
                journalpost.dokumentType = DokumentType.UTGAAENDE_DOKUMENT
                journalpost.setSkannetDato(LocalDate.now())
                journalpost.originalBestilt = false
                val avviksoppgave =
                    journalpost.startAvviksbehandling(
                        enAvvikshendelse().med(Avvikstype.BESTILL_ORIGINAL).byggAvvikshendelseIntern(),
                    )
                org.junit.jupiter.api.Assertions.assertAll(
                    Executable {
                        Assertions
                            .assertThat(journalpost.originalBestilt)
                            .`as`("original bestilt")
                            .isFalse()
                    },
                    Executable {
                        Assertions
                            .assertThat(avviksoppgave.hentOppgave())
                            .`as`("oppgave.journalpostId")
                            .isNotPresent()
                    },
                )
            }

            @Test
            @DisplayName("skal ikke opprette Avvikstype.BESTILL_ORIGINAL på inngående dokument som ikke er skannet")
            fun skalIkkeOppretteAvvikBestillOriginalPaInngaendeDokumentSomIkkeErSkannet() {
                journalpost.dokumentType = DokumentType.INNGAENDE_DOKUMENT
                journalpost.setSkannetDato(null)
                journalpost.originalBestilt = false
                val avviksoppgave =
                    journalpost.startAvviksbehandling(
                        enAvvikshendelse().med(Avvikstype.BESTILL_ORIGINAL).byggAvvikshendelseIntern(),
                    )
                org.junit.jupiter.api.Assertions.assertAll(
                    Executable {
                        Assertions
                            .assertThat(journalpost.originalBestilt)
                            .`as`("original bestilt")
                            .isFalse()
                    },
                    Executable {
                        Assertions
                            .assertThat(avviksoppgave.hentOppgave())
                            .`as`("oppgave.journalpostId")
                            .isNotPresent()
                    },
                )
            }

            @Test
            @DisplayName("skal ikke opprette Avvikstype.BESTILL_ORIGINAL når det allerede er utført")
            fun skalIkkeOppretteAvvikBestillOriginalNarAvvikAlleredeErUtfort() {
                journalpost.dokumentType = DokumentType.INNGAENDE_DOKUMENT
                journalpost.setSkannetDato(LocalDate.now())
                journalpost.originalBestilt = true
                val avviksoppgave =
                    journalpost.startAvviksbehandling(
                        enAvvikshendelse().med(Avvikstype.BESTILL_ORIGINAL).byggAvvikshendelseIntern(),
                    )
                Assertions.assertThat(avviksoppgave.hentOppgave()).isEmpty()
            }

            @Test
            @DisplayName("skal opprette Avvikstype.BESTILL_ORIGINAL")
            fun skalOppretteAvvikBestillOriginal() {
                journalpost.journalpostId = 101
                journalpost.dokumentType = DokumentType.INNGAENDE_DOKUMENT
                journalpost.setSkannetDato(LocalDate.now())
                journalpost.originalBestilt = false
                val avviksoppgave =
                    journalpost.startAvviksbehandling(
                        enAvvikshendelse().med(Avvikstype.BESTILL_ORIGINAL).byggAvvikshendelseIntern(),
                    )
                org.junit.jupiter.api.Assertions.assertAll(
                    Executable {
                        Assertions
                            .assertThat(journalpost.originalBestilt)
                            .`as`("original bestilt")
                            .isTrue()
                    },
                    Executable {
                        Assertions
                            .assertThat(avviksoppgave.hentOppgave())
                            .`as`("oppgave.journalpostId")
                            .isPresent()
                            .get()
                            .extracting(Oppgave::journalpostId)
                            .isEqualTo("101")
                    },
                )
            }
        }

        @Nested
        @DisplayName("BESTILL_RESKANNING")
        internal inner class BestillReskanning {
            @Test
            @DisplayName("skal mangle BESTILL_RESKANNING når ikke dokumentet er inngående")
            fun skalMangleBestilleReskanningNarIkkeDokumentetErInngaende() {
                journalpost.dokumentType = DokumentType.UTGAAENDE_DOKUMENT
                journalpost.setSkannetDato(LocalDate.now())
                val avvikstyper = journalpost.finnAvvik()
                Assertions.assertThat(avvikstyper).doesNotContain(Avvikstype.BESTILL_RESKANNING)
            }

            @Test
            @DisplayName("skal mangle BESTILL_RESKANNING når det inngående dokumentet ikke er skannet")
            fun skalMangleBestilleReskanningNarInngaendeDokumentIkkeErSkannet() {
                journalpost.dokumentType = DokumentType.INNGAENDE_DOKUMENT
                journalpost.setSkannetDato(null)
                val avvikstyper = journalpost.finnAvvik()
                Assertions.assertThat(avvikstyper).doesNotContain(Avvikstype.BESTILL_RESKANNING)
            }

            @Test
            @DisplayName("skal mangle BESTILL_RESKANNING når det er et elektronisk dokument (BJOARK-batch)")
            fun skalMangleBestilleReskanningNarDetErEtElektroniskDokument() {
                journalpost.dokumentType = DokumentType.INNGAENDE_DOKUMENT
                journalpost.setSkannetDato(LocalDate.now())
                journalpost.batchNavn = "BJOARK-007"
                val avvikstyper = journalpost.finnAvvik()
                Assertions.assertThat(avvikstyper).doesNotContain(Avvikstype.BESTILL_RESKANNING)
            }

            @Test
            @DisplayName("skal finne BESTILL_RESKANNING på inngående dokument er skannet")
            fun skalFinneBestilleReskanningNarInngaendeDokumentErSkannet() {
                journalpost.dokumentType = DokumentType.INNGAENDE_DOKUMENT
                journalpost.setSkannetDato(LocalDate.now())
                val avvikstyper = journalpost.finnAvvik()
                Assertions.assertThat(avvikstyper).contains(Avvikstype.BESTILL_RESKANNING)
            }

            @Test
            @DisplayName("skal lage ugyldig avviksoppgave for BESTILL_RESKANNING når avvik ikke finnes")
            fun skalLageUgyldigAvviksoppgaveForBestilleReskanningNarAvvikIkkeFinnes() {
                journalpost.dokumentType =
                    DokumentType.UTGAAENDE_DOKUMENT // må være inngående dokument
                val avviksoppgave =
                    journalpost.startAvviksbehandling(
                        enAvvikshendelse().med(Avvikstype.BESTILL_RESKANNING).byggAvvikshendelseIntern(),
                    )
                org.junit.jupiter.api.Assertions.assertAll(
                    Executable {
                        Assertions
                            .assertThat(avviksoppgave)
                            .extracting { obj: Avviksbehandling -> obj.erGyldig() }
                            .isEqualTo(false)
                    },
                    Executable {
                        Assertions
                            .assertThat(avviksoppgave)
                            .extracting(Avviksbehandling::avvikstype)
                            .isEqualTo(Avvikstype.BESTILL_RESKANNING)
                    },
                )
            }

            @Test
            @DisplayName("skal lage avviksoppgave for BESTILL_RESKANNING når avvik er oppfylt")
            fun skalLageAvviksoppgaveForBestilleReskanningNarAvvikErOppfylt() {
                journalpost.journalpostId = 101
                journalpost.dokumentType = DokumentType.INNGAENDE_DOKUMENT
                journalpost.setSkannetDato(LocalDate.now())
                val avviksoppgave =
                    journalpost.startAvviksbehandling(
                        enAvvikshendelse().med(Avvikstype.BESTILL_RESKANNING).byggAvvikshendelseIntern(),
                    )
                org.junit.jupiter.api.Assertions.assertAll(
                    Executable {
                        Assertions
                            .assertThat(avviksoppgave.avvikstype)
                            .`as`("avvikstype")
                            .isEqualTo(Avvikstype.BESTILL_RESKANNING)
                    },
                    Executable {
                        Assertions.assertThat(avviksoppgave).`as`("avviksbehandling").isInstanceOf(
                            GyldigAvviksbehandling::class.java,
                        )
                    },
                    Executable {
                        Assertions
                            .assertThat(avviksoppgave.hentOppgave())
                            .`as`("oppgave")
                            .isPresent()
                    },
                )
            }

            @Test
            @DisplayName("skal sette ny journalstatus når det er bestilling av reskanning")
            fun skalSetteNyJournalstatusNarDetErBestillingAvReskanning() {
                journalpost.journalpostId = 1
                journalpost.dokumentType = DokumentType.INNGAENDE_DOKUMENT
                journalpost.setSkannetDato(LocalDate.now())
                journalpost.startAvviksbehandling(
                    enAvvikshendelse().med(Avvikstype.BESTILL_RESKANNING).byggAvvikshendelseIntern(),
                )
                Assertions
                    .assertThat(journalpost.getJournalstatus())
                    .`as`("slettet journalpost")
                    .isEqualTo(
                        Journalstatus.AVVIK_BESTILL_RESKANNING,
                    )
            }

            @Test
            @DisplayName("skal gi ugyldig avviksbehandling ved bestilling av reskanning på et elektronisk dokument (BJOARK-batch)")
            fun skalGiUgyldigAvviksbehandlingVedBestillingAvReskanningPaEtElektroniskDokument() {
                journalpost.dokumentType = DokumentType.INNGAENDE_DOKUMENT
                journalpost.setSkannetDato(LocalDate.now())
                journalpost.batchNavn = "BJOARK-007"
                val avviksbehandling =
                    journalpost.startAvviksbehandling(
                        enAvvikshendelse().med(Avvikstype.BESTILL_RESKANNING).byggAvvikshendelseIntern(),
                    )
                Assertions.assertThat(avviksbehandling).isInstanceOf(
                    UgyldigAvviksbehandling::class.java,
                )
            }

            @Test
            @DisplayName("skal ikke feilfore andre journalsaker enn den som det bestilles reskanning på")
            fun skalIkkeFeilforeAndreSakerEnnDenSomDetBestillesReskanningPa() {
                journalpost.leggTil(Journalsak("1001")) // en annen sak er allerede lagt til
                journalpost.journalpostId = 1
                journalpost.leggTil(Journalsak("007"))
                journalpost.startGyldigBehandlingAvBestillReskanning(
                    AvvikshendelseIntern(
                        enAvvikshendelseFor(Avvikstype.BESTILL_RESKANNING)
                            .medSaksnummer("1001")
                            .bygg(),
                        "101",
                        1,
                    ),
                )
                org.junit.jupiter.api.Assertions.assertAll(
                    Executable {
                        Assertions
                            .assertThat(journalpost.journalsaker)
                            .`as`("journalsaker")
                            .hasSize(2)
                    },
                    Executable {
                        journalpost.journalsaker.forEach(
                            Consumer { js: Journalsak ->
                                if (js.getSaksnummer() == "1001") {
                                    Assertions
                                        .assertThat(js.erFeilfort())
                                        .`as`("feilført sak 1001")
                                        .isTrue()
                                } else {
                                    Assertions
                                        .assertThat(js.erFeilfort())
                                        .`as`("feilført sak " + js.getSaksnummer())
                                        .isFalse()
                                }
                            },
                        )
                    },
                )
            }
        }

        @Nested
        @DisplayName("BESTILL_SPLITTING")
        internal inner class BestillSpitting {
            @Test
            @DisplayName("skal mangle BESTILL_SPLITTING når det skannede inngående dokumentet ikke har et filnavn")
            fun skalMangleBestilleSplittingNarSkannedeInngaendeDokumentIkkeHarEtFilnavn() {
                journalpost.dokumentType = DokumentType.INNGAENDE_DOKUMENT
                journalpost.setSkannetDato(LocalDate.now())
                journalpost.filnavn = null
                journalpost.batchNavn = "IKKE BATCH FRA JOARK"
                val avvikstyper = journalpost.finnAvvik()
                Assertions.assertThat(avvikstyper).doesNotContain(Avvikstype.BESTILL_SPLITTING)
            }

            @Test
            @DisplayName("skal feilregistrere alle journalsaker på en journalpost ved BESTILL_SPLITTING")
            fun skalFeilregistrereJournalsakerTilJournalpostVedBestillingAvSplitting() {
                journalpost.batchNavn = "IKKE BATCH FRA JOARK"
                journalpost.dokumentType = DokumentType.INNGAENDE_DOKUMENT
                journalpost.fagomrade = Fagomrade.BIDRAG
                journalpost.filnavn = "jumbo.pdf"
                journalpost.journalpostId = 101
                journalpost.setSkannetDato(LocalDate.now())
                journalpost.leggTil(Journalsak("0000007"))
                journalpost.leggTil(Journalsak("0000008"))
                journalpost.startAvviksbehandling(
                    enAvvikshendelse()
                        .med(Avvikstype.BESTILL_SPLITTING)
                        .medBeskrivelse("Midt pp")
                        .byggAvvikshendelseIntern(),
                )
                journalpost.journalsaker.forEach(
                    Consumer { journalsak: Journalsak ->
                        Assertions
                            .assertThat(journalsak.erFeilfort())
                            .`as`("feilført journalsak: " + journalsak.getSaksnummer())
                            .isTrue()
                    },
                )
            }

            @Test
            @DisplayName("skal sette ny journalstatus når det er bestilling av splitting")
            fun skalSetteNyJournalstatusNarDetErBestillingAvSplitting() {
                journalpost.journalpostId = 1
                journalpost.dokumentType = DokumentType.INNGAENDE_DOKUMENT
                journalpost.filnavn = "fila"
                journalpost.batchNavn = "batchen"
                journalpost.setSkannetDato(LocalDate.now())
                journalpost.startAvviksbehandling(
                    enAvvikshendelse()
                        .med(Avvikstype.BESTILL_SPLITTING)
                        .medBeskrivelse("splitt her")
                        .byggAvvikshendelseIntern(),
                )
                Assertions
                    .assertThat(journalpost.getJournalstatus())
                    .`as`("slettet journalpost")
                    .isEqualTo(
                        Journalstatus.AVVIK_BESTILL_SPLITTING,
                    )
            }
        }

        @Nested
        @DisplayName("INNG_TIL_UTG_DOKUMENT")
        internal inner class InngaendeTilUtgaende {
            @Test
            @DisplayName("skal ikke finne INNG_TIL_UTG_DOKUMENT når journalposten er utgående")
            fun skalIkkeFinneFraInngTilUtgDokumentNarDokumentErUtgaende() {
                journalpost.dokumentType = DokumentType.UTGAAENDE_DOKUMENT
                val avvikstyper = journalpost.finnAvvik()
                Assertions.assertThat(avvikstyper).doesNotContain(Avvikstype.INNG_TIL_UTG_DOKUMENT)
            }

            @Test
            @DisplayName("skal finne INNG_TIL_UTG_DOKUMENT når journalposten er inngående")
            fun skalFinneFraInngTilUtgDokumentNarDokumentErInngaende() {
                journalpost.dokumentType = DokumentType.INNGAENDE_DOKUMENT
                val avvikstyper = journalpost.finnAvvik()
                Assertions.assertThat(avvikstyper).contains(Avvikstype.INNG_TIL_UTG_DOKUMENT)
            }

            @Test
            @DisplayName("skal ikke endre dokumenttype fra utgående til inngående")
            fun skalIkkeEndredokumenttypeFraUtgaendeTilInngaende() {
                journalpost.dokumentType = DokumentType.UTGAAENDE_DOKUMENT
                journalpost.startAvviksbehandling(
                    enAvvikshendelse()
                        .med(Avvikstype.INNG_TIL_UTG_DOKUMENT)
                        .byggAvvikshendelseIntern(),
                )
                Assertions
                    .assertThat(journalpost.getDokumentType())
                    .isEqualTo(DokumentType.UTGAAENDE_DOKUMENT)
            }

            @Test
            @DisplayName("skal sette riktig enhet fra inngående til utgående")
            fun skalSetteRiktigEnhetFraUtgaendeTilInngaende() {
                val opprettetAvEnhetsnummer = "1001"
                journalpost.dokumentType = DokumentType.INNGAENDE_DOKUMENT
                journalpost.journalforendeEnhet = "4806"
                journalpost.startAvviksbehandling(
                    enAvvikshendelse()
                        .med(Avvikstype.INNG_TIL_UTG_DOKUMENT)
                        .medOpprettetAvEnhet(opprettetAvEnhetsnummer)
                        .byggAvvikshendelseIntern(),
                )
                Assertions
                    .assertThat(journalpost.getJournalforendeEnhet())
                    .isEqualTo(opprettetAvEnhetsnummer)
            }

            @Test
            @DisplayName("skal endre dokumenttype fra inngående til utgående")
            fun skalEndredokumenttypeFraUtgaendeTilInngaende() {
                journalpost.dokumentType = DokumentType.INNGAENDE_DOKUMENT
                journalpost.startAvviksbehandling(
                    enAvvikshendelse()
                        .med(Avvikstype.INNG_TIL_UTG_DOKUMENT)
                        .byggAvvikshendelseIntern(),
                )
                Assertions.assertThat(journalpost.getDokumentType()).isEqualTo("U")
            }
        }

        @Nested
        @DisplayName("ENDRE_FAGOMRADE")
        internal inner class EndreFagomrade {
            @Test
            @DisplayName("skal endre fagområde ved start av avviksbehandling for ENDRE_FAGOMRADE")
            fun skalEndreFagomradeVedAvvikEndreFagomrade() {
                journalpost.fagomrade = "BNR"
                journalpost.journalforendeEnhet = "1234"
                journalpost.startAvviksbehandling(
                    enAvvikshendelse()
                        .med(Avvikstype.ENDRE_FAGOMRADE)
                        .medNyttFagomrade("FAR")
                        .byggAvvikshendelseIntern(),
                )
                Assertions.assertThat(journalpost.getFagomrade()).isEqualTo("FAR")
            }

            @ParameterizedTest
            @ValueSource(strings = ["", "BNR", "BID"]) // BID som argument er det samme som BNR i databasen
            @DisplayName("skal lage en ugyldig avviksbehandling ved dårlig input (avviksbeskrivelse) som argument")
            fun skalLageUgyldigAvviksbehandlingVedDarligInputSomArgument(fagomrade: String?) {
                journalpost.journalpostId = 1
                journalpost.fagomrade = "BNR"
                journalpost.journalforendeEnhet = "1234"
                val avviksbehandling =
                    journalpost.startAvviksbehandling(
                        enAvvikshendelse()
                            .med(Avvikstype.ENDRE_FAGOMRADE)
                            .medBeskrivelse(fagomrade)
                            .byggAvvikshendelseIntern(),
                    )
                org.junit.jupiter.api.Assertions.assertAll(
                    Executable {
                        Assertions
                            .assertThat(avviksbehandling)
                            .extracting { obj: Avviksbehandling -> obj.erGyldig() }
                            .isEqualTo(false)
                    },
                    Executable {
                        Assertions
                            .assertThat(avviksbehandling)
                            .extracting(Avviksbehandling::avvikstype)
                            .isEqualTo(Avvikstype.ENDRE_FAGOMRADE)
                    },
                )
            }

            @ParameterizedTest
            @ValueSource(strings = ["FAR", "BNR", "BID"]) // BID som argument er det samme som BNR i databasen
            @DisplayName("skal ikke sette journalstatus til utgår når det er endring av fagområde til BID/BNR eller FAR")
            fun skalIkkeSetteJournalstatusTilUtgaarVedEndringAvFagomradeSomErBidragEllerFarskap(
                fagomrade: String?,
            ) {
                journalpost.fagomrade = "FAG"
                journalpost.journalforendeEnhet = "1234"
                journalpost.startAvviksbehandling(
                    enAvvikshendelse()
                        .med(Avvikstype.ENDRE_FAGOMRADE)
                        .medBeskrivelse(fagomrade)
                        .byggAvvikshendelseIntern(),
                )
                Assertions
                    .assertThat(journalpost.getJournalstatus())
                    .`as`("utgått journalstatus")
                    .isNotEqualTo(
                        Journalstatus.UTGAR,
                    )
            }

            @Test
            @DisplayName("skal sette ny journalstatus når det er endring av fagområde til annet enn BID/BNR eller FAR")
            fun skalSetteNyJournalstatusNarDetErEndringAvFagomradeTilAnnetEnnBidragEllerFarskap() {
                journalpost.fagomrade = "BNR"
                journalpost.journalforendeEnhet = "1234"
                journalpost.startAvviksbehandling(
                    enAvvikshendelse()
                        .med(Avvikstype.ENDRE_FAGOMRADE)
                        .medNyttFagomrade("FAG")
                        .somErSendtScanning()
                        .byggAvvikshendelseIntern(),
                )
                Assertions
                    .assertThat(journalpost.getJournalstatus())
                    .`as`("slettet journalpost")
                    .isEqualTo(
                        Journalstatus.AVVIK_ENDRE_FAGOMRADE,
                    )
            }

            @ParameterizedTest
            @ValueSource(strings = ["FAR", "BNR", "BID"]) // BID som argument er det samme som BNR i databasen
            @DisplayName("skal ikke sette journalstatus til utgår når det er endring av fagområde til BID/BNR eller FAR")
            fun skalIkkeFeilforeJournalpostVedEndringAvFagomradeTilBidragEllerFarskap(fagomrade: String?) {
                journalpost.fagomrade = "FAG"
                journalpost.journalforendeEnhet = "1234"
                journalpost.startAvviksbehandling(
                    enAvvikshendelse()
                        .med(Avvikstype.ENDRE_FAGOMRADE)
                        .medBeskrivelse(fagomrade)
                        .byggAvvikshendelseIntern(),
                )
                Assertions
                    .assertThat(
                        journalpost.journalsaker
                            .stream()
                            .anyMatch { obj: Journalsak -> obj.erFeilfort() },
                    ).`as`("feilført")
                    .isFalse()
            }

            @Test
            @DisplayName("skal feilføre journalposten til det er endring av fagområde til annet enn BID/BNR eller FAR")
            fun skalFeilforeJournalpostVedEndringAvFagomradeTilAnnetEnnBidragEllerFarskap() {
                journalpost.fagomrade = "BNR"
                journalpost.leggTil(Journalsak("007"))
                journalpost.journalforendeEnhet = "1234"
                val avvikshendelseDetaljer =
                    enAvvikshendelse()
                        .med(Avvikstype.ENDRE_FAGOMRADE)
                        .medNyttFagomrade("FAG")
                        .somErSendtScanning()
                        .medSaksnummer(
                            journalpost.journalsaker
                                .iterator()
                                .next()
                                .getSaksnummer(),
                        ).byggAvvikshendelseIntern()
                journalpost.startAvviksbehandling(avvikshendelseDetaljer)
                Assertions
                    .assertThat(
                        journalpost.journalsaker
                            .stream()
                            .anyMatch { obj: Journalsak -> obj.erFeilfort() },
                    ).`as`("feilført")
                    .isTrue()
            }

            @Test
            @DisplayName("skal endre fagomrade fra BNR til FAR")
            fun skalEndreFagomradeFraBnrTilFar() {
                journalpost.fagomrade = "BNR"
                journalpost.journalforendeEnhet = "1234"
                journalpost.startAvviksbehandling(
                    enAvvikshendelse()
                        .med(Avvikstype.ENDRE_FAGOMRADE)
                        .medNyttFagomrade("FAR")
                        .byggAvvikshendelseIntern(),
                )
                Assertions.assertThat(journalpost.getFagomrade()).isEqualTo("FAR")
            }

            @Test
            @DisplayName("skal endre fagomrade fra FAR til BID/BNR")
            fun skalEndreFagomradeFraFarTilBnr() {
                journalpost.fagomrade = "FAR"
                journalpost.journalforendeEnhet = "1234"
                journalpost.startAvviksbehandling(
                    enAvvikshendelse()
                        .med(Avvikstype.ENDRE_FAGOMRADE)
                        .medNyttFagomrade(Fagomrade.BIDRAG)
                        .byggAvvikshendelseIntern(),
                )
                Assertions.assertThat(journalpost.getFagomrade()).isEqualTo("BNR")
            }

            @Test
            @DisplayName(
                "skal gi ugyldig avviksbehandling ved endring av fagområde som ikke er bidrag/farskap og bekreftetSendtScanning er false",
            )
            fun skalGiUgyldigAvviksbehandlingVedEndringAvFagomradeSomIkkeErForBrevlagerOgBekreftetSendtScanninErFalse() {
                journalpost.fagomrade = "FAR"
                journalpost.journalforendeEnhet = "1234"
                val avviksbehandling =
                    journalpost.startAvviksbehandling(
                        enAvvikshendelse()
                            .med(Avvikstype.ENDRE_FAGOMRADE)
                            .medBeskrivelse("ANNET")
                            .somIkkeErSendtScanning()
                            .byggAvvikshendelseIntern(),
                    )
                Assertions.assertThat(avviksbehandling).isInstanceOf(
                    UgyldigAvviksbehandling::class.java,
                )
            }

            @Test
            @DisplayName("skal ikke feilfore andre journalsaker enn den som det endres fagområde på")
            fun skalIkkeFeilforeAndreSakerEnnDenSomDetEndresFagomradePa() {
                journalpost.leggTil(Journalsak("007"))
                journalpost.leggTil(Journalsak("1001"))
                journalpost.startGyldigBehandlingAvEndreFagomrade(
                    enAvvikshendelse()
                        .med(Avvikstype.ENDRE_FAGOMRADE)
                        .medBeskrivelse("fagområde som ikke er FAR/BID")
                        .medNyttFagomrade("AAP")
                        .medSaksnummer("1001")
                        .byggAvvikshendelseIntern(),
                )
                org.junit.jupiter.api.Assertions.assertAll(
                    Executable {
                        Assertions
                            .assertThat(journalpost.journalsaker)
                            .`as`("journalsaker")
                            .hasSize(2)
                    },
                    Executable {
                        journalpost.journalsaker.forEach(
                            Consumer { js: Journalsak ->
                                if (js.getSaksnummer() == "1001") {
                                    Assertions
                                        .assertThat(js.erFeilfort())
                                        .`as`("feilført sak 1001")
                                        .isTrue()
                                } else {
                                    Assertions
                                        .assertThat(js.erFeilfort())
                                        .`as`("feilført sak " + js.getSaksnummer())
                                        .isFalse()
                                }
                            },
                        )
                    },
                )
            }
        }

        @Nested
        @DisplayName("FEILFORE_SAK")
        internal inner class FeilforeSak {
            @Test
            @DisplayName("skal ikke finne avvik for FEILFORE når journalpost er mottatt")
            fun skalIkkeFinneAvvikForFeilforeNarJournalpostErMottatt() {
                journalpost.journalstatus = Journalstatus.MOTTAKSREGISTRERT
                val avvik = journalpost.finnAvvik()
                Assertions.assertThat(avvik).doesNotContain(Avvikstype.FEILFORE_SAK)
            }

            @Test
            @DisplayName("skal ikke finne avvik for FEILFORE_SAK når journalpost er annet enn mottatt, men allerede feilført")
            fun skalIkkeFinneAvvikForFeilforeSakNarJournalpostSomErAnnetEnnMottattMenAlleredeErFeilfort() {
                journalpost = JournalpostBygger.enJournalpostSomErFeilfort("1001").hent()
                journalpost.journalstatus = "x"
                val avvik = journalpost.finnAvvik()
                Assertions.assertThat(avvik).doesNotContain(Avvikstype.FEILFORE_SAK)
            }

            @Test
            @DisplayName("skal finne avvik for FEILFORE_SAK når journalpost ikke er mottatt eller feilført")
            fun skalFinneAvvikForFeilforeSakNarJournalpostIkkeErMottattEllerFeilfort() {
                journalpost.journalstatus = "u"
                val avvik = journalpost.finnAvvik()
                Assertions.assertThat(avvik).contains(Avvikstype.FEILFORE_SAK)
            }

            @Test
            @DisplayName(
                "skal sette journalsak som feilført ved behandling av FEILFORE_SAK når journalpst ikke er mottatt og saken ikke er feilført fra før",
            )
            fun skalFeilforeSakNarBehandlingAvAvvikFeilforeSak() {
                journalpost.leggTil(Journalsak("1001001"))
                journalpost.journalstatus = "u"
                val avviksbehandling =
                    journalpost.startAvviksbehandling(
                        enAvvikshendelse()
                            .med(Avvikstype.FEILFORE_SAK)
                            .medSaksnummer("1001001")
                            .byggAvvikshendelseIntern(),
                    )
                org.junit.jupiter.api.Assertions.assertAll(
                    Executable {
                        Assertions
                            .assertThat(avviksbehandling)
                            .`as`("avviksbehandling")
                            .isInstanceOf(
                                GyldigAvviksbehandling::class.java,
                            )
                    },
                    Executable {
                        Assertions
                            .assertThat(
                                journalpost.journalsaker
                                    .stream()
                                    .anyMatch { obj: Journalsak -> obj.erFeilfort() },
                            ).`as`("feilført journalpost")
                            .isTrue()
                    },
                )
            }

            @Test
            @DisplayName("skal ikke sette journalsak som feilført når journalpost er annet enn mottatt men saksnummer ikke er riktig")
            fun skalIkkeFeilforeSakNarJournalpostErAnnetEnnMottattMenSaksnummerIkkeErRiktig() {
                journalpost.leggTil(Journalsak("1001001"))
                journalpost.journalstatus = "u"
                val avviksbehandling =
                    journalpost.startAvviksbehandling(
                        enAvvikshendelse()
                            .med(Avvikstype.FEILFORE_SAK)
                            .medBeskrivelse("1001002")
                            .byggAvvikshendelseIntern(),
                    )
                org.junit.jupiter.api.Assertions.assertAll(
                    Executable {
                        Assertions
                            .assertThat(avviksbehandling)
                            .extracting { obj: Avviksbehandling -> obj.erGyldig() }
                            .isEqualTo(false)
                    },
                    Executable {
                        Assertions
                            .assertThat(avviksbehandling)
                            .extracting(Avviksbehandling::avvikstype)
                            .`as`("avviksbehandling")
                            .isEqualTo(Avvikstype.FEILFORE_SAK)
                    },
                )
            }
        }

        @Nested
        @DisplayName("SLETT_JOURNALPOST")
        internal inner class SlettJournalpost {
            @Test
            @DisplayName("skal ikke finne SLETT_JOURNALPOST når journalposten ikke er under produksjon, journalstatus D")
            fun skalIkkeFinneSlettJournalpostNarJournalpostenIkkeErUnderProduksjon() {
                journalpost.leggTil(Journalsak("1001001"))
                journalpost.journalstatus = "x"
                val avvikstyper = journalpost.finnAvvik()
                Assertions.assertThat(avvikstyper).doesNotContain(Avvikstype.SLETT_JOURNALPOST)
            }

            @Test
            @DisplayName("skal ikke sette journalstatus til slettet ved behandling av SLETT_JOURNALPOST")
            fun skalSetteJournalstatusTilSlettetVedBehandlingAvSlettJournalpost() {
                journalpost.leggTil(Journalsak("1001001"))
                journalpost.journalstatus = Journalstatus.UNDER_PRODUKSJON
                journalpost.startAvviksbehandling(
                    enAvvikshendelse().med(Avvikstype.SLETT_JOURNALPOST).byggAvvikshendelseIntern(),
                )
                Assertions
                    .assertThat(journalpost)
                    .extracting { obj: Journalpost -> obj.getJournalstatus() }
                    .isEqualTo(Journalstatus.SLETTET)
            }
        }

        @ParameterizedTest
        @EnumSource(value = Avvikstype::class, names = ["FEILFORE_SAK", "SLETT_JOURNALPOST"])
        @DisplayName("skal være ugyldig avviksbehandling når det brukes journalstatus som ikke er gyldig for avvikstype")
        fun skalVareUgyldigAvviksbehandlingPaBegrensetJournalstatus(avvikstype: Avvikstype?) {
            journalpost.journalstatus = Journalstatus.MOTTAKSREGISTRERT
            val avviksbehandling =
                journalpost.startAvviksbehandling(
                    enAvvikshendelse().med(avvikstype).byggAvvikshendelseIntern(),
                )
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(avviksbehandling)
                        .extracting { obj: Avviksbehandling -> obj.erGyldig() }
                        .isEqualTo(false)
                },
            )
        }

        @Test
        @DisplayName("skal ikke finne avvik når saksnummer ikke er riktig")
        fun skalIkkeFinneAvvikNarSaksnummerIkkeErRiktig() {
            journalpost.leggTil(Journalsak("007"))
            val avviksliste = journalpost.finnAvvikForSaksnummer("666")
            Assertions.assertThat(avviksliste).isEmpty()
        }
    }

    @Nested
    @DisplayName("og mottaksregistrert journalpost")
    internal inner class MottaksregistrertJournalpost {
        @Test
        @DisplayName("skal bare journalføre ved angitt journalføring")
        fun skalBareJournalforeVedAngittJournalforing() {
            val endreJournalpostCommand =
                CommandBuilder()
                    .medGjelder("someone")
                    .tilEndreJournalpostCommand()
            val journalforeEndreJournalpostCommand =
                CommandBuilder()
                    .medGjelder(genererFødselsnummer())
                    .medSkalJournalfores()
                    .tilEndreJournalpostCommand()
            val journalpostMedJournalstatusX =
                JournalpostBygger
                    .enJournalpost()
                    .medJournalpostId(101)
                    .leggTilSaksnummer("1001")
                    .medJournalstatus("X")
                    .hent()
            val journalpostMottagsregistrert =
                JournalpostBygger
                    .enJournalpost()
                    .medJournalpostId(101)
                    .leggTilSaksnummer("1002")
                    .medJournalstatus(Journalstatus.MOTTAKSREGISTRERT)
                    .hent()
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(
                            journalpostMedJournalstatusX.endre(
                                EndreJournalpostCommandIntern(101, "4806", endreJournalpostCommand),
                            ),
                        ).extracting { obj: Journalpost -> obj.getJournalstatus() }
                        .isEqualTo("X")
                },
                Executable {
                    Assertions
                        .assertThat(
                            journalpostMottagsregistrert.endre(
                                EndreJournalpostCommandIntern(
                                    101,
                                    "4806",
                                    journalforeEndreJournalpostCommand,
                                ),
                            ),
                        ).extracting { obj: Journalpost -> obj.getJournalstatus() }
                        .isEqualTo("J")
                },
            )
        }

        @Test
        @DisplayName("skal bare oppdatere brukerid, journalforendeEnhet, og journalfortAv ved journalføring")
        fun skalBareOppdatereBrukeridJournalforendeEnhetOgJournalfortAvVedJournalforing() {
            val endreJournalpostCommand =
                EndreJournalpostCommand(
                    gjelder = "someone",
                    skalJournalfores = false,
                )
            val journalforeEndreJournalpostCommand =
                EndreJournalpostCommand(
                    gjelder = genererFødselsnummer(),
                    skalJournalfores = true,
                )
            val endreJournalpostCommandIntern =
                EndreJournalpostCommandIntern(101, "4806", endreJournalpostCommand)
            endreJournalpostCommandIntern.brukerId = "s123456"
            endreJournalpostCommandIntern.journalforendeEnhet = "4806"
            endreJournalpostCommandIntern.journalfortAv = "Parafin, Pelle"
            val journalpostSomIkkeSkalJournalfoeres =
                JournalpostBygger
                    .enJournalpost()
                    .medJournalpostId(101)
                    .medJournalstatus("X")
                    .leggTilSaksnummer("1001")
                    .hent()
                    .endre(endreJournalpostCommandIntern)
            endreJournalpostCommandIntern.skalJournalfores = true
            val journalpostSomSkalJournalfoeres =
                JournalpostBygger
                    .enJournalpost()
                    .medJournalpostId(101)
                    .medJournalstatus(
                        Journalstatus.MOTTAKSREGISTRERT,
                    ).leggTilSaksnummer("1002")
                    .hent()
                    .endre(endreJournalpostCommandIntern)
            org.junit.jupiter.api.Assertions.assertAll(
                Executable {
                    Assertions
                        .assertThat(journalpostSomIkkeSkalJournalfoeres.getJournalstatus())
                        .isEqualTo("X")
                },
                Executable {
                    Assertions.assertThat(journalpostSomIkkeSkalJournalfoeres.brukerid).isNull()
                },
                Executable {
                    Assertions
                        .assertThat(journalpostSomIkkeSkalJournalfoeres.getJournalforendeEnhet())
                        .isNotNull()
                },
                Executable {
                    Assertions
                        .assertThat(journalpostSomIkkeSkalJournalfoeres.getJournalfortAv())
                        .isNull()
                },
                Executable {
                    Assertions
                        .assertThat(journalpostSomSkalJournalfoeres.getJournalstatus())
                        .isEqualTo("J")
                },
                Executable {
                    Assertions
                        .assertThat(journalpostSomSkalJournalfoeres.brukerid)
                        .isEqualTo("s123456")
                },
                Executable {
                    Assertions
                        .assertThat(journalpostSomSkalJournalfoeres.getJournalfortAv())
                        .isEqualTo("Parafin, Pelle")
                },
            )
        }

        @Test
        @DisplayName("skal feile ved registrering av journalpost når det mangler gjelder på journalpost")
        fun skalFeileVedRegistreringAvJournalpostNarDetManglerGjelder() {
            journalpost.journalpostId = 101
            val endreJournalpostCommand =
                CommandBuilder()
                    .medSkalJournalfores()
                    .tilEndreJournalpostCommand()
            val registrerJournalpostCommandIntern =
                EndreJournalpostCommandIntern(101, "4806", endreJournalpostCommand)
            Assertions
                .assertThatExceptionOfType(
                    ViolationException::class.java,
                ).isThrownBy { journalpost.endre(registrerJournalpostCommandIntern) }
                .withMessageContaining("Kan ikke registrere journalpost når det mangler gjelder for sak")
        }

        @Test
        @DisplayName("skal feile ved registrering av journalpost og det mangler sak på journalpost")
        fun skalFeileVedRegistreringAvJournalpostOgDetManglerSak() {
            val endreJournalpostCommand =
                EndreJournalpostCommand(
                    skalJournalfores = true,
                )
            val endreJournalpostCommandIntern =
                EndreJournalpostCommandIntern(101, "4806", endreJournalpostCommand)
            val journalpost = Journalpost()
            journalpost.gjelder = "nobody is perfect"
            journalpost.journalpostId = 101
            Assertions
                .assertThatExceptionOfType(
                    ViolationException::class.java,
                ).isThrownBy { journalpost.endre(endreJournalpostCommandIntern) }
                .withMessageContaining("Kan ikke registrere journalpost uten sak")
        }

        @Test
        @DisplayName("skal bruke dokumentets sin tittel som beskrivelse")
        fun skalBrukeDokumentetsSinTittelSomBeskrivelse() {
            journalpost.journalpostId = 101
            val dokument = EndreDokument(null, null, null, "et sabla bra dokument")
            val endreJournalpostCommand =
                EndreJournalpostCommand(
                    gjelder = "dr. who",
                    skalJournalfores = false,
                    endreDokumenter = listOf(dokument),
                )
            val endreJournalpostCommandIntern =
                EndreJournalpostCommandIntern(101, "4806", endreJournalpostCommand)
            val registrertJournalpost = journalpost.endre(endreJournalpostCommandIntern)
            Assertions
                .assertThat(registrertJournalpost.getBeskrivelse())
                .isEqualTo("et sabla bra dokument")
        }

        @Test
        @DisplayName("skal feile når man registrerer en journalpost som allerede er journalført")
        fun skalFeileVedRegistreringAvEnJournalpostSomAlleredeErJournalfort() {
            journalpost.journalpostId = 1010
            journalpost.journalstatus = Journalstatus.JOURNALFORT
            journalpost.leggTil(Journalsak("007"))
            val endreJournalpostCommand =
                EndreJournalpostCommand(
                    gjelder = "something that matters",
                    skalJournalfores = true,
                )
            Assertions
                .assertThatExceptionOfType(
                    ViolationException::class.java,
                ).isThrownBy {
                    journalpost.endre(
                        EndreJournalpostCommandIntern(
                            1010,
                            "4806",
                            endreJournalpostCommand,
                        ),
                    )
                }.withMessageContaining("Journalpost med journalstatus J kan ikke journalføres")
        }

        @Test
        @DisplayName("skal sette brukerid og journalfortAv ved journalføring")
        fun skalSetteBrukerIdOgJournalFortAvVedJournalforing() {
            val saksbehandlersBrukerId = "s123456"
            val saksbehandlersNavn = "Tom Jones"
            val journalforendeEnhet = "4806"
            journalpost.journalpostId = 1011
            journalpost.journalstatus = Journalstatus.MOTTAKSREGISTRERT
            journalpost.leggTil(Journalsak("007"))
            val endreJournalpostCommand =
                EndreJournalpostCommand(
                    gjelder = "something that matters",
                    skalJournalfores = true,
                )
            val endreJournalpostCommandIntern =
                EndreJournalpostCommandIntern(1011, journalforendeEnhet, endreJournalpostCommand)
            endreJournalpostCommandIntern.brukerId = saksbehandlersBrukerId
            endreJournalpostCommandIntern.journalfortAv = saksbehandlersNavn
            val journalfortJournalpost = journalpost.endre(endreJournalpostCommandIntern)
            Assertions
                .assertThat(journalfortJournalpost.getJournalforendeEnhet())
                .isEqualTo(journalforendeEnhet)
            Assertions
                .assertThat(journalfortJournalpost.getJournalfortAv())
                .isEqualTo(saksbehandlersNavn)
            Assertions.assertThat(journalfortJournalpost.brukerid).isEqualTo(saksbehandlersBrukerId)
        }
    }

    companion object {
        private const val BLANK_STRENG = ""
    }
}
