package no.nav.bidrag.sjablon.yaml;

import org.junit.jupiter.api.Test;

public class ForbruksutgifterYamlTest {

  @Test
  public void testLastInn() {
    ForbruksutgifterYaml bidragsevne = ForbruksutgifterYaml.lastInn();
    bidragsevne.valider();
  }
}
