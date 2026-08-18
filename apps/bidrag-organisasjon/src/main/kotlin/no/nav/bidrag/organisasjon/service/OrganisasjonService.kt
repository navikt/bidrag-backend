package no.nav.bidrag.organisasjon.service

import no.nav.bidrag.commons.security.SikkerhetsKontekst.medApplikasjonKontekst
import no.nav.bidrag.commons.web.HttpResponse
import no.nav.bidrag.domene.enums.diverse.Enhetsstatus
import no.nav.bidrag.domene.enums.diverse.Språk
import no.nav.bidrag.domene.enums.diverse.Tema
import no.nav.bidrag.domene.enums.person.Diskresjonskode
import no.nav.bidrag.domene.enums.person.Gradering
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.organisasjon.consumer.EntraAnsatt
import no.nav.bidrag.organisasjon.consumer.EntraConsumer
import no.nav.bidrag.organisasjon.consumer.Norg2Consumer
import no.nav.bidrag.organisasjon.consumer.PersonConsumer
import no.nav.bidrag.organisasjon.consumer.SkjermingConsumer
import no.nav.bidrag.organisasjon.consumer.dto.ArbeidsfordelingEnheterBestMatchRequest
import no.nav.bidrag.organisasjon.consumer.dto.ArbeidsfordelingEnheterRequest
import no.nav.bidrag.organisasjon.consumer.dto.SkjermingRequest
import no.nav.bidrag.organisasjon.dto.SaksbehandlerDto
import no.nav.bidrag.transport.organisasjon.BidragEnheterResponsDto
import no.nav.bidrag.transport.organisasjon.EnhetBrukerDto
import no.nav.bidrag.transport.organisasjon.EnhetDetaljerDto
import no.nav.bidrag.transport.organisasjon.EnhetDto
import no.nav.bidrag.transport.organisasjon.EnhetKontaktinfoDto
import no.nav.bidrag.transport.organisasjon.EnhetPostadresseDto
import no.nav.bidrag.transport.organisasjon.HentEnhetRequest
import no.nav.bidrag.transport.organisasjon.JournalførendeEnhetDto
import no.nav.bidrag.transport.person.Graderingsinfo
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class OrganisasjonService(
    private val entraConsumer: EntraConsumer,
    private val norg2Consumer: Norg2Consumer,
    private val personConsumer: PersonConsumer,
    private val skjermingConsumer: SkjermingConsumer,
) {
    fun hentEnhetInfo(enhetNr: Enhetsnummer): EnhetDto {
        LOGGER.info("Hent enhetinfo for enhetNr {}", enhetNr)
        val enhetInfoResponse = norg2Consumer.hentEnhetInfo(enhetNr)
        return EnhetDto(
            nummer = enhetInfoResponse.enhetNr,
            navn = enhetInfoResponse.navn,
            status = if (enhetInfoResponse.erNedlagt()) Enhetsstatus.NEDLAGT else Enhetsstatus.AKTIV,
        )
    }

    fun hentEnhetKontaktinformasjon(enhetNr: Enhetsnummer, spraak: String?): EnhetKontaktinfoDto {
        val enhetKontaktinfo = norg2Consumer.hentEnhetKontaktinfo(enhetNr.verdi)
        val språkEnum = try {
            spraak?.let { Språk.valueOf(spraak) } ?: Språk.NB
        } catch (e: Exception) {
            Språk.NB
        }

        if (enhetKontaktinfo == null) {
            LOGGER.info("Fant ingen kontakinformasjon for enhet {}. Returnerer standard kontaktinformasjon", enhetNr)
            return EnhetKontaktinfoDto.medStandardadresse(enhetNr)
        }

        LOGGER.info("Hentet kontakinformasjon for enhet {}", enhetNr)

        val result = enhetKontaktinfo.postadresse[språkEnum]?.let { enhetKontaktinfo.tilKontaktadresse(it) } ?: run {
            val norskAdresse = enhetKontaktinfo.postadresse[Språk.NB]
            val engelskAdresse = enhetKontaktinfo.postadresse[Språk.EN]
            if (språkEnum !in listOf(Språk.NB, Språk.NN, Språk.SV, Språk.DA) && engelskAdresse != null) {
                enhetKontaktinfo.tilKontaktadresse(engelskAdresse)!!
            } else {
                enhetKontaktinfo.tilKontaktadresse(norskAdresse) ?: EnhetKontaktinfoDto.medStandardadresse(enhetNr)
            }
        }
        LOGGER.info("Hentet kontakinformasjon for enhet $enhetNr med info $result")
        return result
    }

    private fun EnhetDetaljerDto.tilKontaktadresse(postadresseDto: EnhetPostadresseDto?): EnhetKontaktinfoDto? = postadresseDto?.let {
        EnhetKontaktinfoDto(
            nummer = Enhetsnummer(enhetId),
            telefonnummer = telefonnummer,
            navn = postadresseDto.navn ?: navn,
            postadresse = postadresseDto,
        )
    }
    fun hentSaksbehandlerInfo(saksbehandlerIdent: String): SaksbehandlerDto? {
        val entraAnsatt = entraConsumer.hentPersonInformasjon(saksbehandlerIdent)
        return if (entraAnsatt == null) {
            null
        } else {
            SaksbehandlerDto(saksbehandlerIdent, entraAnsatt.visningNavn)
        }
    }

    fun hentPersonerEnhet(enhet: String): List<EnhetBrukerDto> = medApplikasjonKontekst {
        LOGGER.info("Hent liste brukere som har tilgang til enhet $enhet")
        val personer = entraConsumer.hentBrukereForEnhet(enhet)
        personer.filter { harTilgangTilTemaBID(it) }.map { ansatt ->
            EnhetBrukerDto(ansatt.navIdent, ansatt.visningNavn)
        }
    }

    private fun harTilgangTilTemaBID(ansatt: EntraAnsatt): Boolean {
        val saksbehandlereMedBIDTilgang = entraConsumer.hentSaksbehandlereSOmHarTilgangTilTema(Tema.TEMA_BIDRAG.verdi)
        return saksbehandlereMedBIDTilgang.any { it.navIdent == ansatt.navIdent }
    }

    fun hentSaksbehandlerEnheter(saksbehandlerIdent: String): List<EnhetDto> = medApplikasjonKontekst {
        LOGGER.info("Hent liste over enheter en saksbehandler har tilgang til")
        val enheter = entraConsumer.hentPersonEnheter(saksbehandlerIdent)
        enheter.map { (enhetsnummer, navn) ->
            EnhetDto(
                nummer = Enhetsnummer(enhetsnummer),
                enhetIdent = Enhetsnummer(enhetsnummer),
                navn = navn,
                enhetNavn = navn,
            )
        }
    }

    fun hentArbeidsfordelingJournalforendeEnheter(): HttpResponse<List<JournalførendeEnhetDto>> {
        LOGGER.info("Hent liste over alle journaførende enheter fra arbeidsfordeling")
        val journalforendeEnheterParam = ArbeidsfordelingEnheterRequest(listOf(FORVALTNING, SPESIALENHETER, KLAGE), BIDRAG)
        val arbeidsfordelingResponse = norg2Consumer.finnArbeidsfordelingEnheterListe(journalforendeEnheterParam)
        val muligResponseBody = arbeidsfordelingResponse.responseEntity.body ?: listOf()

        val journalforendeEnhetDtoListe =
            muligResponseBody.map {
                JournalførendeEnhetDto(it.enhetNr, it.navn, mapTypeVerdi(it.type))
            }

        return if (journalforendeEnhetDtoListe.isEmpty()) {
            HttpResponse.from(HttpStatus.NO_CONTENT, null)
        } else {
            HttpResponse.from(HttpStatus.OK, journalforendeEnhetDtoListe)
        }
    }

    @JvmOverloads
    fun hentArbeidsfordelingGeografiskTilknytningEnheter(
        personIdent: Personident,
        tema: String? = null,
        behandlingstema: String? = null,
    ): EnhetDto? {
        val (ident, _, _, geografiskTilknytning, utland, diskresjonskode) = personConsumer.hentPersonGeografiskTilknytning(personIdent)

        val identErSkjermet = skjermingConsumer.erPersonSkjermet(SkjermingRequest(ident.verdi))
        val sokTema = if (!tema.isNullOrBlank()) tema else Tema.TEMA_BIDRAG.verdi

        val arbeidsfordelingParam =
            ArbeidsfordelingEnheterBestMatchRequest(
                behandlingstype = if (utland) BEHANDLINGSTYPE_UTLAND else null,
                diskresjonskode = diskresjonskode,
                tema = sokTema,
                geografiskOmraade = geografiskTilknytning,
                skjermet = identErSkjermet,
                behandlingstema = behandlingstema,
            )
        val arbeidsfordelingResponse = norg2Consumer.finnArbeidsfordelingEnheterBestMatch(arbeidsfordelingParam)

        return arbeidsfordelingResponse?.firstOrNull()?.let { EnhetDto(it.enhetNr, it.navn) }
    }

    fun hentArbeidsfordelingGeografiskTilknytningEnhet(request: HentEnhetRequest): EnhetDto? {
        val (_, _, _, geografiskTilknytning, utland, _) = personConsumer.hentPersonGeografiskTilknytning(request.ident)

        val graderingsinfo = personConsumer.hentGraderingsinfo(request.alleIdenter)
        val skjermet = graderingsinfo.identerTilSkjerming.values.contains(true)
        val diskresjonskode = finnStrengesteDiskresjonskode(graderingsinfo)

        val arbeidsfordelingParam =
            ArbeidsfordelingEnheterBestMatchRequest(
                diskresjonskode = diskresjonskode,
                tema = request.tema,
                geografiskOmraade = geografiskTilknytning,
                skjermet = skjermet,
                behandlingstema = request.behandlingstema ?: request.arbeidsfordeling?.behandlingstema,
                behandlingstype = mapBehandlingstype(request) ?: if (utland) BEHANDLINGSTYPE_UTLAND else null,
            )
        return norg2Consumer.finnArbeidsfordelingEnheterBestMatch(arbeidsfordelingParam)
            ?.firstOrNull()?.let { EnhetDto(it.enhetNr, it.navn) }
    }

    private fun finnStrengesteDiskresjonskode(graderingsinfo: Graderingsinfo): Diskresjonskode? {
        val graderinger = graderingsinfo.identerTilGradering.values.filterNotNull()
        val gradering =
            graderinger.firstOrNull { it == Gradering.STRENGT_FORTROLIG }
                ?: graderinger.firstOrNull { it == Gradering.STRENGT_FORTROLIG_UTLAND }
                ?: graderinger.firstOrNull { it == Gradering.FORTROLIG }
        return mapTilDiskresjonskode(gradering)
    }

    private fun mapTilDiskresjonskode(gradering: Gradering?): Diskresjonskode? = when (gradering) {
        Gradering.STRENGT_FORTROLIG -> Diskresjonskode.SPSF
        Gradering.FORTROLIG -> Diskresjonskode.SPFO
        Gradering.STRENGT_FORTROLIG_UTLAND -> Diskresjonskode.SPSF
        else -> null
    }

    private fun mapTypeVerdi(enhetType: String?): String {
        val enhetstypeverdi =
            when (enhetType) {
                FORVALTNING -> "Forvaltning"
                SPESIALENHETER -> "Spesialenheter"
                KLAGE -> "Klage"
                else -> enhetType!!
            }
        return enhetstypeverdi
    }

    private fun mapBehandlingstype(request: HentEnhetRequest): String? {
        if (request.behandlingstype == null) {
            return null
        }
        return request.sakskategori?.finnBehandlingstypekode(request.behandlingstype!!)
    }

    fun hentAlleEnheterGrupper(): BidragEnheterResponsDto = BidragEnheterResponsDto(EnhetYamlConverter.hentAlleEnheterGrupper())

    companion object {
        private val LOGGER = LoggerFactory.getLogger(OrganisasjonService::class.java)
        private const val FORVALTNING = "FPY"
        private const val SPESIALENHETER = "KO"
        private const val KLAGE = "KLAGE"
        private const val BIDRAG = "BID"
        const val BEHANDLINGSTYPE_UTLAND = "ae0106"
    }
}
