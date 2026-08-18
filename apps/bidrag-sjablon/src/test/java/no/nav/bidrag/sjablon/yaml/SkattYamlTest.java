package no.nav.bidrag.sjablon.yaml;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class SkattYamlTest {

  @Test
  public void testLastInn() {
    SkattYaml skatt = SkattYaml.lastInn();
    assertNotNull(skatt);
    assertNotNull(skatt.getSkattAlminneligInntekt());
    assertNotNull(skatt.getFordelSkatteklasse2());
    assertNotNull(skatt.getMinstefradrag());
    assertNotNull(skatt.getPersonfradrag());
    skatt.valider();
  }
}
