package no.nav.bidrag.tilgangskontroll.tjeneste

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.commons.web.MdcConstants
import no.nav.bidrag.domene.enums.behandling.Behandlingstema
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.tilgangskontroll.konfigurasjon.UnleashFeatures
import no.nav.bidrag.tilgangskontroll.konsumer.MicrosoftGraphConsumer
import no.nav.bidrag.tilgangskontroll.konsumer.SakPipKonsumer
import no.nav.bidrag.tilgangskontroll.konsumer.TilgangsmaskinConsumer
import no.nav.bidrag.tilgangskontroll.model.graph.BrukerGrupperResponse
import no.nav.bidrag.tilgangskontroll.model.graph.Søknadsgruppe
import no.nav.bidrag.tilgangskontroll.model.kodeverk.Informasjonstilgang
import no.nav.bidrag.transport.sak.BidragssakPipDto
import no.nav.bidrag.transport.tilgang.Brukertilganger
import no.nav.bidrag.transport.tilgang.OpprinnelseTilgangsbeslutning
import no.nav.bidrag.transport.tilgang.TilgangskontrollResponse
import no.nav.bidrag.transport.tilgang.TilgangskontrollResponseDetaljer
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

private val LOGGER = KotlinLogging.logger {}

@Service
class TilgangskontrollService(
    private val sakPipKonsumer: SakPipKonsumer,
    private val tilgangsmaskinConsumer: TilgangsmaskinConsumer,
    private val microsoftGraphConsumer: MicrosoftGraphConsumer,
    private val kodeverkService: KodeverkService,
    @param:Value($$"${IDENTENTER_SOM_KAN_OPPRETTE_SAK_UTEN_BM}") private val identerSomKanOppretteSakUtenBm: List<String>,
) {
    fun sjekkTilgangSak(saksnr: String): TilgangskontrollResponse {
        MDC.put(MdcConstants.MDC_SAKSNUMMER, saksnr)
        val metadataPip = sakPipKonsumer.hentPipMetadata(saksnr)
        val lesetilgangSak = sjekkLesetilgangSakV2(metadataPip)
        val tilgangAlleRoller = sjekkTilgangAlleRollerV2(metadataPip.roller)

        return TilgangskontrollResponse(
            harTilgang = lesetilgangSak.harTilgang && tilgangAlleRoller.harTilgang,
            detaljer = tilgangAlleRoller.detaljer + lesetilgangSak.detaljer,
        )
    }

    fun sjekkTilgangPerson(personident: Personident): TilgangskontrollResponse = sjekkTilgangAlleRollerV2(listOf(personident.verdi))

    fun sjekkTilgangTema(
        tema: String,
        navIdent: String? = null,
    ): TilgangskontrollResponse = sjekkTilgangAlleRollerV2(emptyList(), tema, navIdent)

    fun sjekkTilgangOpprettSakUtenBm(): TilgangskontrollResponse {
        val navIdent = TokenUtils.hentSaksbehandlerIdent()
        if (identerSomKanOppretteSakUtenBm.contains(navIdent)) {
            return TilgangskontrollResponse(
                harTilgang = true,
                detaljer =
                listOf(
                    TilgangskontrollResponseDetaljer(
                        harTilgang = true,
                        begrunnelse = "Bruker $navIdent har tilgang til å opprette sak uten BM.",
                        opprinnelseTilgangsbeslutning = OpprinnelseTilgangsbeslutning.BIDRAG_TILGANGSKONTROLL,
                    ),
                ),
            )
        }
        return TilgangskontrollResponse(
            harTilgang = false,
            detaljer =
            listOf(
                TilgangskontrollResponseDetaljer(
                    harTilgang = false,
                    begrunnelse = "Bruker $navIdent har ikke tilgang til å opprette sak uten BM.",
                    opprinnelseTilgangsbeslutning = OpprinnelseTilgangsbeslutning.BIDRAG_TILGANGSKONTROLL,
                ),
            ),
        )
    }

    fun sjekkLesetilgangSakV2(sakPip: BidragssakPipDto): TilgangskontrollResponse {
        if (sakPip.avsluttet && !UnleashFeatures.TILGANG_TIL_AVSLUTTET_SAK.isEnabled) {
            val begrunnelse = "Ingen tilgang til sak ${sakPip.saksnummer}. Saken er avsluttet"
            LOGGER.info { begrunnelse }
            return TilgangskontrollResponse(
                harTilgang = false,
                listOf(
                    TilgangskontrollResponseDetaljer(
                        harTilgang = false,
                        begrunnelse = begrunnelse,
                        opprinnelseTilgangsbeslutning = OpprinnelseTilgangsbeslutning.BIDRAG_SAK_PIP,
                    ),
                ),
            )
        }
        return TilgangskontrollResponse(
            harTilgang = true,
            listOf(
                TilgangskontrollResponseDetaljer(
                    harTilgang = true,
                    begrunnelse = "Sak ${sakPip.saksnummer} er ikke avsluttet, tilgang antas å være gyldig",
                    opprinnelseTilgangsbeslutning = OpprinnelseTilgangsbeslutning.BIDRAG_SAK_PIP,
                ),
            ),
        )
    }

    fun sjekkTilgangAlleRollerV2(
        roller: List<String>,
        tema: String? = null,
        saksbehandlerNavIdent: String? = null,
    ): TilgangskontrollResponse {
        if (TokenUtils.erApplikasjonsbruker() && saksbehandlerNavIdent == null) {
            LOGGER.debug { "Token er Azure client credentials token, hopper over tilgangskontroll." }
            return TilgangskontrollResponse(
                harTilgang = true,
                detaljer =
                listOf(
                    TilgangskontrollResponseDetaljer(
                        harTilgang = true,
                        begrunnelse = "Token er Azure client credentials token, hopper over tilgangskontroll.",
                        opprinnelseTilgangsbeslutning = OpprinnelseTilgangsbeslutning.BIDRAG_TILGANGSKONTROLL,
                    ),
                ),
            )
        }

        val navIdent = saksbehandlerNavIdent ?: TokenUtils.hentSaksbehandlerIdent()
        secureLogger.debug { "Utfører tilgangskontroll for roller $roller og saksbehandler $navIdent og tema $tema" }

        val tilgangsmaskin = sjekkTilgangTilgangsmaskinV2(roller)
        val tematilgang = sjekkTematilgangV2(tema, saksbehandlerNavIdent)

        return TilgangskontrollResponse(
            harTilgang = tilgangsmaskin.harTilgang && tematilgang.harTilgang,
            detaljer = listOf(tilgangsmaskin, tematilgang),
        )
    }

    fun sjekkTilgangSøknadsgruppe(
        søknadsgruppe: Søknadsgruppe,
        navident: String?,
    ): TilgangskontrollResponse {
        val navident = TokenUtils.hentSaksbehandlerIdent() ?: navident

        if (navident.isNullOrBlank()) {
            return TilgangskontrollResponse(
                harTilgang = false,
                detaljer =
                listOf(
                    TilgangskontrollResponseDetaljer(
                        harTilgang = false,
                        begrunnelse =
                        "Ingen saksbehandler ident funnet i token, kan ikke hente søknadsgrupper for bruker. " +
                            "Tilgang antas som ugyldig.",
                        opprinnelseTilgangsbeslutning = OpprinnelseTilgangsbeslutning.BIDRAG_TILGANGSKONTROLL,
                    ),
                ),
            )
        }

        val brukergrupper = microsoftGraphConsumer.hentGrupperForBruker(navident)
        søknadsgruppe.enheter.forEach { enhet ->
            brukergrupper?.value?.find { it.navn == "0000-GA-ENHET_$enhet" }?.let {
                return TilgangskontrollResponse(
                    harTilgang = true,
                    detaljer =
                    listOf(
                        TilgangskontrollResponseDetaljer(
                            harTilgang = true,
                            begrunnelse = "Bruker har tilgang til søknadsgruppe ${søknadsgruppe.name}",
                            opprinnelseTilgangsbeslutning = OpprinnelseTilgangsbeslutning.GRAPH,
                        ),
                    ),
                )
            }
        }
        return TilgangskontrollResponse(
            harTilgang = false,
            detaljer =
            listOf(
                TilgangskontrollResponseDetaljer(
                    harTilgang = false,
                    begrunnelse = "Bruker har ikke tilgang til søknadsgruppe ${søknadsgruppe.name}",
                    opprinnelseTilgangsbeslutning = OpprinnelseTilgangsbeslutning.GRAPH,
                ),
            ),
        )
    }

    private fun sjekkTematilgangV2(
        tema: String?,
        saksbehandlerNavIdent: String?,
    ): TilgangskontrollResponseDetaljer {
        if (tema.isNullOrBlank()) {
            return TilgangskontrollResponseDetaljer(
                harTilgang = true,
                begrunnelse = "Ingen tema angitt, tilgang antas å være gyldig",
                opprinnelseTilgangsbeslutning = OpprinnelseTilgangsbeslutning.BIDRAG_TILGANGSKONTROLL,
            )
        }

        val navident = TokenUtils.hentSaksbehandlerIdent() ?: saksbehandlerNavIdent

        if (navident.isNullOrBlank()) {
            return TilgangskontrollResponseDetaljer(
                harTilgang = false,
                begrunnelse = "Ingen saksbehandler ident funnet i token, tilgang til tema 0000-GA-TEMA_$tema antas å være ugyldig.",
                opprinnelseTilgangsbeslutning = OpprinnelseTilgangsbeslutning.BIDRAG_TILGANGSKONTROLL,
            )
        }

        microsoftGraphConsumer
            .hentGrupperForBruker(navident)
            ?.value
            ?.find { it.navn == "0000-GA-TEMA_$tema" }
            ?.let {
                return TilgangskontrollResponseDetaljer(
                    harTilgang = true,
                    begrunnelse = "Bruker har tilgang til tema 0000-GA-TEMA_$tema",
                    opprinnelseTilgangsbeslutning = OpprinnelseTilgangsbeslutning.GRAPH,
                )
            }

        LOGGER.info { "Bruker har ikke tilgang til tema 0000-GA-TEMA_$tema." }
        return TilgangskontrollResponseDetaljer(
            false,
            "Bruker har ikke tilgang til tema 0000-GA-TEMA_$tema.",
            OpprinnelseTilgangsbeslutning.GRAPH,
        )
    }

    private fun sjekkTilgangTilgangsmaskinV2(roller: List<String>): TilgangskontrollResponseDetaljer {
        val filtrerteRoller = roller.filter { it.isNotBlank() }.map { it.trim() }
        if (filtrerteRoller.isEmpty()) {
            return TilgangskontrollResponseDetaljer(
                harTilgang = true,
                begrunnelse = "Ingen roller angitt, tilgang antas å være gyldig",
                opprinnelseTilgangsbeslutning = OpprinnelseTilgangsbeslutning.TILGANGSMASKIN,
            )
        }

        val tilgangsmaskinResponse = tilgangsmaskinConsumer.evaluerKomplettRegelsettForFlereBrukere(filtrerteRoller)
        tilgangsmaskinResponse.resultater.forEach { resultat ->

            if (resultat.status == 403) {
                secureLogger.info {
                    "${resultat.detaljer?.navIdent} har ikke tilgang. Begrunnelse: ${resultat.detaljer?.begrunnelse}"
                }
                return TilgangskontrollResponseDetaljer(
                    false,
                    resultat.detaljer?.begrunnelse ?: "Du har ikke tilgang til bruker..",
                    OpprinnelseTilgangsbeslutning.TILGANGSMASKIN,
                )
            }
            if (resultat.status == 404) {
                secureLogger.info { "Person ${resultat.brukerId} ikke funnet i tilgangsmaskinen, tilgang antas som å være gyldig" }
                return TilgangskontrollResponseDetaljer(
                    true,
                    "Person ikke funnet i tilgangsmaskinen.",
                    OpprinnelseTilgangsbeslutning.TILGANGSMASKIN,
                )
            } else {
                secureLogger.debug {
                    "Tilgangsmaskin har svar med status: ${resultat.status} for navident: ${resultat.detaljer?.navIdent}. Begrunnelse: ${resultat.detaljer?.begrunnelse}"
                }
                return TilgangskontrollResponseDetaljer(
                    true,
                    resultat.detaljer?.begrunnelse
                        ?: "Mangler begrunnelse fra tilgangsmaskin. Status: ${resultat.status}",
                    OpprinnelseTilgangsbeslutning.TILGANGSMASKIN,
                )
            }
        }
        return TilgangskontrollResponseDetaljer(
            harTilgang = true,
            begrunnelse = "Bruker har tilgang til etterspurte roller.",
            opprinnelseTilgangsbeslutning = OpprinnelseTilgangsbeslutning.TILGANGSMASKIN,
        )
    }

    fun hentBrukertilganger(): Brukertilganger {
        val brukerAdgrupper: List<String> = hentAdgrupper()

        return Brukertilganger(
            bisysTilgang = brukerAdgrupper.contains(kodeverkService.hentAdgruppe(Informasjonstilgang.BISYS)),
            utlandTilgang = brukerAdgrupper.contains(kodeverkService.hentAdgruppe(Informasjonstilgang.UTLAND)),
            leseSakTilgang = brukerAdgrupper.contains(kodeverkService.hentAdgruppe(Informasjonstilgang.LESE)),
            behandleSakTilgang = brukerAdgrupper.contains(kodeverkService.hentAdgruppe(Informasjonstilgang.BEHANDLE)),
            foreldreskapTilgang = brukerAdgrupper.contains(kodeverkService.hentAdgruppe(Behandlingstema.FARSSKAP)),
            administrasjonTilgang = brukerAdgrupper.contains(kodeverkService.hentAdgruppe(Informasjonstilgang.ADMINISTRASJON)),
            behandlingstemaer = hentAlleBehandlingstemaerTilBruker(brukerAdgrupper),
        )
    }

    private fun hentAdgrupper(): List<String> {
        val navident = TokenUtils.hentSaksbehandlerIdent()
        val brukerGrupper = microsoftGraphConsumer.hentGrupperForBruker(navident)
        val brukerAdgrupper: List<String> = hentAlleAdgrupperForBruker(brukerGrupper)
        return brukerAdgrupper
    }

    fun sjekkLesetilgangTilBehandlingstema(behandlingstema: List<Behandlingstema>): TilgangskontrollResponse {
        val brukersAdgrupper: List<String> = hentAdgrupper()
        val harTilgangTilBisys = brukersAdgrupper.contains(kodeverkService.hentAdgruppe(Informasjonstilgang.BISYS))
        if (!harTilgangTilBisys) {
            return TilgangskontrollResponse(
                harTilgang = false,
                detaljer =
                listOf(
                    TilgangskontrollResponseDetaljer(
                        harTilgang = false,
                        begrunnelse = "Bruker mangler tilgang til Bisys.",
                        opprinnelseTilgangsbeslutning = OpprinnelseTilgangsbeslutning.BIDRAG_TILGANGSKONTROLL,
                    ),
                ),
            )
        }

        val harLeseTilgang = brukersAdgrupper.contains(kodeverkService.hentAdgruppe(Informasjonstilgang.LESE))
        if (!harLeseTilgang) {
            return TilgangskontrollResponse(
                harTilgang = false,
                detaljer =
                listOf(
                    TilgangskontrollResponseDetaljer(
                        harTilgang = false,
                        begrunnelse = "Bruker mangler lesetilgang.",
                        opprinnelseTilgangsbeslutning = OpprinnelseTilgangsbeslutning.BIDRAG_TILGANGSKONTROLL,
                    ),
                ),
            )
        }

        val manglendeTemaer =
            behandlingstema.filter { tema ->
                !brukersAdgrupper.contains(kodeverkService.hentAdgruppe(tema))
            }

        return TilgangskontrollResponse(
            harTilgang = manglendeTemaer.isEmpty(),
            detaljer =
            listOf(
                TilgangskontrollResponseDetaljer(
                    harTilgang = manglendeTemaer.isEmpty(),
                    begrunnelse =
                    if (manglendeTemaer.isEmpty()) {
                        "Bruker har lesetilgang til alle etterspurte behandlingstemaer."
                    } else {
                        "Bruker mangler lesetilgang til behandlingstema: ${manglendeTemaer.joinToString()}."
                    },
                    opprinnelseTilgangsbeslutning = OpprinnelseTilgangsbeslutning.BIDRAG_TILGANGSKONTROLL,
                ),
            ),
        )
    }

    fun sjekkSkrivetilgangTilBehandlingstema(behandlingstema: List<Behandlingstema>): TilgangskontrollResponse {
        val brukersAdgrupper: List<String> = hentAdgrupper()

        val harTilgangTilBisys = brukersAdgrupper.contains(kodeverkService.hentAdgruppe(Informasjonstilgang.BISYS))
        if (!harTilgangTilBisys) {
            return TilgangskontrollResponse(
                harTilgang = false,
                detaljer =
                listOf(
                    TilgangskontrollResponseDetaljer(
                        harTilgang = false,
                        begrunnelse = "Bruker mangler tilgang til Bisys.",
                        opprinnelseTilgangsbeslutning = OpprinnelseTilgangsbeslutning.BIDRAG_TILGANGSKONTROLL,
                    ),
                ),
            )
        }

        val harBehandlingstilgang = brukersAdgrupper.contains(kodeverkService.hentAdgruppe(Informasjonstilgang.BEHANDLE))
        if (!harBehandlingstilgang) {
            return TilgangskontrollResponse(
                harTilgang = false,
                detaljer =
                listOf(
                    TilgangskontrollResponseDetaljer(
                        harTilgang = false,
                        begrunnelse = "Bruker mangler behandletilgang.",
                        opprinnelseTilgangsbeslutning = OpprinnelseTilgangsbeslutning.BIDRAG_TILGANGSKONTROLL,
                    ),
                ),
            )
        }

        val manglendeTemaer =
            behandlingstema.filter { tema ->
                !brukersAdgrupper.contains(kodeverkService.hentAdgruppe(tema))
            }

        return TilgangskontrollResponse(
            harTilgang = manglendeTemaer.isEmpty(),
            detaljer =
            listOf(
                TilgangskontrollResponseDetaljer(
                    harTilgang = manglendeTemaer.isEmpty(),
                    begrunnelse =
                    if (manglendeTemaer.isEmpty()) {
                        "Bruker har behandlingstilgang til alle etterspurte behandlingstemaer."
                    } else {
                        "Bruker mangler behandlingstilgang til behandlingstema: ${manglendeTemaer.joinToString()}."
                    },
                    opprinnelseTilgangsbeslutning = OpprinnelseTilgangsbeslutning.BIDRAG_TILGANGSKONTROLL,
                ),
            ),
        )
    }

    private fun hentAlleBehandlingstemaerTilBruker(brukerAdgrupper: List<String>): List<Behandlingstema> {
        // Filtrer behandlingstemaer basert på brukerens GA-koder
        val behhandlingstemaer =
            Behandlingstema.entries.filter { tema ->
                val adgruppe = kodeverkService.hentAdgruppe(tema)
                brukerAdgrupper.contains(adgruppe)
            }
        return behhandlingstemaer
    }

    private fun hentAlleAdgrupperForBruker(brukerGrupper: BrukerGrupperResponse?): List<String> {
        val brukerAdgrupper: List<String> =
            brukerGrupper
                ?.value
                ?.filter { it.navn?.startsWith("0000-GA-") == true }
                ?.map { it.navn!! }
                ?: emptyList()
        return brukerAdgrupper
    }
}
