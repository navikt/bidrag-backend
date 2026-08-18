package no.nav.bidrag.sjablon.yaml;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class StønaderYamlTest {

  @Test
  public void testLastInn() {
    StønaderYaml stønader = StønaderYaml.lastInn();
    assertNotNull(stønader);
    stønader.valider();
  }
}
