package no.nav.bidrag.sjablon.yaml;

import org.junit.jupiter.api.Test;

public class ForskuddYamlTest {
  @Test
  public void testLastInn() {
    ForskuddYaml forskudd = ForskuddYaml.lastInn();
    forskudd.valider();
  }
}
