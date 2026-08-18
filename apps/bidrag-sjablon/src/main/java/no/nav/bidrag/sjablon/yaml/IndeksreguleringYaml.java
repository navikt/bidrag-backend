package no.nav.bidrag.sjablon.yaml;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import no.nav.bidrag.sjablon.validering.Validerbar;

@Data
public class IndeksreguleringYaml implements Validerbar {
  @JsonProperty("Land")
  private final Map<String, Land> land = new HashMap<>();

  @Data
  public static class Land implements Validerbar {

    @JsonProperty("Indeksprosent")
    private TidslinjeYaml<ProsentYaml> indeksprosent;

    @Override
    public void valider() {
      checkNotNull(indeksprosent, "Tidslinje for indeksprosent må være satt");
    }
  }

  @Override
  public void valider() {
    checkNotNull(land.get("NOR"), "Landet NOR må være angitt");
  }

  public static IndeksreguleringYaml lastInn() {
    return SjablonerYaml.lastInn("sjabloner/indeksregulering.yaml", IndeksreguleringYaml.class);
  }
}
