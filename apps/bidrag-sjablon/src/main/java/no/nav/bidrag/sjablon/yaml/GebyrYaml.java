package no.nav.bidrag.sjablon.yaml;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import no.nav.bidrag.sjablon.validering.Validerbar;

@Data
public class GebyrYaml implements Validerbar {
  @JsonProperty("Fastsettelsesgebyr")
  private TidslinjeYaml<BeløpYaml> fastsettelsesgebyr;

  @JsonProperty("Nedre inntektsgrense")
  private TidslinjeYaml<BeløpYaml> nedreInntektsgrense;

  @Override
  public void valider() {
    checkNotNull(fastsettelsesgebyr, "Fastsettelsesgebyr må angis");
    checkNotNull(nedreInntektsgrense, "Nedre inntektsgrense må angis");
  }

  public static GebyrYaml lastInn() {
    return SjablonerYaml.lastInn("sjabloner/gebyr.yaml", GebyrYaml.class);
  }
}
