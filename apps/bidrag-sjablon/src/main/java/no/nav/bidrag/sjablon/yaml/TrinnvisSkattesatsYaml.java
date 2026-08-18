package no.nav.bidrag.sjablon.yaml;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.Data;
import no.nav.bidrag.sjablon.validering.Validerbar;

@Data
public class TrinnvisSkattesatsYaml implements Validerbar {

  @JsonProperty("Trinnvis skattesats")
  private TidslinjeYaml<Trinnskatt> trinnvisSkattesats;

  @Data
  public static class Trinnskatt {
    private final List<Trinn> trinn;

    @JsonCreator
    public Trinnskatt(LinkedHashMap<BeløpYaml, ProsentYaml> values) {
      trinn = new ArrayList<>(values.size());
      Iterator<Entry<BeløpYaml, ProsentYaml>> iterator = values.entrySet().iterator();
      if (iterator.hasNext()) {
        Map.Entry<BeløpYaml, ProsentYaml> entry = iterator.next();
        do {
          Map.Entry<BeløpYaml, ProsentYaml> nextEntry = iterator.hasNext() ? iterator.next() : null;

          BigDecimal nesteBeløp = nextEntry != null ? nextEntry.getKey().getBeløp() : null;

          trinn.add(new Trinn(entry.getKey().getBeløp(), nesteBeløp, entry.getValue().getFaktor()));

          entry = nextEntry;
        } while (entry != null);
      }
    }
  }

  @Data
  public static class Trinn {
    private final BigDecimal beløpFra;
    private final BigDecimal beløpTil;
    private final BigDecimal faktor;
  }

  @Override
  public void valider() {
    // TODO
  }

  public static TrinnvisSkattesatsYaml lastInn() {
    return SjablonerYaml.lastInn("sjabloner/trinnskatt.yaml", TrinnvisSkattesatsYaml.class);
  }
}
