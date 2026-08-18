package no.nav.bidrag.dokument.journalpost.service;

import static no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpost.SECURE_LOGGER;

import java.util.stream.Stream;
import no.nav.bidrag.dokument.journalpost.configuration.BidragDokumentJournalpostConfig.SaksbehandlerOidcTokenManager;
import no.nav.bidrag.dokument.journalpost.consumer.BidragPersonConsumer;
import no.nav.bidrag.dokument.journalpost.consumer.NorgConsumer;
import no.nav.bidrag.dokument.journalpost.consumer.SaksbehandlerConsumer;
import no.nav.bidrag.dokument.journalpost.entity.Journalpost;
import no.nav.bidrag.dokument.journalpost.exception.JournalpostHendelseException;
import no.nav.bidrag.dokument.journalpost.exception.JournalpostIkkeFunnetException;
import no.nav.bidrag.dokument.journalpost.model.Avviksbehandling;
import no.nav.bidrag.dokument.journalpost.model.Avvikstype;
import no.nav.bidrag.dokument.journalpost.model.BehandleAvvikRequest;
import no.nav.bidrag.dokument.journalpost.model.BehandleAvvikResponse;
import no.nav.bidrag.dokument.journalpost.model.Behandlingstype;
import no.nav.bidrag.dokument.journalpost.model.FinnAvvik;
import no.nav.bidrag.dokument.journalpost.model.GyldigAvviksbehandling;
import no.nav.bidrag.dokument.journalpost.model.HendelseData;
import no.nav.bidrag.dokument.journalpost.model.JournalHendelseForAvvik;
import no.nav.bidrag.dokument.journalpost.model.Journalstatus;
import no.nav.bidrag.dokument.journalpost.model.SaksbehandlersEnhet;
import no.nav.bidrag.dokument.journalpost.model.StatusAvviksbehandling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class AvvikService {

  private static final Logger LOGGER = LoggerFactory.getLogger(AvvikService.class);

  private final ApplicationEventPublisher applicationEventPublisher;
  private final KodeService kodeService;
  private final HendelseService hendelseService;
  private final JournalpostService journalpostService;
  private final OppgaveService oppgaveService;
  private final NorgConsumer norgConsumer;
  private final BidragPersonConsumer bidragPersonConsumer;
  private final SaksbehandlerConsumer saksbehandlerConsumer;
  private final SaksbehandlerOidcTokenManager saksbehandlerOidcTokenManager;
  private final TokenInformationService tokenInformationService;

  public AvvikService(
      ApplicationEventPublisher applicationEventPublisher,
      KodeService kodeService,
      HendelseService hendelseService,
      JournalpostService journalpostService,
      NorgConsumer norgConsumer,
      OppgaveService oppgaveService,
      BidragPersonConsumer bidragPersonConsumer, SaksbehandlerConsumer saksbehandlerConsumer,
      SaksbehandlerOidcTokenManager saksbehandlerOidcTokenManager,
      TokenInformationService tokenInformationService
  ) {
    this.applicationEventPublisher = applicationEventPublisher;
    this.kodeService = kodeService;
    this.hendelseService = hendelseService;
    this.journalpostService = journalpostService;
    this.oppgaveService = oppgaveService;
    this.norgConsumer = norgConsumer;
    this.bidragPersonConsumer = bidragPersonConsumer;
    this.saksbehandlerConsumer = saksbehandlerConsumer;
    this.saksbehandlerOidcTokenManager = saksbehandlerOidcTokenManager;
    this.tokenInformationService = tokenInformationService;
  }

  public FinnAvvik finnAvvik(Integer journalpostId) {
    var journalpost = hentJournalpost(journalpostId);

    if (Journalstatus.MOTTAKSREGISTRERT.equals(journalpost.getJournalstatus()) || kodeService.skalVise(journalpost.getJournalstatus())) {
      return new FinnAvvik(journalpost);
    }

    return new FinnAvvik();
  }

  public FinnAvvik finnAvvik(Integer journalpostId, String saksnummer) {
    var journalpost = hentJournalpost(journalpostId);

    if (kodeService.skalVise(journalpost.getJournalstatus())) {
      return new FinnAvvik(journalpost, saksnummer);
    }

    return new FinnAvvik();
  }

  private Journalpost hentJournalpost(Integer journalpostId) {
    return journalpostService.hentJournalpostEntitet(journalpostId)
        .orElseThrow(() -> new JournalpostIkkeFunnetException(String.format("Journalpost med id %d finnes ikke", journalpostId)));
  }

  public BehandleAvvikResponse behandleAvvik(BehandleAvvikRequest behandleAvvikRequest) {
    var behandlingstype = brukBehandlingForJournalfortMottaksregistrertEllerReservert(behandleAvvikRequest);

    return journalpostService.hentJournalpostEntitet(behandleAvvikRequest.hentJournalpostId())
        .map(jp -> behandleAvvik(jp, behandleAvvikRequest, behandlingstype))
        .orElseThrow(() -> new JournalpostIkkeFunnetException("Fant ikke journalpost med id lik " + behandleAvvikRequest.hentJournalpostId()));
  }

  private Behandlingstype brukBehandlingForJournalfortMottaksregistrertEllerReservert(BehandleAvvikRequest behandleAvvikRequest) {
    var behandlingstype = behandleAvvikRequest.harSaksnummer() ? Behandlingstype.JOURNALFORT : Behandlingstype.MOTTAKSREGISTRERT;

    if (behandleAvvikRequest.getAvvikshendelseIntern().getAvvikstype() == Avvikstype.ARKIVERE_JOURNALPOST) {
      behandlingstype = Behandlingstype.KLAR_TIL_PRINT;
    }

    return behandlingstype;
  }

  private BehandleAvvikResponse behandleAvvik(Journalpost journalpost, BehandleAvvikRequest behandleAvvikRequest, Behandlingstype behandlingstype) {
    if (behandleAvvikRequest.erForArkiveringAvJournalpost() && !journalpost.erGyldigJournalstatusForArkivering()) {
      return new BehandleAvvikResponse(StatusAvviksbehandling.ER_IKKE_KLAR_FOR_ARKIVERING);
    } else if (behandlingstype == Behandlingstype.MOTTAKSREGISTRERT && journalpost.erJournalstatusIkkeMottaksregistrert()) {
      return new BehandleAvvikResponse(StatusAvviksbehandling.ER_IKKE_MOTTAKSREGISTRERT);
    }

    var avvikshendelseIntern = behandleAvvikRequest.getAvvikshendelseIntern();
    var hendelseData = journalpost.leggTilHendelseData(avvikshendelseIntern);
    var avviksbehandling = journalpost.startAvviksbehandling(avvikshendelseIntern);

    if (avviksbehandling.erGyldig()) {
      var gyldigAvviksbehandling = ((GyldigAvviksbehandling) avviksbehandling)
          .leggTil(journalpost.hentFagomrade());

      var behandleAvvikResponse = Stream.of(gyldigAvviksbehandling)
          .map(this::berikMedEnhetsinformasjon)
          .map(this::berikMedAktoerId)
          .map(this::berikMedSaksbehandler)
          .map(this::opprettOppgaveTilSkanningssenteretVedBehov)
          .findFirst().orElseThrow(() -> new IllegalStateException("skal ikke forekomme"));

      int journalHendelseId = opprettJournalHendelse(behandleAvvikRequest, hendelseData, avviksbehandling);
      leggEventuellOppgaveIdPaJournalhendelsen(behandleAvvikResponse.hentOppgaveId(), journalHendelseId);
      publiserJournalpostHendelse(journalpost, avvikshendelseIntern.getSaksbehandlersEnhet());

      return behandleAvvikResponse;
    }

    return new BehandleAvvikResponse(avviksbehandling);
  }

  private int opprettJournalHendelse(BehandleAvvikRequest behandleAvvikRequest, HendelseData hendelseData, Avviksbehandling avviksbehandling) {
    return hendelseService.lagHendelseFor(new JournalHendelseForAvvik(
        behandleAvvikRequest.getAvvikshendelseIntern(),
        hendelseData,
        tokenInformationService.hentSaksbehandlersBrukerid(),
        avviksbehandling.hentEnhetForAvviksbehandling()
    ));
  }

  private void leggEventuellOppgaveIdPaJournalhendelsen(Long oppgaveId, int journalHendelseId) {
    if (oppgaveId != null) {
      var journalHendelse = hendelseService.hent(journalHendelseId).orElseThrow(
          () -> new JournalpostHendelseException("Kunne ikke finne JournhalHendelse med id: " + journalHendelseId)
      );

      journalHendelse.setOppgaveId(oppgaveId);
    }
  }

  private void publiserJournalpostHendelse(Journalpost journalpost, SaksbehandlersEnhet saksbehandlersEnhet) {
    applicationEventPublisher.publishEvent(journalpost.initJournalpostHendelse(saksbehandlersEnhet.getEnhetsnummer()));
  }

  private GyldigAvviksbehandling berikMedEnhetsinformasjon(GyldigAvviksbehandling gyldigAvviksbehandling) {
    if (gyldigAvviksbehandling.skalBerikeOppgaveMedEnhetsinformasjon()) {
      var muligEnhet = norgConsumer.hentEnhetsinformasjon(gyldigAvviksbehandling.hentEnhetsnummer());

      muligEnhet.ifPresent(gyldigAvviksbehandling::berikOppgaveMedEnhet);
    }

    return gyldigAvviksbehandling;
  }

  private GyldigAvviksbehandling berikMedAktoerId(GyldigAvviksbehandling gyldigAvviksbehandling) {
    gyldigAvviksbehandling.hentOppgave().ifPresent(o-> {
      if (o.hentGjelder() != null){
        var person = bidragPersonConsumer.hentPerson(o.hentGjelder());

        person.ifPresent(gyldigAvviksbehandling::berikOppgaveMedAktoerId);
      }
    });

    return gyldigAvviksbehandling;
  }

  private GyldigAvviksbehandling berikMedSaksbehandler(GyldigAvviksbehandling gyldigAvviksbehandling) {
    if (gyldigAvviksbehandling.skalBerikeOppgaveMedInformasjonOmSaksbehandler()) {
      var ident = saksbehandlerOidcTokenManager.hentSaksbehandler();
      var muligSaksbehandlerResponse = saksbehandlerConsumer.hentSaksbehandler(ident);

      muligSaksbehandlerResponse.ifPresent(gyldigAvviksbehandling::berikOppgaveMedSaksbehandler);
    }

    return gyldigAvviksbehandling;
  }

  private BehandleAvvikResponse opprettOppgaveTilSkanningssenteretVedBehov(GyldigAvviksbehandling gyldigAvviksbehandling) {
    if (gyldigAvviksbehandling.skalOppretteOppgave()) {
      LOGGER.info("Gyldig avviksbehandling med type {}", gyldigAvviksbehandling.getAvvikstype());
      SECURE_LOGGER.info("Gyldig avviksbehandling: {}", gyldigAvviksbehandling);
      return opprettOppgaveTilSkanningssenteret(gyldigAvviksbehandling);
    }

    LOGGER.info("Gyldig avviksbehandling uten oppgave med type {}", gyldigAvviksbehandling.getAvvikstype());
    SECURE_LOGGER.info("Gyldig avviksbehandling uten oppgave: {}", gyldigAvviksbehandling);
    return new BehandleAvvikResponse(gyldigAvviksbehandling);
  }

  private BehandleAvvikResponse opprettOppgaveTilSkanningssenteret(GyldigAvviksbehandling gyldigAvviksbehandling) {
    var httpResponse = oppgaveService.opprettOppgave(gyldigAvviksbehandling);

    return new BehandleAvvikResponse(
        gyldigAvviksbehandling.getAvvikstype(),
        httpResponse.fetchBody().orElse(null),
        httpResponse.is2xxSuccessful() ? StatusAvviksbehandling.GYLDIG : StatusAvviksbehandling.OPPRETT_OPPGAVE_FEILET
    );
  }
}
