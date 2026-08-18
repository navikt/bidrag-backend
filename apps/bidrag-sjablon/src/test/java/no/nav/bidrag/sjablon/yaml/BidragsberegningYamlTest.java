package no.nav.bidrag.sjablon.yaml;

import org.junit.jupiter.api.Test;

public class BidragsberegningYamlTest {

  @Test
  public void testLastInn() {
    BidragsberegningYaml bidragsberegning = BidragsberegningYaml.lastInn();
    bidragsberegning.valider();
  }
}
