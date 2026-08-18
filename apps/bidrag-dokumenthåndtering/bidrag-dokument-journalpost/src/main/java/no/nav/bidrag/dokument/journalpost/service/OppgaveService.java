package no.nav.bidrag.dokument.journalpost.service;

import static no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpost.SECURE_LOGGER;

import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.bidrag.dokument.journalpost.consumer.OppgaveConsumer;
import no.nav.bidrag.dokument.journalpost.dto.Oppgave;
import no.nav.bidrag.dokument.journalpost.dto.OpprettOppgaveResponse;
import no.nav.bidrag.dokument.journalpost.exception.OppgaveIkkeOpprettetException;
import no.nav.bidrag.dokument.journalpost.model.Avviksbehandling;
import no.nav.bidrag.dokument.journalpost.model.Discriminator;
import no.nav.bidrag.dokument.journalpost.model.ResourceByDiscriminator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OppgaveService {

  private static final Logger LOGGER = LoggerFactory.getLogger(OppgaveService.class);

  private final ResourceByDiscriminator<OppgaveConsumer> oppgaveConsumers;

  public OppgaveService(ResourceByDiscriminator<OppgaveConsumer> oppgaveConsumers) {
    this.oppgaveConsumers = oppgaveConsumers;
  }

  HttpResponse<OpprettOppgaveResponse> opprettOppgave(Avviksbehandling avviksbehandling) {
    LOGGER.info("Oppretter oppgave {} for avviktype {}", avviksbehandling.getClass().getSimpleName(), avviksbehandling.getAvvikstype());
    SECURE_LOGGER.info("Oppretter oppgave {}:{}", avviksbehandling.getClass().getSimpleName(), asJson(avviksbehandling));

    return avviksbehandling.hentOppgave()
        .map(oppgave -> oppgaveConsumers.get(Discriminator.REGULAR_USER).opprett(oppgave))
        .orElseThrow(() -> new OppgaveIkkeOpprettetException(
            String.format("oppgave for avviksbehanding (%s) kunne ikke opprettes", avviksbehandling)
        ));
  }

  private String asJson(Avviksbehandling avviksbehandling) {
    return avviksbehandling.hentOppgave()
        .map(Oppgave::asJson)
        .map(json -> json.replaceAll(" {2}+", ""))
        .map(json -> json.replaceAll("\n", " "))
        .orElse("ingen oppgave finnes");
  }
}
