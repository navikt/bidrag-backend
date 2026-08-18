package no.nav.bidrag.dokument.journalpost.model;

import static org.assertj.core.api.Assertions.assertThat;

import no.nav.bidrag.transport.dokument.AvvikType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Avvikstype")
class AvvikstypeTest {

  @ParameterizedTest
  @DisplayName("skal matche AvvikType fra bidrag-dokument-dto")
  @EnumSource(Avvikstype.class)
  void skalHaEnumSomAvvikType(Avvikstype avvikstype) {
    assertThat(AvvikType.valueOf(avvikstype.name())).isNotNull();
  }

  @Test
  @DisplayName("skal ha like mange enummer som AvvikType fra bidrag-dokument-dto")
  void skalHaLiktAntallEnummerSomAvvikType() {
    assertThat(Avvikstype.values().length).isEqualTo(AvvikType.values().length - 4);
  }
}
