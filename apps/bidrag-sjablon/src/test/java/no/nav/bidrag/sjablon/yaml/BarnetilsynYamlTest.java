package no.nav.bidrag.sjablon.yaml;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class BarnetilsynYamlTest {
  @Test
  public void testLastInn() {

    BarnetilsynYaml barnetilsyn = BarnetilsynYaml.lastInn();
    assertNotNull(barnetilsyn);
    barnetilsyn.valider();
  }
}
