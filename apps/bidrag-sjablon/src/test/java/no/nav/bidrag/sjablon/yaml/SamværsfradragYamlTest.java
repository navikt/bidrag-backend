package no.nav.bidrag.sjablon.yaml;

import org.junit.jupiter.api.Test;

public class SamværsfradragYamlTest {
  @Test
  public void testLastInn() {
    SamværsfradragYaml særfradrag = SamværsfradragYaml.lastInn();
    særfradrag.valider();
  }
}
