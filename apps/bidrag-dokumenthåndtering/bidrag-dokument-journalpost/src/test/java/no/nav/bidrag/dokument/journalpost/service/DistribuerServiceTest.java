package no.nav.bidrag.dokument.journalpost.service;

import static no.nav.bidrag.dokument.journalpost.entity.JournalpostBygger.enUtgaendeJournalpostKlarTilPrint;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Optional;
import no.nav.bidrag.dokument.journalpost.repository.JournalpostRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DistribuerServiceTest {

  @Mock
  JournalpostRepository repository;

  @Mock
  FeatureService featureService;

  @InjectMocks
  DistribuerService distribuerService;
  @Test
  public void skalValidereAtJournalpostKanDistribueres(){
    when(repository.findById(anyInt())).thenReturn(Optional.of(enUtgaendeJournalpostKlarTilPrint()
        .leggTilSaksnummer("123213")
        .medGjelder("123123")
        .medMottaker("123123")
        .medFagomrade("BID")
        .hent()));
    when(featureService.kanDistribuereJournalpost(anyString())).thenReturn(true);

    Assertions.assertDoesNotThrow(()->distribuerService.kanDistribuereJournalpost(2, ""));
  }

  @Nested
  @DisplayName("Skal validere at journalpost ikke kan distribueres")
  class ValiderJournalpostIkkeKanDistribueres {
    @Test
    @DisplayName("hvis journalpost har flere enn en saksnummer")
    public void hvisJournalpostHarFlereEnnEnSaksnummer(){
      when(repository.findById(anyInt())).thenReturn(Optional.of(enUtgaendeJournalpostKlarTilPrint()
          .leggTilSaksnummer("123213")
          .leggTilSaksnummer("213333")
          .medGjelder("123123")
          .medFagomrade("BID")
          .hent()));
      when(featureService.kanDistribuereJournalpost(anyString())).thenReturn(true);

      var exceptionResult = Assertions.assertThrows(IllegalArgumentException.class, ()->distribuerService.kanDistribuereJournalpost(2, ""));
      assertThat(exceptionResult.getMessage()).contains("tilknyttet sak");
    }

    @Test
    @DisplayName("hvis journalpost ikke har satt gjelder")
    public void hvisJournalpostIkkeHarGjelder(){
      when(repository.findById(anyInt())).thenReturn(Optional.of(enUtgaendeJournalpostKlarTilPrint()
          .leggTilSaksnummer("123213")
          .medGjelder(null)
          .medFagomrade("BID")
          .hent()));
      when(featureService.kanDistribuereJournalpost(anyString())).thenReturn(true);

      var exceptionResult = Assertions.assertThrows(IllegalArgumentException.class, ()->distribuerService.kanDistribuereJournalpost(2, ""));
      assertThat(exceptionResult.getMessage()).contains("Journalpost må ha satt gjelder");
    }

    @Test
    @DisplayName("hvis journalpost gjelder er samhandlerid starter med 8")
    public void hvisJournalpostGjelderErSamhandlerIdStarterMed8(){
      when(repository.findById(anyInt())).thenReturn(Optional.of(enUtgaendeJournalpostKlarTilPrint()
          .leggTilSaksnummer("123213")
          .medGjelder("81231232")
          .medFagomrade("BID")
          .hent()));
      when(featureService.kanDistribuereJournalpost(anyString())).thenReturn(true);

      var exceptionResult = Assertions.assertThrows(IllegalArgumentException.class, ()->distribuerService.kanDistribuereJournalpost(2, ""));
      assertThat(exceptionResult.getMessage()).contains("samhandler ident");
    }

    @Test
    @DisplayName("hvis journalpost gjelder er samhandlerid start med 9")
    public void hvisJournalpostGjelderErSamhandlerId_starterMed9(){
      when(repository.findById(anyInt())).thenReturn(Optional.of(enUtgaendeJournalpostKlarTilPrint()
          .leggTilSaksnummer("123213")
          .medGjelder("91231232")
          .medFagomrade("BID")
          .hent()));
      when(featureService.kanDistribuereJournalpost(anyString())).thenReturn(true);

      var exceptionResult = Assertions.assertThrows(IllegalArgumentException.class, ()->distribuerService.kanDistribuereJournalpost(2, ""));
      assertThat(exceptionResult.getMessage()).contains("samhandler ident");
    }

    @Test
    @DisplayName("hvis journalpost tema ikke er BID")
    public void hvisJournalpostTemaIkkeErBID(){
      when(repository.findById(anyInt())).thenReturn(Optional.of(enUtgaendeJournalpostKlarTilPrint()
          .leggTilSaksnummer("123213")
          .medGjelder("81231232")
          .medFagomrade("FAR")
          .hent()));
      when(featureService.kanDistribuereJournalpost(anyString())).thenReturn(true);

      var exceptionResult = Assertions.assertThrows(IllegalArgumentException.class, ()->distribuerService.kanDistribuereJournalpost(2, ""));
      assertThat(exceptionResult.getMessage()).contains("tema BID");
    }

    @Test
    @DisplayName("hvis journalpost ikke er utgående")
    public void hvisJournalpostIkkeErUtgaaende(){
      when(repository.findById(anyInt())).thenReturn(Optional.of(enUtgaendeJournalpostKlarTilPrint()
          .leggTilSaksnummer("123213")
          .medGjelder("81231232")
          .medFagomrade("BID")
          .medDokumentType("I")
          .hent()));
      when(featureService.kanDistribuereJournalpost(anyString())).thenReturn(true);

      var exceptionResult = Assertions.assertThrows(IllegalArgumentException.class, ()->distribuerService.kanDistribuereJournalpost(2, ""));
      assertThat(exceptionResult.getMessage()).contains("utgående");
    }

    @Test
    @DisplayName("hvis journalstatus ikke er KP")
    public void hvisJournalpostStatusIkkeErKlarTilPrint(){
      when(repository.findById(anyInt())).thenReturn(Optional.of(enUtgaendeJournalpostKlarTilPrint()
          .leggTilSaksnummer("123213")
              .medJournalstatus("J")
          .medGjelder("81231232")
          .medFagomrade("BID")
          .hent()));
      when(featureService.kanDistribuereJournalpost(anyString())).thenReturn(true);

      var exceptionResult = Assertions.assertThrows(IllegalArgumentException.class, ()->distribuerService.kanDistribuereJournalpost(2, ""));
      assertThat(exceptionResult.getMessage()).contains("KP");
    }

    @Test
    @DisplayName("hvis bruker ikke har tilgang")
    public void hvisBrukerIkkeHarTilgang(){
      when(repository.findById(anyInt())).thenReturn(Optional.of(enUtgaendeJournalpostKlarTilPrint()
          .leggTilSaksnummer("123213")
          .medGjelder("1231232")
          .medFagomrade("BID")
          .hent()));
      when(featureService.kanDistribuereJournalpost(anyString())).thenReturn(false);

      var exceptionResult = Assertions.assertThrows(IllegalArgumentException.class, ()->distribuerService.kanDistribuereJournalpost(2, ""));
      assertThat(exceptionResult.getMessage()).contains("Saksbehandler eller enhet må ha tilgang til å distribuere journalpost");
    }

    @Test
    @DisplayName("hvis mottaker og gjelder ikke er samme")
    public void hvisMottakerOgGjelderIkkeErSamme(){
      when(repository.findById(anyInt())).thenReturn(Optional.of(enUtgaendeJournalpostKlarTilPrint()
          .leggTilSaksnummer("123213")
          .medGjelder("12312532")
              .medMottaker("123213")
          .medFagomrade("BID")
          .hent()));
      when(featureService.kanDistribuereJournalpost(anyString())).thenReturn(true);

      var exceptionResult = Assertions.assertThrows(IllegalArgumentException.class, ()->distribuerService.kanDistribuereJournalpost(2, ""));
      assertThat(exceptionResult.getMessage()).contains("Mottaker og gjelder må være samme");
    }
  }


}
