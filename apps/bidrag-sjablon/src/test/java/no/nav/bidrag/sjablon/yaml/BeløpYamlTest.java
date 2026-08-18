package no.nav.bidrag.sjablon.yaml;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

public class BeløpYamlTest {

  @Test
  public void testDeserialize() {
    assertDeserialize("0,-", 0);
    assertDeserialize("999,-", 999);
    assertDeserialize("1.000,-", 1000);
    assertDeserialize("999.999,-", 999999);
    assertDeserialize("1.000.000,-", 1000000);
    assertDeserialize("999.999.999,-", 999999999);
    assertDeserialize("1.000.000.000,-", 1000000000);
  }

  private void assertDeserialize(String beløpString, double value) {
    assertTrue(new BeløpYaml(beløpString).getBeløp().compareTo(new BigDecimal(value)) == 0);
  }
}
