package no.nav.bidrag.dokument.journalpost.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDate;
import no.nav.bidrag.dokument.journalpost.model.Enhet;
import no.nav.bidrag.dokument.journalpost.model.SaksbehandlersEnhet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OppgaveTest {

  @Test
  @DisplayName("skal lage en oppgave fra kotlin dataklasse og er berike den med enhetsinformasjone")
  void skalLageOppgaveDataKlasseBeriketMedEnhetsinformasjon() {
    var oppgave = new BestillOriginalOppgave(
        101, null, null, null, "123213", new SaksbehandlersEnhet("69")
    );

    oppgave.berikMed(new Enhet("Enheten", "1", "EN"));

    assertThat(oppgave.asJson()).contains("enhet 1 - Enheten");
  }

  @Test
  @DisplayName("skal lage en oppgave fra kotlin dataklasse og berike den med saksbehandlirinformasjon")
  void skalLageOppgaveDataKlasseBerikhetMedSaksbehandlerInformasjon() {
    var oppgave = new BestillOriginalOppgave(
        101, null, null, null, "123213", new SaksbehandlersEnhet("69")
    );

    oppgave.berikMed(new Saksbehandler("x123456", "S. Vin Dyr"));

    assertThat(oppgave.asJson()).contains("x123456 - S. Vin Dyr");
  }

  @Test
  @DisplayName("skal lage en BestillOrginalOppgave for kotlin dataklasse som blir beriket med informasjon")
  void skalLageOppgaveDataKlasseBerikhetMedInformasjon() {
    var bestillOriginalOppgave = new BestillOriginalOppgave(
        101,
        "dokref",
        LocalDate.now(),
        "batchen", "123213",
        new SaksbehandlersEnhet("1001")
    );

    bestillOriginalOppgave.berikMed(new Enhet("Enheten", "1", "EN"));
    bestillOriginalOppgave.berikMed(new Saksbehandler("x123456", "S. Vin Dyr"));
    var beskrivelse = bestillOriginalOppgave.getBeskrivelse();

    assertAll(
        () -> assertThat(beskrivelse).contains("Originalbestilling: Vi ber om å få tilsendt papirdokumentet av vedlagte skannede dokument, se link."),
        () -> assertThat(beskrivelse).contains("Dokumentet ble skannet " + LocalDate.now()),
        () -> assertThat(beskrivelse).contains("med batchnavnet batchen"),
        () -> assertThat(beskrivelse).contains("enhet 1 - Enheten"),
        () -> assertThat(beskrivelse).contains("x123456 - S. Vin Dyr")
    );
  }

  @Test
  @DisplayName("skal inneholde prioriet og aktivDato")
  void skalInneholdePrioritetOgAktivDato() {
    var oppgave = new BestillReskanningOppgave(
        1001, "dokref", LocalDate.now().minusDays(1), "svada", null, "123213", new SaksbehandlersEnhet("1")
    );

    assertAll(
        () -> assertThat(oppgave.getPrioritet()).as("prioritet").isEqualTo("HOY"),
        () -> assertThat(oppgave.getAktivDato()).as("aktivDato").isEqualTo(LocalDate.now().toString())
    );
  }
}
