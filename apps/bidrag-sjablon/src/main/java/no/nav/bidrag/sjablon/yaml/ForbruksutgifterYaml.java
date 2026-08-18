package no.nav.bidrag.sjablon.yaml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import no.nav.bidrag.sjablon.validering.Validerbar;

@Data
public class ForbruksutgifterYaml implements Validerbar {

  @JsonProperty("Forbruksutgifter")
  private TidslinjeYaml<AldersgrupperYaml<BeløpYaml>> forbruksutgifter;

  @Override
  public void valider() {
    // TODO Auto-generated method stub
  }

  public static ForbruksutgifterYaml lastInn() {
    return SjablonerYaml.lastInn("sjabloner/forbruksutgifter.yaml", ForbruksutgifterYaml.class);
  }
}
