package no.nav.bidrag.reisekostnad.integrasjon.bidrag.dokument.pdf;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import no.nav.bidrag.generer.testdata.person.PersonidentGeneratorKt;
import no.nav.bidrag.reisekostnad.api.dto.ut.PersonDto;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.doument.pdf.PdfGenerator;
import no.nav.bidrag.reisekostnad.tjeneste.støtte.Krypteringsverktøy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PdfGeneratorTest {
  private static boolean skriveUtPdf = false;

  // Krypteringsverktøy sin krypteringsnøkkel er statiske felt som normalt settes av Spring via
  // @Value-konstruktøren. Denne testen kjører uten Spring-kontekst, så uten dette oppsettet vil
  // Krypteringsverktøy.kryptere() kaste NullPointerException når testen kjøres isolert (f.eks.
  // med -Dtest=PdfGeneratorTest).
  @BeforeAll
  static void settOppKrypteringsnøkkel() {
    new Krypteringsverktøy("TopSecret", "saltySalt");
  }

  @Test
  void skalOpprettePdf() {

    // gitt
    var barn = Set.of(
        new PersonDto(Krypteringsverktøy.kryptere(PersonidentGeneratorKt.genererFødselsnummer(null, null)), "Sandstrand", "Sandstrand", LocalDate.now().minusMonths(126)),
        new PersonDto(Krypteringsverktøy.kryptere(PersonidentGeneratorKt.genererFødselsnummer(null, null)), "Verksted", "Verksted", LocalDate.now().minusMonths(159)));

    var hovedpart = new PersonDto(Krypteringsverktøy.kryptere(PersonidentGeneratorKt.genererFødselsnummer(null, null)), "Parkas", "Parkas", LocalDate.now().minusMonths(465));
    var motpart = new PersonDto(Krypteringsverktøy.kryptere(PersonidentGeneratorKt.genererFødselsnummer(null, null)), "Bonjour", "Bonjour", LocalDate.now().minusMonths(512));

    // hvis
    var pdfstrøm = PdfGenerator.genererePdf(barn, hovedpart, motpart, LocalDateTime.now());

    // så
    if (skriveUtPdf) {
      skriveUtPdfForInspeksjon(pdfstrøm);
    }

    assertAll(
        () -> assertThat(skriveUtPdf).isFalse(),
        () -> assertThat(pdfstrøm).isNotNull()
    );
  }

  private void skriveUtPdfForInspeksjon(byte[] pdfstroem) {
    try (final FileOutputStream filstroem = new FileOutputStream("forespørsel.pdf")) {
      filstroem.write(pdfstroem);
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

}
