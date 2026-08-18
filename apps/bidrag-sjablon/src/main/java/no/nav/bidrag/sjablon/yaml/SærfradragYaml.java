package no.nav.bidrag.sjablon.yaml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import no.nav.bidrag.sjablon.validering.Validerbar;

@Data
public class SærfradragYaml implements Validerbar {
  @JsonProperty("Fordel av særfradrag")
  private TidslinjeYaml<FordelSærfradrag> fordelSærfradrag;

  @Data
  public static class FordelSærfradrag {
    @JsonProperty("Inntektsgrenser")
    private Inntektsgrenser inntektsgrenser;

    @JsonProperty("Fordelsbeløp")
    private BeløpYaml fordelsbeløp;
  }

  @Data
  public static class Inntektsgrenser {
    @JsonProperty("Full fordel fra")
    private BeløpYaml fullFordelFra;

    @JsonProperty("Halv fordel fra")
    private BeløpYaml halvFordelFra;
  }

  @Override
  public void valider() {
    // TODO
  }

  public static SærfradragYaml lastInn() {
    return SjablonerYaml.lastInn("sjabloner/særfradrag.yaml", SærfradragYaml.class);
  }
}
