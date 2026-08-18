package no.nav.bidrag.sjablon.yaml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import no.nav.bidrag.sjablon.validering.Validerbar;

@Data
public class SærbidragYaml implements Validerbar {
  @JsonProperty("Øvre grense for særbidrag")
  private TidslinjeYaml<BeløpYaml> øvreGrense;

  @Override
  public void valider() {
    // TODO
  }

  public static SærbidragYaml lastInn() {
    return SjablonerYaml.lastInn("sjabloner/særbidrag.yaml", SærbidragYaml.class);
  }
}
