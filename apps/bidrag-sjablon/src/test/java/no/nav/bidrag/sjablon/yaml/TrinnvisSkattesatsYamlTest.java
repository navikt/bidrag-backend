package no.nav.bidrag.sjablon.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.LinkedHashMap;
import java.util.List;
import no.nav.bidrag.sjablon.yaml.TrinnvisSkattesatsYaml.Trinn;
import no.nav.bidrag.sjablon.yaml.TrinnvisSkattesatsYaml.Trinnskatt;
import org.junit.jupiter.api.Test;

public class TrinnvisSkattesatsYamlTest {
  @Test
  public void testLastInn() {

    TrinnvisSkattesatsYaml trinnskatt = TrinnvisSkattesatsYaml.lastInn();
    assertNotNull(trinnskatt);
    trinnskatt.valider();
  }

  @Test
  public void testTrinnskatt() {
    LinkedHashMap<BeløpYaml, ProsentYaml> map = new LinkedHashMap<>();

    map.put(new BeløpYaml("208.050,-"), new ProsentYaml("1,7%"));
    map.put(new BeløpYaml("292.850,-"), new ProsentYaml("4,0%"));
    map.put(new BeløpYaml("670.000,-"), new ProsentYaml("13,6%"));
    map.put(new BeløpYaml("937.900,-"), new ProsentYaml("16,6%"));
    map.put(new BeløpYaml("1.350.000,-"), new ProsentYaml("17,6%"));

    List<Trinn> trinn = new Trinnskatt(map).getTrinn();
    assertEquals(5, trinn.size());
    assertEquals(trinn("208.050,-", "292.850,-", "1,7%"), trinn.get(0));
    assertEquals(trinn("292.850,-", "670.000,-", "4,0%"), trinn.get(1));
    assertEquals(trinn("670.000,-", "937.900,-", "13,6%"), trinn.get(2));
    assertEquals(trinn("937.900,-", "1.350.000,-", "16,6%"), trinn.get(3));
    assertEquals(trinn("1.350.000,-", null, "17,6%"), trinn.get(4));
  }

  private static Trinn trinn(String beløpFra, String beløpTil, String prosent) {
    return new Trinn(
        new BeløpYaml(beløpFra).getBeløp(),
        beløpTil != null ? new BeløpYaml(beløpTil).getBeløp() : null,
        new ProsentYaml(prosent).getFaktor());
  }
}
