package no.nav.bidrag.sjablon.yaml;

import org.junit.jupiter.api.Test;

public class IndeksreguleringYamlTest {

  @Test
  public void testLastInn() {
    IndeksreguleringYaml indeksregulering = IndeksreguleringYaml.lastInn();
    indeksregulering.valider();
  }
}
