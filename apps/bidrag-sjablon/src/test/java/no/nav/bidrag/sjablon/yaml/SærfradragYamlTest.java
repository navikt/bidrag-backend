package no.nav.bidrag.sjablon.yaml;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class SærfradragYamlTest {

  @Test
  public void testLastInn() {
    SærfradragYaml særfradrag = SærfradragYaml.lastInn();
    assertNotNull(særfradrag.getFordelSærfradrag());
    særfradrag.valider();
  }
}
