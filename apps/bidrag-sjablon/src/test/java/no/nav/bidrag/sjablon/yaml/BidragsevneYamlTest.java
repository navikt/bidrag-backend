package no.nav.bidrag.sjablon.yaml;

import org.junit.jupiter.api.Test;

public class BidragsevneYamlTest {

  @Test
  public void testLastInn() {
    BidragsevneYaml bidragsevne = BidragsevneYaml.lastInn();
    bidragsevne.valider();
  }
}
