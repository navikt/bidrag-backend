package no.nav.bidrag.sjablon.yaml;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class SærbidragYamlTest {

  @Test
  public void testLastInn() {
    SærbidragYaml særbidrag = SærbidragYaml.lastInn();
    assertNotNull(særbidrag.getØvreGrense());
    særbidrag.valider();
  }
}
