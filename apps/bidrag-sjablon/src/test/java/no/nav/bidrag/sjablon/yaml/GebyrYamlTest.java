package no.nav.bidrag.sjablon.yaml;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class GebyrYamlTest {

  @Test
  public void testLastInn() {
    GebyrYaml gebyr = GebyrYaml.lastInn();
    assertNotNull(gebyr.getFastsettelsesgebyr());
    gebyr.valider();
  }
}
